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

package com.exactpro.th2.readlog.cfg;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.exactpro.th2.common.grpc.Direction;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestLogReaderConfiguration {
    @Test
    void deserializes() throws IOException {
        try (var input = getResourceAsStream("test_cfg.json")) {
            LogReaderConfiguration cfg = LogReaderConfiguration.MAPPER.readValue(input, LogReaderConfiguration.class);
            assertEquals(Duration.ofSeconds(5), cfg.getPullingInterval());
            assertEquals(Set.of("A", "B"), cfg.getAliases().keySet());
            AliasConfiguration aliasA = cfg.getAliases().get("A");
            AliasConfiguration aliasB = cfg.getAliases().get("B");
            assertEquals(Set.of(Direction.FIRST, Direction.SECOND), aliasA.getDirectionToPattern().keySet());
            assertEquals(Set.of(Direction.FIRST), aliasB.getDirectionToPattern().keySet());
            assertEquals(".*", Objects.requireNonNull(aliasA.getRegexp()).pattern());
            assertEquals("202.*$", Objects.requireNonNull(aliasB.getTimestampRegexp()).pattern());
            assertEquals(DateTimeFormatter.ofPattern("yyyy.MM.dd").getResolverFields(), Objects.requireNonNull(aliasB.getTimestampFormat()).getResolverFields());
            assertEquals(
                    Instant.parse("2022-10-31T10:35:00Z"),
                    aliasB.getSkipBefore(),
                    "unexpected 'skipBefore' value"
            );
        }
    }

    @Test
    void deserializeWithJoinParams() throws IOException {
        try (var input = getResourceAsStream("test_cfg_csv_join.json")) {
            LogReaderConfiguration cfg = LogReaderConfiguration.MAPPER.readValue(input, LogReaderConfiguration.class);
            assertEquals(Duration.ofSeconds(5), cfg.getPullingInterval());
            assertEquals(Set.of("A", "B"), cfg.getAliases().keySet());
            AliasConfiguration aliasA = cfg.getAliases().get("A");
            assertEquals(Set.of(Direction.FIRST, Direction.SECOND), aliasA.getDirectionToPattern().keySet());
            AliasConfiguration aliasB = cfg.getAliases().get("B");
            assertEquals(Set.of(Direction.FIRST), aliasB.getDirectionToPattern().keySet());
            assertEquals(".*", Objects.requireNonNull(aliasA.getRegexp()).pattern());
            assertEquals("202.*$", Objects.requireNonNull(aliasB.getTimestampRegexp()).pattern());
            assertEquals(DateTimeFormatter.ofPattern("yyyy.MM.dd").getResolverFields(), Objects.requireNonNull(aliasB.getTimestampFormat()).getResolverFields());

            assertEquals(",", aliasA.getGroupsJoinDelimiter());
            assertEquals("\t", aliasB.getGroupsJoinDelimiter());

            assertTrue(aliasA.isJoinGroups());
            assertTrue(aliasB.isJoinGroups());

            assertEquals(Map.of(
                    "A", "foo ${0}",
                    "B", "bar ${1}"
            ), aliasA.getHeadersFormat());

            assertEquals(Map.of(
                    "A", "foo ${0}",
                    "B", "bar ${1}",
                    "C", "const"
            ), aliasB.getHeadersFormat());
        }
    }

    @Nullable
    private InputStream getResourceAsStream(String name) {
        return TestLogReaderConfiguration.class.getClassLoader().getResourceAsStream(name);
    }
}