/*
 * Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.exactpro.th2.read.file.common.StreamId;
import com.exactpro.th2.readlog.cfg.AliasConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegexLogParser {
    private static final Logger logger = LoggerFactory.getLogger(RegexLogParser.class);
    private final Map<String, AliasConfiguration> cfg;

    public RegexLogParser(Map<String, AliasConfiguration> cfg) {
        this.cfg = Objects.requireNonNull(cfg, "'Cfg' parameter");
        if (cfg.isEmpty()) {
            throw new IllegalArgumentException("At least one alis must be specified");
        }
    }

    public List<String> parse(String alias, String raw) {
        List<String> result = new ArrayList<>();
        AliasConfiguration configuration = cfg.get(alias);
        if (configuration == null) {
            logger.info("Unknown alias {}, there no configuration", alias);
            return Collections.emptyList();
        }

        Pattern pattern = configuration.getRegexp();
        List<Integer> regexGroups = configuration.getGroups();
        Matcher matcher = pattern.matcher(raw);

        if (regexGroups.isEmpty()) {
            while (matcher.find()) {
                for (int i = 0; i <= matcher.groupCount(); ++i) {
                    String res = matcher.group(i);
                    result.add(res);
                    logger.trace("ParsedLogLine: {}", res);
                }
            }
        } else {
            while (matcher.find()) {
                for (int index : regexGroups) {
                    String res = matcher.group(index);
                    result.add(res);
                    logger.trace("ParsedLogLine: {}", res);
                }
            }

        }

        return result;
    }
}
