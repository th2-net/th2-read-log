/*
 * Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.th2.readlog;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.exactpro.th2.common.event.Event;
import com.exactpro.th2.common.event.EventUtils;
import com.exactpro.th2.common.grpc.Direction;
import com.exactpro.th2.common.grpc.EventBatch;
import com.exactpro.th2.common.grpc.RawMessage;
import com.exactpro.th2.common.grpc.RawMessageBatch;
import com.exactpro.th2.common.metrics.CommonMetrics;
import com.exactpro.th2.common.schema.factory.CommonFactory;
import com.exactpro.th2.common.schema.message.MessageRouter;
import com.exactpro.th2.read.file.common.AbstractFileReader;
import com.exactpro.th2.read.file.common.DirectoryChecker;
import com.exactpro.th2.read.file.common.FileSourceWrapper;
import com.exactpro.th2.read.file.common.MovedFileTracker;
import com.exactpro.th2.read.file.common.StreamId;
import com.exactpro.th2.read.file.common.impl.BufferedReaderSourceWrapper;
import com.exactpro.th2.read.file.common.impl.DefaultFileReader;
import com.exactpro.th2.read.file.common.state.impl.InMemoryReaderState;
import com.exactpro.th2.readlog.cfg.LogReaderConfiguration;
import com.exactpro.th2.readlog.impl.RegexpContentParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import kotlin.Unit;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Comparator.comparing;

public class Main extends Object {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new KotlinModule())
            .registerModule(new JavaTimeModule());

    public static void main(String[] args) {
        Deque<AutoCloseable> toDispose = new ArrayDeque<>();
        var lock = new ReentrantLock();
        var condition = lock.newCondition();
        configureShutdownHook(toDispose, lock, condition);

        CommonMetrics.setLiveness(true);
        CommonFactory commonFactory = CommonFactory.createFromArguments(args);
        toDispose.add(commonFactory);

        MessageRouter<RawMessageBatch> rawMessageBatchRouter = commonFactory.getMessageRouterRawBatch();
        MessageRouter<EventBatch> eventBatchRouter = commonFactory.getEventBatchRouter();

        LogReaderConfiguration configuration = commonFactory.getCustomConfiguration(LogReaderConfiguration.class, MAPPER);
        Comparator<Path> pathComparator = comparing(it -> it.getFileName().toString(), String.CASE_INSENSITIVE_ORDER);
        var directoryChecker = new DirectoryChecker(
                configuration.getLogDirectory(),
                (Path path) -> configuration.getAliases().entrySet().stream()
                        .filter(entry -> entry.getValue().getPathFilter().matcher(path.getFileName().toString()).matches())
                        .map(it -> new StreamId(it.getKey(), Direction.FIRST))
                        .collect(Collectors.toSet()),
                files -> files.sort(pathComparator),
                path -> true
        );
        RegexLogParser logParser = new RegexLogParser(configuration.getAliases());

        if (configuration.getPullingInterval().isNegative()) {
            throw new IllegalArgumentException("Pulling interval " + configuration.getPullingInterval() + " must not be negative");
        }

        try {
            Event rootEvent = Event.start().endTimestamp()
                    .name("Log reader for " + String.join(",", configuration.getAliases().keySet()))
                    .type("Microservice");
            eventBatchRouter.sendAll(EventBatch.newBuilder().addEvents(rootEvent.toProtoEvent(null)).build());

            CommonMetrics.setReadiness(true);
            AbstractFileReader<BufferedReader> reader = new DefaultFileReader.Builder<>(
                    configuration.getCommon(),
                    directoryChecker,
                    new RegexpContentParser(logParser),
                    new MovedFileTracker(configuration.getLogDirectory(), null),
                    new InMemoryReaderState(),
                    Main::createSource
            )
                    .readFileImmediately()
                    .acceptNewerFiles()
                    .onStreamData((streamId, builders) -> publishMessages(rawMessageBatchRouter, streamId, builders))
                    .onError((streamId, message, ex) -> publishErrorEvent(eventBatchRouter, streamId, message, ex, rootEvent.getId()))
                    .onSourceCorrupted((streamId, path, e) -> publishSourceCorruptedEvent(eventBatchRouter, path, streamId, e, rootEvent.getId()))
                    .build();

            toDispose.add(reader);

            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            toDispose.add(() -> {
                executorService.shutdown();
                if (executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.warn("Cannot shutdown executor for 5 seconds");
                    executorService.shutdownNow();
                }
            });


            ScheduledFuture<?> future = executorService.scheduleWithFixedDelay(reader::processUpdates, 0, configuration.getPullingInterval().toMillis(), TimeUnit.MILLISECONDS);
            awaitShutdown(lock, condition);
            future.cancel(true);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Cannot read files from: {}", configuration.getLogDirectory(), e);
        }
    }

    @NotNull
    private static Unit publishSourceCorruptedEvent(MessageRouter<EventBatch> eventBatchRouter, Path path, StreamId streamId, Exception e, String rootEventId) {
        Event error = Event.start()
                .name("Corrupted source " + path + " for " + streamId.getSessionAlias())
                .type("CorruptedSource");
        return publishError(eventBatchRouter, streamId, e, error, rootEventId);
    }

    @NotNull
    private static Unit publishErrorEvent(MessageRouter<EventBatch> eventBatchRouter, StreamId streamId, String message, Exception ex, String rootEventId) {
        Event error = Event.start().endTimestamp()
                .name(streamId == null ? "General error" : "Error for session alias " + streamId.getSessionAlias())
                .type("Error")
                .bodyData(EventUtils.createMessageBean(message));
        return publishError(eventBatchRouter, streamId, ex, error, rootEventId);
    }

    @NotNull
    private static Unit publishError(MessageRouter<EventBatch> eventBatchRouter, StreamId streamId, Exception ex, Event error, String rootEventId) {
        Throwable tmp = ex;
        while (tmp != null) {
            error.bodyData(EventUtils.createMessageBean(tmp.getMessage()));
            tmp = tmp.getCause();
        }
        try {
            eventBatchRouter.sendAll(EventBatch.newBuilder().addEvents(error.toProtoEvent(rootEventId)).build());
        } catch (Exception e) {
            LOGGER.error("Cannot send event for stream {}", streamId, e);
        }
        return Unit.INSTANCE;
    }

    @NotNull
    private static Unit publishMessages(MessageRouter<RawMessageBatch> rawMessageBatchRouter, StreamId streamId, List<RawMessage.Builder> builders) {
        try {
            RawMessageBatch.Builder builder = RawMessageBatch.newBuilder();
            for (RawMessage.Builder msg : builders) {
                builder.addMessages(msg);
            }
            rawMessageBatchRouter.sendAll(builder.build());
        } catch (Exception e) {
            LOGGER.error("Cannot publish batch for {}", streamId, e);
        }
        return Unit.INSTANCE;
    }

    private static FileSourceWrapper<BufferedReader> createSource(StreamId streamId, Path path) {
        try {
            return new BufferedReaderSourceWrapper<>(Files.newBufferedReader(path));
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    private static void configureShutdownHook(Deque<AutoCloseable> resources, ReentrantLock lock, Condition condition) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown start");
            CommonMetrics.setReadiness(false);
            try {
                lock.lock();
                condition.signalAll();
            } finally {
                lock.unlock();
            }
            resources.descendingIterator().forEachRemaining(resource -> {
                try {
                    resource.close();
                } catch (Exception e) {
                    LOGGER.error("Cannot close resource {}", resource.getClass(), e);
                }
            });

            CommonMetrics.setLiveness(false);
            LOGGER.info("Shutdown end");
        }, "Shutdown hook"));
    }

    private static void awaitShutdown(ReentrantLock lock, Condition condition) throws InterruptedException {
        try {
            lock.lock();
            LOGGER.info("Wait shutdown");
            condition.await();
            LOGGER.info("App shutdown");
        } finally {
            lock.unlock();
        }
    }

}
