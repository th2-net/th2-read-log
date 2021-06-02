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

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;

public class AliasConfiguration {
    private final Pattern regexp;
    private final Pattern pathFilter;

    @JsonPropertyDescription("The regexp which will be used to get timestamp from log line")
    private final Pattern timestampRegexp;

    @JsonPropertyDescription("The format which will be used to parse matched timestamp")
    private final String timestampFormat;

    private List<Integer> groups = Collections.emptyList();

    @JsonCreator
    public AliasConfiguration(
            @JsonProperty(value = "regexp", required = true) String regexp,
            @JsonProperty(value = "pathFilter", required = true) String pathFilter,
            @JsonProperty(value = "timestampRegexp") String timestampRegexp,
            @JsonProperty(value = "timestampFormat") String timestampFormat
    ) {
        this.regexp = Pattern.compile(Objects.requireNonNull(regexp, "'Regexp' parameter"));
        this.pathFilter = Pattern.compile(Objects.requireNonNull(pathFilter, "'Path filter' parameter"));
        this.timestampRegexp = timestampRegexp != null ? Pattern.compile(timestampRegexp) : null;
        this.timestampFormat = timestampFormat;
    }

    public Pattern getRegexp() {
        return regexp;
    }

    public Pattern getPathFilter() {
        return pathFilter;
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
    public String getTimestampFormat() {
        return timestampFormat;
    }
}
