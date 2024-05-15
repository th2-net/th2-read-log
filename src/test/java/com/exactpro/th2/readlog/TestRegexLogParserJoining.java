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

package com.exactpro.th2.readlog;

import java.util.List;
import java.util.Map;

import com.exactpro.th2.common.grpc.Direction;
import com.exactpro.th2.read.file.common.StreamId;
import com.exactpro.th2.readlog.cfg.AliasConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestRegexLogParserJoining {
    @Test
    void joinsIfOneMatchFound() {
        AliasConfiguration configuration = new AliasConfiguration(
                "(\\S+),\\s+(\\d+)",
                ".*",
                null,
                null,
                null
        );
        configuration.setJoinGroups(true);
        configuration.setHeadersFormat(Map.of(
                "HeaderA", "Whole string: ${0}",
                "HeaderB", "Group 1: ${1}",
                "HeaderC", "Group 2: ${2}"
        ));
        RegexLogParser parser = new RegexLogParser(Map.of("test", configuration));

        LogData data = parser.parse(new StreamId("test"), "this a test string, 123 and no more");
        List<String> body = data.getBody();
        Assertions.assertEquals(1, body.size(), () -> "Unexpected strings: " + body);
        Assertions.assertEquals(
                "\"HeaderA\",\"HeaderB\",\"HeaderC\"\n"
                        + "\"Whole string: string, 123\",\"Group 1: string\",\"Group 2: 123\"",
                body.get(0));
    }

    @Test
    void doesNotTryToSubstituteInResultString() {
        AliasConfiguration configuration = new AliasConfiguration(
                "(] )(.*$)",
                ".*",
                null,
                null,
                null
        );
        configuration.setJoinGroups(true);
        configuration.setHeadersFormat(Map.of(
                "Header", "Group 2: ${2}"
        ));
        RegexLogParser parser = new RegexLogParser(Map.of("test", configuration));

        LogData data = parser.parse(new StreamId("test"), "[test] should not try to process ${3} and ${variable}");
        List<String> body = data.getBody();
        Assertions.assertEquals(1, body.size(), () -> "Unexpected strings: " + body);
        Assertions.assertEquals(
                "\"Header\"\n"
                        + "\"Group 2: should not try to process ${3} and ${variable}\"",
                body.get(0));
    }

    @Test
    void joinsIfManyMatchesFound() {
        AliasConfiguration configuration = new AliasConfiguration(
                "(\\S+),\\s+(\\d+)",
                ".*",
                null,
                null,
                null
        );
        configuration.setJoinGroups(true);
        configuration.setHeadersFormat(Map.of(
                "HeaderA", "${1}",
                "HeaderB", "${2}"
        ));
        RegexLogParser parser = new RegexLogParser(Map.of("test", configuration));

        LogData data = parser.parse(new StreamId("test"), "A, 42; B, 53");
        List<String> body = data.getBody();
        Assertions.assertEquals(1, body.size(), () -> "Unexpected strings: " + body);
        Assertions.assertEquals(
                "\"HeaderA\",\"HeaderB\"\n"
                        + "\"A\",\"42\"\n"
                        + "\"B\",\"53\"",
                body.get(0));
    }

    @Test
    void usesCorrectDelimiter() {
        AliasConfiguration configuration = new AliasConfiguration(
                "(\\S+),\\s+(\\d+)",
                ".*",
                null,
                null,
                null
        );
        configuration.setJoinGroups(true);
        configuration.setGroupsJoinDelimiter("\t");
        configuration.setHeadersFormat(Map.of(
                "HeaderA", "${1}",
                "HeaderB", "${2}"
        ));
        RegexLogParser parser = new RegexLogParser(Map.of("test", configuration));

        LogData data = parser.parse(new StreamId("test"), "A, 42");
        List<String> body = data.getBody();
        Assertions.assertEquals(1, body.size(), () -> "Unexpected strings: " + body);
        Assertions.assertEquals(
                "\"HeaderA\"\t\"HeaderB\"\n"
                        + "\"A\"\t\"42\"",
                body.get(0));
    }

    @Test
    void usesConstantsFromMap() {
        AliasConfiguration configuration = new AliasConfiguration(
                "(\\S+),\\s+(\\d+)",
                ".*",
                null,
                null,
                null
        );
        configuration.setJoinGroups(true);
        configuration.setHeadersFormat(Map.of(
                "HeaderA", "${1}",
                "HeaderB", "${2}",
                "HeaderC", "const"
        ));
        RegexLogParser parser = new RegexLogParser(Map.of("test", configuration));

        LogData data = parser.parse(new StreamId("test"), "A, 42; B, 53");
        List<String> body = data.getBody();
        Assertions.assertEquals(1, body.size(), () -> "Unexpected strings: " + body);
        Assertions.assertEquals(
                "\"HeaderA\",\"HeaderB\",\"HeaderC\"\n"
                        + "\"A\",\"42\",\"const\"\n"
                        + "\"B\",\"53\",\"const\"",
                body.get(0));
    }

    @Test
    void correctlyAcceptsGroupsByName() {
        AliasConfiguration configuration = new AliasConfiguration(
                "(?<A>\\S+),\\s+(?<B>\\d+)",
                ".*",
                null,
                null,
                null
        );
        configuration.setJoinGroups(true);
        configuration.setHeadersFormat(Map.of(
                "HeaderA", "${A}",
                "HeaderB", "${B}"
        ));
        RegexLogParser parser = new RegexLogParser(Map.of("test", configuration));

        LogData data = parser.parse(new StreamId("test"), "A, 42; B, 53");
        List<String> body = data.getBody();
        Assertions.assertEquals(1, body.size(), () -> "Unexpected strings: " + body);
        Assertions.assertEquals(
                "\"HeaderA\",\"HeaderB\"\n"
                        + "\"A\",\"42\"\n"
                        + "\"B\",\"53\"",
                body.get(0));
    }
}
