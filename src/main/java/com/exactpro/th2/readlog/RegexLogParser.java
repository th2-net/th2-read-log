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

import java.io.StringWriter;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.exactpro.th2.read.file.common.StreamId;
import com.exactpro.th2.readlog.cfg.AliasConfiguration;
import com.exactpro.th2.readlog.cfg.Group;
import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import org.apache.commons.text.StringSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.opencsv.ICSVWriter.DEFAULT_ESCAPE_CHARACTER;
import static com.opencsv.ICSVWriter.DEFAULT_LINE_END;
import static com.opencsv.ICSVWriter.DEFAULT_QUOTE_CHARACTER;
import static java.util.stream.Collectors.toUnmodifiableList;

public class RegexLogParser {
    private static final Logger logger = LoggerFactory.getLogger(RegexLogParser.class);
    private final Map<String, AliasConfiguration> cfg;
    private final Group defaultGroup;

    public RegexLogParser(Map<String, AliasConfiguration> cfg) {
        this(cfg, null);
    }
    public RegexLogParser(Map<String, AliasConfiguration> cfg, Group defaultGroup) {
        this.cfg = Objects.requireNonNull(cfg, "'Cfg' parameter");
        if (cfg.isEmpty()) {
            throw new IllegalArgumentException("At least one alis must be specified");
        }
        this.defaultGroup = defaultGroup;
    }

    public LogData parse(StreamId streamId, String raw) {

        String sessionAlias = streamId.getSessionAlias();
        AliasConfiguration configuration = cfg.get(sessionAlias);
        if (configuration == null) {
            logger.error("Unknown alias {}, there no configuration", sessionAlias);
            throw new IllegalArgumentException("Unknown alias '" + sessionAlias +"'. No configuration found" );
        }

        LogData resultData = new LogData(Group.isDefault(configuration.getAliasGroup())
                ? defaultGroup
                : configuration.getAliasGroup()
        );

        Pattern directionPattern = Objects.requireNonNull(configuration.getDirectionToPattern().get(streamId.getDirection()),
                () -> "Pattern for direction " + streamId.getDirection() + " and session " + sessionAlias);
        Matcher matcher = directionPattern.matcher(raw);
        // check if the a string matches the direction from streamId
        // skip line if it is not ours direction
        if (!matcher.find()) {
            return resultData;
        }

        List<Integer> regexGroups = configuration.getGroups();
        if (configuration.isJoinGroups()) {
            parseBodyJoined(raw, configuration, resultData);
        } else {
            parseBody(raw, configuration.getRegexp(), regexGroups, resultData);
        }

        if (resultData.getBody().isEmpty()) {
            // fast way, nothing matches the regexp so we don't need to check for date pattern
            return resultData;
        }

        // Timestamp string from log
        Pattern datePattern = configuration.getTimestampRegexp();
        if (datePattern != null) {
            if (!lookForTimestamp(raw, datePattern, resultData)) {
                throw new IllegalStateException("The pattern '" + datePattern.pattern() + "' cannot extract the timestamp from the string: " + raw);
            }
        }

        // DateTime from log
        DateTimeFormatter timestampFormat = configuration.getTimestampFormat();
        if (timestampFormat != null) {
            ZoneOffset offset = Objects.requireNonNullElse(configuration.getTimestampZone(), ZoneId.systemDefault())
                    .getRules().getOffset(Instant.now());
            parseTimestamp(timestampFormat, resultData, offset);
        }

        if (resultData.getParsedTimestamp() != null && configuration.getSkipBefore() != null) {
            if (resultData.getParsedTimestamp().isBefore(configuration.getSkipBefore())) {
                logger.trace("Content dropped because of 'skipBefore' condition. Log timestamp: {}, Skip before: {}",
                        resultData.getParsedTimestamp(), configuration.getSkipBefore()
                );
                return LogData.EMPTY;
            }
        }

        return resultData;
    }

    private void parseTimestamp(DateTimeFormatter formatter, LogData data, ZoneOffset offset) {
        String rawTimestamp = data.getRawTimestamp();
        try {
            Instant dateTime = LocalDateTime.parse(rawTimestamp, formatter).toInstant(offset);
            data.setParsedTimestamp(dateTime);
            logger.trace("ParsedTimestamp: {}", dateTime);
        } catch (DateTimeException e) {
            throw new IllegalStateException("The timestamp '" + rawTimestamp + "' cannot be parsed using the '" + formatter + "' format", e);
        }
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

    private void parseBody(String text, Pattern pattern, List<Integer> groups, LogData data) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            var indexes = groups.isEmpty()
                    ? IntStream.rangeClosed(0, matcher.groupCount()).boxed().collect(Collectors.toList())
                    : groups;
            for (int i : indexes) {
                String res = matcher.group(i);
                data.addBody(res);
                logger.trace("ParsedLogLine: {}", res);
            }
        }
    }

    private void parseBodyJoined(String raw, AliasConfiguration configuration, LogData resultData) {
        Pattern pattern = configuration.getRegexp();
        Matcher matcher = pattern.matcher(raw);
        List<List<String>> valuesCollection = null;
        Map<String, String> headersFormat = configuration.getHeadersFormat();
        if (headersFormat.isEmpty()) {
            return;
        }
        while (matcher.find()) {
            if (valuesCollection == null) {
                valuesCollection = new ArrayList<>();
                valuesCollection.add(headersFormat.keySet().stream().collect(toUnmodifiableList()));
            }
            StringSubstitutor stringSubstitutor = createSubstitutor(matcher);
            valuesCollection.add(
                    headersFormat.values().stream()
                            .map(stringSubstitutor::replace)
                            .collect(Collectors.toUnmodifiableList())
            );
        }
        if (valuesCollection != null) {
            addJoined(resultData, valuesCollection, configuration.getGroupsJoinDelimiter().charAt(0));
        }
    }

    private void addJoined(LogData data, List<List<String>> values, char delimiter) {
        var writer = new StringWriter();
        ICSVWriter csvPrinter = createCsvWriter(writer, delimiter); // we can ignore closing because there is not IO
        for (List<String> value : values) {
            csvPrinter.writeNext(value.toArray(String[]::new));
        }

        String joinedData = writer.toString().trim();
        data.addBody(joinedData);
        logger.trace("Result after joining all groups: '{}'", joinedData);
    }

    private StringSubstitutor createSubstitutor(Matcher matcher) {
        StringSubstitutor stringSubstitutor = new StringSubstitutor(key -> {
            Integer index = tryParse(key);
            if (index == null) {
                return matcher.group(key);
            }
            if (index < 0) {
                throw new IllegalArgumentException("group index cannot be negative");
            }
            return matcher.group(index);
        });
        stringSubstitutor.setEnableUndefinedVariableException(true); // exception if key is unknown
        stringSubstitutor.setDisableSubstitutionInValues(true);
        return stringSubstitutor;
    }

    @NotNull
    private ICSVWriter createCsvWriter(StringWriter writer, char delimiter) {
        return new CSVWriter(writer, delimiter, DEFAULT_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END);
    }

    private Integer tryParse(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
