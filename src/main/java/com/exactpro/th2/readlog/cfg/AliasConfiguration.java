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

import com.exactpro.th2.common.grpc.Direction;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

public class AliasConfiguration {
    private static final Pattern ACCEPT_ANY = Pattern.compile(".*");
    private final Pattern regexp;
    private final Pattern pathFilter;
    private final Map<Direction, Pattern> directionToPattern;

    @JsonPropertyDescription("The regexp which will be used to get timestamp from log line")
    private final Pattern timestampRegexp;

    @JsonPropertyDescription("The format which will be used to parse matched timestamp")
    private final DateTimeFormatter timestampFormat;

    private List<Integer> groups = Collections.emptyList();

    private boolean joinGroups;

    private String groupsJoinDelimiter = ",";

    private Map<String, String> headersFormat = Collections.emptyMap();

    @JsonPropertyDescription("Defines time zone that should be used to parse the timestamp from the log file."
            + "It not set the time zone from the local machine will be taken")
    private ZoneId timestampZone;

    @JsonDeserialize(using = CharsetDeserializer.class)
    private Charset charset = StandardCharsets.UTF_8;

    @JsonCreator
    public AliasConfiguration(
            @JsonProperty(value = "regexp", required = true) String regexp,
            @JsonProperty(value = "pathFilter", required = true) String pathFilter,
            @JsonProperty(value = "directionRegexps") Map<Direction, String> directionRegexps,
            @JsonProperty(value = "timestampRegexp") String timestampRegexp,
            @JsonProperty(value = "timestampFormat") String timestampFormat
    ) {
        this.regexp = Pattern.compile(Objects.requireNonNull(regexp, "'Regexp' parameter"));
        this.pathFilter = Pattern.compile(Objects.requireNonNull(pathFilter, "'Path filter' parameter"));

        Map<Direction, Pattern> patternByDirection = new EnumMap<>(Direction.class);
        if (directionRegexps == null || directionRegexps.isEmpty()) {
            patternByDirection.put(Direction.FIRST, ACCEPT_ANY);
        } else {
            directionRegexps.forEach((direction, directionRegexp) -> patternByDirection.put(direction,
                    Pattern.compile(Objects.requireNonNull(directionRegexp, "'Direction regexp' parameter"))));
        }
        directionToPattern = Collections.unmodifiableMap(patternByDirection);
        this.timestampRegexp = timestampRegexp == null ? null : Pattern.compile(timestampRegexp);
        this.timestampFormat = StringUtils.isEmpty(timestampFormat)
                ? null
                : DateTimeFormatter.ofPattern(timestampFormat);
    }

    public Pattern getRegexp() {
        return regexp;
    }

    public Pattern getPathFilter() {
        return pathFilter;
    }

    public Map<Direction, Pattern> getDirectionToPattern() {
        return directionToPattern;
    }

    public List<Integer> getGroups() {
        return groups;
    }

    public void setGroups(List<Integer> groups) {
        this.groups = groups;
    }

    @Nullable
    public Pattern getTimestampRegexp() {
        return timestampRegexp;
    }

    @Nullable
    public DateTimeFormatter getTimestampFormat() {
        return timestampFormat;
    }

    public boolean isJoinGroups() {
        return joinGroups;
    }

    public void setJoinGroups(boolean joinGroups) {
        this.joinGroups = joinGroups;
    }

    public String getGroupsJoinDelimiter() {
        return groupsJoinDelimiter;
    }

    public void setGroupsJoinDelimiter(String groupsJoinDelimiter) {
        if (groupsJoinDelimiter.length() != 1) {
            throw new IllegalArgumentException("the delimiter '" + groupsJoinDelimiter + "' must contain only one character");
        }
        this.groupsJoinDelimiter = groupsJoinDelimiter;
    }

    public Map<String, String> getHeadersFormat() {
        return headersFormat;
    }

    public void setHeadersFormat(Map<String, String> headersFormat) {
        this.headersFormat = new TreeMap<>(headersFormat);
    }

    public ZoneId getTimestampZone() {
        return timestampZone;
    }

    public void setTimestampZone(ZoneOffset timestampZone) {
        this.timestampZone = timestampZone;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }
}
