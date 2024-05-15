/*
 * Copyright 2020-2023 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.th2.common.event.Event;
import com.exactpro.th2.common.event.Event.Status;
import com.exactpro.th2.common.event.EventUtils;
import com.exactpro.th2.common.grpc.EventBatch;
import com.exactpro.th2.common.grpc.EventID;
import com.exactpro.th2.common.metrics.CommonMetrics;
import com.exactpro.th2.common.schema.factory.CommonFactory;
import com.exactpro.th2.common.schema.message.MessageRouter;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.GroupBatch;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.MessageGroup;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.MessageId;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.RawMessage;
import com.exactpro.th2.read.file.common.AbstractFileReader;
import com.exactpro.th2.read.file.common.StreamId;
import com.exactpro.th2.read.file.common.state.impl.InMemoryReaderState;
import com.exactpro.th2.readlog.cfg.LogReaderConfiguration;
import com.exactpro.th2.readlog.impl.CradleReaderState;
import com.exactpro.th2.readlog.impl.LogFileReader;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Deque<AutoCloseable> toDispose = new ArrayDeque<>();
        var lock = new ReentrantLock();
        var condition = lock.newCondition();
        configureShutdownHook(toDispose, lock, condition);

        CommonMetrics.LIVENESS_MONITOR.enable();
        CommonFactory commonFactory = CommonFactory.createFromArguments(args);
        var boxBookName = commonFactory.getBoxConfiguration().getBookName();
        toDispose.add(commonFactory);

        MessageRouter<EventBatch> eventBatchRouter = commonFactory.getEventBatchRouter();

        LogReaderConfiguration configuration = commonFactory.getCustomConfiguration(LogReaderConfiguration.class, LogReaderConfiguration.MAPPER);
        configuration.getAliases().forEach((alias, cfg) -> {
            if (cfg.isJoinGroups() && cfg.getHeadersFormat().isEmpty()) {
                throw new IllegalArgumentException("Alias " + alias + " has parameter joinGroups = true but does not have any headers defined");
            }
        });

        if (configuration.getPullingInterval().isNegative()) {
            throw new IllegalArgumentException("Pulling interval " + configuration.getPullingInterval() + " must not be negative");
        }

        try {
            Event rootEvent = Event.start().endTimestamp()
                    .name("Log reader for " + String.join(",", configuration.getAliases().keySet()))
                    .type("ReadLog")
                    .status(Status.PASSED);
            EventID componentRootEvent = commonFactory.getRootEventId();
            var protoEvent = rootEvent.toProto(componentRootEvent);
            eventBatchRouter.sendAll(EventBatch.newBuilder().addEvents(protoEvent).build());
            EventID rootId = protoEvent.getId();

            CommonMetrics.READINESS_MONITOR.enable();

            final Runnable processUpdates;

            if (configuration.isUseTransport()) {
                AbstractFileReader<LineNumberReader, RawMessage.Builder, MessageId.Builder> reader
                        = LogFileReader.getTransportLogFileReader(
                        configuration,
                        configuration.isSyncWithCradle()
                                ? new CradleReaderState(commonFactory.getCradleManager().getStorage(),
                                streamId -> commonFactory.newMessageIDBuilder().getBookName(),
                                CradleReaderState.WRAP_TRANSPORT)
                                : new InMemoryReaderState(),
                        streamId -> MessageId.builder(),
                        (streamId, builders) -> publishTransportMessages(commonFactory.getTransportGroupBatchRouter(), streamId, builders, boxBookName),
                        (streamId, message, ex) -> publishErrorEvent(eventBatchRouter, streamId, message, ex, rootId),
                        (streamId, path, e) -> publishSourceCorruptedEvent(eventBatchRouter, path, streamId, e, rootId)
                );

                processUpdates = reader::processUpdates;
                toDispose.add(reader);
            } else {
                AbstractFileReader<LineNumberReader,com.exactpro.th2.common.grpc.RawMessage.Builder, com.exactpro.th2.common.grpc.MessageID> reader
                        = LogFileReader.getProtoLogFileReader(
                        configuration,
                        configuration.isSyncWithCradle()
                                ? new CradleReaderState(commonFactory.getCradleManager().getStorage(), streamId -> boxBookName, CradleReaderState.WRAP_PROTO)
                                : new InMemoryReaderState(),
                        streamId -> commonFactory.newMessageIDBuilder().build(),
                        (streamId, builders) -> publishProtoMessages(commonFactory.getMessageRouterRawBatch(), streamId, builders),
                        (streamId, message, ex) -> publishErrorEvent(eventBatchRouter, streamId, message, ex, rootId),
                        (streamId, path, e) -> publishSourceCorruptedEvent(eventBatchRouter, path, streamId, e, rootId)
                );

                processUpdates = reader::processUpdates;
                toDispose.add(reader);
            }

            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            toDispose.add(() -> {
                executorService.shutdown();
                if (executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.warn("Cannot shutdown executor for 5 seconds");
                    executorService.shutdownNow();
                }
            });

            ScheduledFuture<?> future = executorService.scheduleWithFixedDelay(processUpdates, 0, configuration.getPullingInterval().toMillis(), TimeUnit.MILLISECONDS);
            awaitShutdown(lock, condition);
            future.cancel(true);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Cannot read files from: {}", configuration.getLogDirectory(), e);
        }
    }

    @NotNull
    private static Unit publishSourceCorruptedEvent(MessageRouter<EventBatch> eventBatchRouter, Path path, StreamId streamId, Exception e, EventID rootEventId) {
        Event error = Event.start()
                .name("Corrupted source " + path + " for " + streamId.getSessionAlias())
                .type("CorruptedSource");
        return publishError(eventBatchRouter, streamId, e, error, rootEventId);
    }

    @NotNull
    private static Unit publishErrorEvent(MessageRouter<EventBatch> eventBatchRouter, StreamId streamId, String message, Exception ex, EventID rootEventId) {
        Event error = Event.start().endTimestamp()
                .name(streamId == null ? "General error" : "Error for session alias " + streamId.getSessionAlias())
                .type("Error")
                .bodyData(EventUtils.createMessageBean(message));
        return publishError(eventBatchRouter, streamId, ex, error, rootEventId);
    }

    @NotNull
    private static Unit publishError(MessageRouter<EventBatch> eventBatchRouter, StreamId streamId, Exception ex, Event error, EventID rootEventId) {
        Throwable tmp = ex;
        while (tmp != null) {
            error.bodyData(EventUtils.createMessageBean(tmp.getMessage()));
            tmp = tmp.getCause();
        }
        try {
            eventBatchRouter.sendAll(EventBatch.newBuilder().addEvents(error.toProto(rootEventId)).build());
        } catch (Exception e) {
            LOGGER.error("Cannot send event for stream {}", streamId, e);
        }
        return Unit.INSTANCE;
    }

    @NotNull
    private static Unit publishProtoMessages(MessageRouter<com.exactpro.th2.common.grpc.RawMessageBatch> rawMessageBatchRouter, StreamId streamId, List<? extends com.exactpro.th2.common.grpc.RawMessage.Builder> builders) {
        try {
            com.exactpro.th2.common.grpc.RawMessageBatch.Builder builder = com.exactpro.th2.common.grpc.RawMessageBatch.newBuilder();
            for (com.exactpro.th2.common.grpc.RawMessage.Builder msg : builders) {
                builder.addMessages(msg);
            }
            rawMessageBatchRouter.sendAll(builder.build(), "raw");
        } catch (Exception e) {
            LOGGER.error("Cannot publish batch for {}", streamId, e);
        }
        return Unit.INSTANCE;
    }

    @NotNull
    private static Unit publishTransportMessages(MessageRouter<GroupBatch> rawMessageBatchRouter, StreamId streamId, List<? extends RawMessage.Builder> builders, String bookName) {
        try {
            // messages are grouped by session aliases
            String sessionGroup = builders.get(0).idBuilder().getSessionAlias();

            List<MessageGroup> groups = new ArrayList<>(builders.size());
            for (RawMessage.Builder msgBuilder : builders) {
                groups.add(new MessageGroup(List.of(msgBuilder.build())));
            }

            var batch = new GroupBatch(bookName, sessionGroup, groups);
            rawMessageBatchRouter.sendAll(batch, "transport-group");
        } catch (Exception e) {
            LOGGER.error("Cannot publish batch for {}", streamId, e);
        }

        return Unit.INSTANCE;
    }

    private static void configureShutdownHook(Deque<AutoCloseable> resources, ReentrantLock lock, Condition condition) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown start");
            CommonMetrics.READINESS_MONITOR.disable();
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

            CommonMetrics.LIVENESS_MONITOR.disable();
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