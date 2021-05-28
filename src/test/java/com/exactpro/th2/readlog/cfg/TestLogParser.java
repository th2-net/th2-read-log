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

import com.exactpro.th2.readlog.LogData;
import com.exactpro.th2.readlog.RegexLogParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLogParser {

    static final String TEST_MESSAGE_ALIAS = "tma";
    static final String TEST_MESSAGE_ALIAS_WRONG_TIMESTAMP_FORMAT = "tma_wrong_format";
    static final String TEST_MESSAGE_ALIAS_WRONG_TIMESTAMP_PATTERN = "tma_wrong_pattern";
    static final String RAW_LOG = "2021-03-23 13:21:37.991337479 QUICK.TEST INFO quicktest (Test.cpp:99) - fix message fix NewOrderSingle={ AuthenticationBlock={ UserID=\"qwrqwrq\" SessionKey=123456 } Header={ MsgTime=2021-Mar-21 21:21:21.210000000 CreationTime=2021-Mar-21 21:21:21.210000000 } NewOrder={ InstrumentBlock={ InstrSymbol=\"TEST_SYMBOL\" SecurityID=\"212121\" SecurityIDSource=TestSource SecurityExchange=\"test\" }}}";

    @Test
    void parser() {
        RegexLogParser logParser = new RegexLogParser(getConfiguration());
        LogData data = logParser.parse(TEST_MESSAGE_ALIAS, RAW_LOG);
        assertEquals(1, data.getBody().size());
        assertEquals("NewOrderSingle={ AuthenticationBlock={ UserID=\"qwrqwrq\" SessionKey=123456 } Header={ MsgTime=2021-Mar-21 21:21:21.210000000 CreationTime=2021-Mar-21 21:21:21.210000000 } NewOrder={ InstrumentBlock={ InstrSymbol=\"TEST_SYMBOL\" SecurityID=\"212121\" SecurityIDSource=TestSource SecurityExchange=\"test\" }}}", data.getBody().get(0));
        assertEquals("2021-03-23 13:21:37.991337479", data.getRawTimestamp());
        assertEquals(2021, data.getLocalDateTime().getYear());
        assertEquals(23, data.getLocalDateTime().getDayOfMonth());
        assertEquals(3, data.getLocalDateTime().getMonthValue());
        assertEquals(13, data.getLocalDateTime().getHour());
        assertEquals(21, data.getLocalDateTime().getMinute());
        assertEquals(37, data.getLocalDateTime().getSecond());
        assertEquals(991337479, data.getLocalDateTime().getNano());
    }

    @Test
    void parserErrors() {
        RegexLogParser logParser = new RegexLogParser(getConfiguration());

        LogData data = logParser.parse(TEST_MESSAGE_ALIAS_WRONG_TIMESTAMP_FORMAT, RAW_LOG);
        assertEquals(0, data.getBody().size());

        data = logParser.parse(TEST_MESSAGE_ALIAS_WRONG_TIMESTAMP_PATTERN, RAW_LOG);
        assertEquals(0, data.getBody().size());

        data = logParser.parse("wrong_alias", RAW_LOG);
        assertEquals(0, data.getBody().size());
    }

    private Map<String, AliasConfiguration> getConfiguration() {
        String regexp = "fix ([A-Za-z]+[=][\\{].*[\\}])";
        String timextampRegexp = "^202.+?(?= QUICK)";
        String timestampFormat = "yyyy-MM-dd HH:mm:ss.SSSSSSSSS";

        Map<String, AliasConfiguration> result = new HashMap<>();
        result.put(TEST_MESSAGE_ALIAS, new AliasConfiguration(regexp, "", timextampRegexp, timestampFormat));
        result.put(TEST_MESSAGE_ALIAS_WRONG_TIMESTAMP_FORMAT, new AliasConfiguration(regexp, "", timextampRegexp, "123"));
        result.put(TEST_MESSAGE_ALIAS_WRONG_TIMESTAMP_PATTERN, new AliasConfiguration(regexp, "", "3012.*", timestampFormat));

        List<Integer> groupList = new ArrayList<>();
        groupList.add(1);
        result.values().forEach(it -> it.setGroups(groupList));

        return result;
    }
}
