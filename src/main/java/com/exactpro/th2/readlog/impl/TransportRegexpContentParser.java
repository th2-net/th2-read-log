/*
 * Copyright 2023 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.RawMessage;
import com.exactpro.th2.read.file.common.StreamId;
import com.exactpro.th2.read.file.common.impl.LineParser;
import com.exactpro.th2.readlog.LogData;
import com.exactpro.th2.readlog.RegexLogParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class TransportRegexpContentParser extends LineParser<RawMessage.Builder> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransportRegexpContentParser.class);
    private final RegexLogParser parser;

    public TransportRegexpContentParser(RegexLogParser parser) {
        super(LineParser.TRANSPORT);
        this.parser = requireNonNull(parser, "'Parser' parameter");
    }

    @Nonnull
    @Override
    protected List<RawMessage.Builder> lineToMessages(@Nonnull StreamId streamId, @Nonnull String readLine) {
        LogData logData = parser.parse(streamId, readLine);
        LOGGER.trace("{} line(s) extracted from {}: {}", logData.getBody().size(), readLine, logData.getBody());
        return logData.getBody().stream().map(it -> {
            RawMessage.Builder builder = RawMessage.builder();
            setupMetadata(builder, logData);
            builder.setBody(it.getBytes(StandardCharsets.UTF_8));
            return builder;
        }).collect(Collectors.toList());
    }

    private void setupMetadata(RawMessage.Builder builder, LogData logData) {
        builder.idBuilder().setDirection(requireNonNull(logData.getDirection(), "direction is not set"));

        if (logData.getParsedTimestamp() != null) {
            builder.idBuilder().setTimestamp(logData.getParsedTimestamp());
        }

        if (logData.getRawTimestamp() != null) {
            builder.addMetadataProperty("logTimestamp", logData.getRawTimestamp());
        }
    }
}