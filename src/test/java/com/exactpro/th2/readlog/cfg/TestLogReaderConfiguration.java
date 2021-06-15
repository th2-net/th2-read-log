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
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;

import com.exactpro.th2.common.grpc.Direction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestLogReaderConfiguration {
    @Test
    void deserializes() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new KotlinModule());
        try(var input = TestLogReaderConfiguration.class.getClassLoader().getResourceAsStream("test_cfg.json")) {
            LogReaderConfiguration cfg = objectMapper.readValue(input, LogReaderConfiguration.class);
            assertEquals(Duration.ofSeconds(5), cfg.getPullingInterval());
            assertEquals(Set.of("A", "B"), cfg.getAliases().keySet());
            assertEquals(Set.of(Direction.FIRST, Direction.SECOND), cfg.getAliases().get("A").getDirectionToPattern().keySet());
            assertEquals(Set.of(Direction.FIRST), cfg.getAliases().get("B").getDirectionToPattern().keySet());
            assertEquals(".*", Objects.requireNonNull(cfg.getAliases().get("A").getRegexp()).pattern());
            assertEquals("202.*$", Objects.requireNonNull(cfg.getAliases().get("B").getTimestampRegexp()).pattern());
            assertEquals(DateTimeFormatter.ofPattern("yyyy.MM.dd").getResolverFields(), Objects.requireNonNull(cfg.getAliases().get("B").getTimestampFormat()).getResolverFields());
        }
    }
}