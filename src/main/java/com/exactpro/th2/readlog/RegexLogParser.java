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

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public LogData parse(String alias, String raw) {
        LogData resultData = new LogData();

        AliasConfiguration configuration = cfg.get(alias);
        if (configuration == null) {
            logger.error("Unknown alias {}, there no configuration", alias);
            return new LogData();
        }

        List<Integer> regexGroups = configuration.getGroups();
        if (regexGroups.isEmpty()) {
            parseBody(raw, configuration.getRegexp(), resultData);
        } else {
            parseBody(raw, configuration.getRegexp(), regexGroups, resultData);
        }

        // Timestamp string from log
        Pattern datePattern = configuration.getTimestampRegexp();
        if (datePattern!=null) {
            if (!lookForTimestamp(raw, datePattern, resultData)) return new LogData();
        }

        // DateTime from log
        String timestampFormat = configuration.getTimestampFormat();
        if (timestampFormat != null && !timestampFormat.isEmpty()) {
            if (!parseTimestamp(timestampFormat, resultData)) return new LogData();
        }

        return resultData;
    }

    private boolean parseTimestamp(String format, LogData data) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            LocalDateTime dateTime = LocalDateTime.parse(data.getRawTimestamp(), formatter);
            data.setParsedTimestamp(dateTime);
            logger.trace("ParsedTimestamp: {}", dateTime.toString());
        } catch (DateTimeException e) {
            logger.error("Timestamp \"{}\" can't be parsed to format \"{}\"", data.getRawTimestamp(),  format);
            return false;
        }
        return true;
    }

    private boolean lookForTimestamp(String text, Pattern pattern, LogData data) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            logger.error("Timestamp with regex \"{}\" was not found in the log", pattern.pattern());
            return false;
        }
        String res = matcher.group(0);
        data.setRawTimestamp(res);
        logger.trace("Found timestamp: {}", res);
        return true;
    }

    private void parseBody(String text, Pattern pattern, LogData data) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            for (int i = 0; i <= matcher.groupCount(); ++i) {
                String res = matcher.group(i);
                data.addBody(res);
                logger.trace("ParsedLogLine: {}", res);
            }
        }
    }

    private void parseBody(String text, Pattern pattern, List<Integer> groups, LogData data) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            for (int index : groups) {
                String res = matcher.group(index);
                data.addBody(res);
                logger.trace("ParsedLogLine: {}", res);
            }
        }
    }
}
