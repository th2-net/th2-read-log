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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

public class AliasConfiguration {
    private final Pattern regexp;
    private final Pattern pathFilter;
    private List<Integer> groups = Collections.emptyList();
//    private List<SortingConfiguration> sortBy = Collections.emptyList();

    @JsonCreator
    public AliasConfiguration(
            @JsonProperty(value = "regexp", required = true) String regexp,
            @JsonProperty(value = "pathFilter") String pathFilter
    ) {
        this.regexp = Pattern.compile(Objects.requireNonNull(regexp, "'Regexp' parameter"));
        this.pathFilter = Pattern.compile(Objects.requireNonNull(pathFilter, "'Path filter' parameter"));
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

//    public List<SortingConfiguration> getSortBy() {
//        return sortBy;
//    }
//
//    public void setSortBy(List<SortingConfiguration> sortBy) {
//        this.sortBy = sortBy;
//    }
}