/*
 * Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.exactpro.th2.common.grpc.RawMessage;
import com.exactpro.th2.read.file.common.StreamId;
import com.exactpro.th2.read.file.common.impl.LineParser;
import com.exactpro.th2.readlog.RegexLogParser;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegexpContentParser extends LineParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegexpContentParser.class);
    private final RegexLogParser parser;

    public RegexpContentParser(RegexLogParser parser) {
        this.parser = Objects.requireNonNull(parser, "'Parser' parameter");
    }

    @Nonnull
    @Override
    protected List<RawMessage.Builder> lineToMessages(@Nonnull StreamId streamId, @Nonnull String readLine) {
        List<String> toPublish = parser.parse(streamId.getSessionAlias(), readLine);
        LOGGER.trace("{} line(s) extracted from {}: {}", toPublish.size(), readLine, toPublish);
        return toPublish.stream()
                .map(it -> RawMessage.newBuilder().setBody(ByteString.copyFrom(it.getBytes(StandardCharsets.UTF_8))))
                .collect(Collectors.toList());
    }
}
