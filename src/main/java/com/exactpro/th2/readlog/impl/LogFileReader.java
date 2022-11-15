/*******************************************************************************
 * Copyright 2022 Exactpro (Exactpro Systems Limited)
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
 ******************************************************************************/

package com.exactpro.th2.readlog.impl;

import com.exactpro.th2.common.grpc.RawMessage;
import com.exactpro.th2.read.file.common.*;
import com.exactpro.th2.read.file.common.impl.DefaultFileReader.Builder;
import com.exactpro.th2.read.file.common.impl.RecoverableBufferedReaderWrapper;
import com.exactpro.th2.read.file.common.state.ReaderState;
import com.exactpro.th2.readlog.RegexLogParser;
import com.exactpro.th2.readlog.cfg.LogReaderConfiguration;
import kotlin.Unit;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

interface ForOnStreamData {
    Unit action (StreamId id, List<RawMessage.Builder> builder);
}

interface ForOnError {
    Unit action (StreamId id, String message, Exception ex);
}

interface ForOnSourceCorrupted {
    Unit action (StreamId id, Path path, Exception ex);
}

public class LogFileReader {

    public static AbstractFileReader<LineNumberReader> getLogFileReader(
            LogReaderConfiguration configuration,
            ReaderState readerState,
            ForOnStreamData forStream,
            ForOnError forError,
            ForOnSourceCorrupted forCorrupted
    ){
            return new Builder<>(
                    configuration.getCommon(),
                    getDirectoryChecker(configuration),
                    new RegexpContentParser(new RegexLogParser(configuration.getAliases())),
                    new MovedFileTracker(configuration.getLogDirectory()),
                    readerState,
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
                        .flatMap(entry -> entry.getValue().getDirectionToPattern()
                                .keySet().stream()
                                .map(direction -> new StreamId(entry.getKey(), direction))
                        ).collect(Collectors.toSet()),
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
