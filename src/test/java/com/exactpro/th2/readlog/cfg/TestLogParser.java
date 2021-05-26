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

import com.exactpro.th2.readlog.RegexLogParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLogParser {

    private final String TEST_MESSAGE_ALIAS = "testMessageAlias";
    private final String TEST_DATE_ALIAS = "testDateAlias";

    @Test
    void parser() {
        RegexLogParser logParser = new RegexLogParser(getConfiguration());
        String raw = "2021-03-23 13:21:37.991337479 QUICK.TEST INFO quicktest (Test.cpp:99) - fix message fix NewOrderSingle={ AuthenticationBlock={ UserID=\"qwrqwrq\" SessionKey=123456 } Header={ MsgTime=2021-Mar-21 21:21:21.210000000 CreationTime=2021-Mar-21 21:21:21.210000000 } NewOrder={ InstrumentBlock={ InstrSymbol=\"TEST_SYMBOL\" SecurityID=\"212121\" SecurityIDSource=TestSource SecurityExchange=\"test\" }}}";
        assertEquals("2021-03-23 13:21:37.991337479", logParser.parse(TEST_DATE_ALIAS, raw).get(0));
        assertEquals("NewOrderSingle={ AuthenticationBlock={ UserID=\"qwrqwrq\" SessionKey=123456 } Header={ MsgTime=2021-Mar-21 21:21:21.210000000 CreationTime=2021-Mar-21 21:21:21.210000000 } NewOrder={ InstrumentBlock={ InstrSymbol=\"TEST_SYMBOL\" SecurityID=\"212121\" SecurityIDSource=TestSource SecurityExchange=\"test\" }}}", logParser.parse(TEST_MESSAGE_ALIAS, raw).get(0));
    }

    private Map<String, AliasConfiguration> getConfiguration() {
        Map<String, AliasConfiguration> result = new HashMap<>();
        result.put(TEST_DATE_ALIAS, new AliasConfiguration("^202.+?(?= QUICK)", ""));
        AliasConfiguration messageAliasConfig = new AliasConfiguration("fix ([A-Za-z]+[=][\\{].*[\\}])", "");
        List<Integer> groupList = new ArrayList<>();
        groupList.add(1);
        messageAliasConfig.setGroups(groupList);
        result.put(TEST_MESSAGE_ALIAS, messageAliasConfig);
        return result;
    }
}
