/*
 * Copyright 2022-2023 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.readlog.impl;

import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.MessageId;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.RawMessage;
import com.exactpro.th2.read.file.common.AbstractFileReader;
import com.exactpro.th2.read.file.common.DirectoryChecker;
import com.exactpro.th2.read.file.common.FileSourceWrapper;
import com.exactpro.th2.read.file.common.MovedFileTracker;
import com.exactpro.th2.read.file.common.StreamId;
import com.exactpro.th2.read.file.common.impl.ProtoDefaultFileReader;
import com.exactpro.th2.read.file.common.impl.RecoverableBufferedReaderWrapper;
import com.exactpro.th2.read.file.common.impl.TransportDefaultFileReader;
import com.exactpro.th2.read.file.common.state.ReaderState;
import com.exactpro.th2.readlog.RegexLogParser;
import com.exactpro.th2.readlog.cfg.LogReaderConfiguration;
import com.exactpro.th2.readlog.impl.lambdas.ForOnError;
import com.exactpro.th2.readlog.impl.lambdas.ForOnSourceCorrupted;
import com.exactpro.th2.readlog.impl.lambdas.ProtoForOnStreamData;
import com.exactpro.th2.readlog.impl.lambdas.TransportForOnStreamData;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

public class LogFileReader {

    public static AbstractFileReader<LineNumberReader, com.exactpro.th2.common.grpc.RawMessage.Builder, com.exactpro.th2.common.grpc.MessageID> getProtoLogFileReader(
            LogReaderConfiguration configuration,
            ReaderState readerState,
            Function<StreamId, com.exactpro.th2.common.grpc.MessageID> initialMessageId,
            ProtoForOnStreamData forStream,
            ForOnError forError,
            ForOnSourceCorrupted forCorrupted
    ){
            return new ProtoDefaultFileReader.Builder<>(
                    configuration.getCommon(),
                    getDirectoryChecker(configuration),
                    new ProtoRegexpContentParser(new RegexLogParser(configuration.getAliases())),
                    new MovedFileTracker(configuration.getLogDirectory()),
                    readerState,
                    initialMessageId::apply,
                    LogFileReader::createSource
            )
                    .readFileImmediately()
                    .acceptNewerFiles()
                    .onStreamData(forStream::action)
                    .onError(forError::action)
                    .onSourceCorrupted(forCorrupted::action)
                    .build();
    }

    public static AbstractFileReader<LineNumberReader, RawMessage.Builder, MessageId.Builder> getTransportLogFileReader(
            LogReaderConfiguration configuration,
            ReaderState readerState,
            Function<StreamId, MessageId.Builder> initialMessageId,
            TransportForOnStreamData forStream,
            ForOnError forError,
            ForOnSourceCorrupted forCorrupted
    ){
        return new TransportDefaultFileReader.Builder<>(
                configuration.getCommon(),
                getDirectoryChecker(configuration),
                new TransportRegexpContentParser(new RegexLogParser(configuration.getAliases())),
                new MovedFileTracker(configuration.getLogDirectory()),
                readerState,
                initialMessageId::apply,
                LogFileReader::createSource
        )
                .readFileImmediately()
                .acceptNewerFiles()
                .onStreamData(forStream::action)
                .onError(forError::action)
                .onSourceCorrupted(forCorrupted::action)
                .build();
    }

    private static DirectoryChecker getDirectoryChecker(LogReaderConfiguration configuration) {
        Comparator<Path> pathComparator = comparing(it -> it.getFileName().toString(), String.CASE_INSENSITIVE_ORDER);
        return new DirectoryChecker(
                configuration.getLogDirectory(),
                (Path path) -> configuration.getAliases().entrySet().stream()
                        .filter(entry -> entry.getValue().getPathFilter().matcher(path.getFileName().toString()).matches())
                        .map(entry -> new StreamId(entry.getKey()))
                        .collect(Collectors.toSet()),
                files -> files.sort(pathComparator),
                path -> true
        );
    }

    private static FileSourceWrapper<LineNumberReader> createSource(StreamId streamId, Path path) {
        try {
            return new RecoverableBufferedReaderWrapper(new LineNumberReader(Files.newBufferedReader(path)));
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }
}