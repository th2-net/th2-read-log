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

import com.exactpro.th2.common.grpc.Direction;
import com.exactpro.th2.read.file.common.StreamId;
import com.exactpro.th2.readlog.cfg.AliasConfiguration;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class TestLogParser {

    static final String TEST_MESSAGE_ALIAS = "tma";
    static final String TEST_MESSAGE_ALIAS_WRONG_TIMESTAMP_FORMAT = "tma_wrong_format";
    static final String TEST_MESSAGE_ALIAS_WRONG_TIMESTAMP_PATTERN = "tma_wrong_pattern";
    static final String RAW_LOG_IN = "2021-03-23 13:21:37.991337479 QUICK.TEST INFO quicktest (Test.cpp:99) - incoming fix message fix NewOrderSingle={ AuthenticationBlock={ UserID=\"qwrqwrq\" SessionKey=123456 } Header={ MsgTime=2021-Mar-21 21:21:21.210000000 CreationTime=2021-Mar-21 21:21:21.210000000 } NewOrder={ InstrumentBlock={ InstrSymbol=\"TEST_SYMBOL\" SecurityID=\"212121\" SecurityIDSource=TestSource SecurityExchange=\"test\" }}}";
    static final String RAW_LOG_OUT = "2021-03-23 13:21:37.991337479 QUICK.TEST INFO quicktest (Test.cpp:99) - outgoing fix message fix NewOrderSingle={ AuthenticationBlock={ UserID=\"qwrqwrq\" SessionKey=123456 } Header={ MsgTime=2021-Mar-21 21:21:21.210000000 CreationTime=2021-Mar-21 21:21:21.210000000 } NewOrder={ InstrumentBlock={ InstrSymbol=\"TEST_SYMBOL\" SecurityID=\"212121\" SecurityIDSource=TestSource SecurityExchange=\"test\" }}}";

    @ParameterizedTest(name = "Has message: {1}, Skips before: {0}")
    @MethodSource("skipMessageParams")
    void skipMessageByTimestamp(Instant skipBefore, boolean hasMessages) {
        AliasConfiguration cfg = new AliasConfiguration(
                ".+",
                ".*",
                Map.of(),
                "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.\\d{3,9}",
                "yyyy-MM-dd HH:mm:ss.SSSSSSSSS"
        );
        cfg.setGroups(List.of(0));
        cfg.setTimestampZone(ZoneOffset.UTC);
        cfg.setSkipBefore(skipBefore);
        RegexLogParser parser = new RegexLogParser(Map.of(
                "test", cfg
        ));

        LogData data = parser.parse(new StreamId("test"), "2021-03-23 13:21:37.991337479 some data");
        if (hasMessages) {
            assertEquals(Direction.FIRST, data.getDirection(), "unexpected direction");
        }
        assertEquals(hasMessages, !data.getBody().isEmpty(), () -> "unexpected data " + data.getBody());
        if (hasMessages) {
            assertEquals(List.of("2021-03-23 13:21:37.991337479 some data"), data.getBody());
        }
    }

    static List<Arguments> skipMessageParams() {
        return List.of(
                arguments(Instant.parse("2021-03-23T13:21:38Z"), false),
                arguments(Instant.parse("2021-03-23T13:21:36Z"), true)
        );
    }

    @ParameterizedTest
    @EnumSource(value = Direction.class, mode = EnumSource.Mode.EXCLUDE, names = "UNRECOGNIZED")
    void parser(Direction direction) {
        RegexLogParser logParser = new RegexLogParser(getConfiguration());
        LogData data = logParser.parse(new StreamId(TEST_MESSAGE_ALIAS), direction == Direction.FIRST ? RAW_LOG_IN : RAW_LOG_OUT);
        assertEquals(direction, data.getDirection(), "unexpected direction");
        assertEquals(1, data.getBody().size());
        assertEquals("NewOrderSingle={ AuthenticationBlock={ UserID=\"qwrqwrq\" SessionKey=123456 } Header={ MsgTime=2021-Mar-21 21:21:21.210000000 CreationTime=2021-Mar-21 21:21:21.210000000 } NewOrder={ InstrumentBlock={ InstrSymbol=\"TEST_SYMBOL\" SecurityID=\"212121\" SecurityIDSource=TestSource SecurityExchange=\"test\" }}}", data.getBody().get(0));
        assertEquals("2021-03-23 13:21:37.991337479", data.getRawTimestamp());
        assertEquals(Instant.parse("2021-03-23T13:21:37.991337479Z"), data.getParsedTimestamp());
    }

    @Test
    void parserErrors() {
        RegexLogParser logParser = new RegexLogParser(getConfiguration());

        assertAll(
                () -> {
                    var ex = assertThrows(IllegalStateException.class,
                            () -> logParser.parse(new StreamId(TEST_MESSAGE_ALIAS_WRONG_TIMESTAMP_FORMAT), RAW_LOG_IN));
                    Assertions.assertTrue(
                            ex.getMessage().startsWith("The timestamp '2021-03-23 13:21:37.991337479' cannot be parsed"),
                            () -> "Actual error: " + ex.getMessage());
                },
                () -> {
                    var ex = assertThrows(IllegalStateException.class,
                            () -> logParser.parse(new StreamId(TEST_MESSAGE_ALIAS_WRONG_TIMESTAMP_PATTERN), RAW_LOG_IN));
                    Assertions.assertTrue(
                            ex.getMessage().startsWith("The pattern '3012.*' cannot extract the timestamp from the string"),
                            () -> "Actual error: " + ex.getMessage());
                },
                () -> {
                    var ex = assertThrows(IllegalArgumentException.class,
                            () -> logParser.parse(new StreamId("wrong_alias"), RAW_LOG_IN));
                    Assertions.assertTrue(
                            ex.getMessage().startsWith("Unknown alias 'wrong_alias'. No configuration found"),
                            () -> "Actual error: " + ex.getMessage());
                }
        );
    }

    private Map<String, AliasConfiguration> getConfiguration() {
        String regexp = "fix ([A-Za-z]+[=][\\{].*[\\}])";
        String timextampRegexp = "^202.+?(?= QUICK)";
        String timestampFormat = "yyyy-MM-dd HH:mm:ss.SSSSSSSSS";

        Map<String, AliasConfiguration> result = new HashMap<>();
        AliasConfiguration cfg = new AliasConfiguration(regexp, "",
                Map.of(Direction.FIRST, "incoming", Direction.SECOND, "outgoing"),
                timextampRegexp, timestampFormat);
        cfg.setTimestampZone(ZoneOffset.UTC);
        result.put(TEST_MESSAGE_ALIAS, cfg);
        result.put(TEST_MESSAGE_ALIAS_WRONG_TIMESTAMP_FORMAT, new AliasConfiguration(regexp, "", Collections.emptyMap(), timextampRegexp, "123"));
        result.put(TEST_MESSAGE_ALIAS_WRONG_TIMESTAMP_PATTERN, new AliasConfiguration(regexp, "", Collections.emptyMap(), "3012.*", timestampFormat));

        List<Integer> groupList = new ArrayList<>();
        groupList.add(1);
        result.values().forEach(it -> it.setGroups(groupList));

        return result;
    }
}
