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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonSubTypes({
        @JsonSubTypes.Type(name = "NUMBER", value = SortingConfiguration.NumberSortingConfiguration.class),
        @JsonSubTypes.Type(name = "TIME", value = SortingConfiguration.TimeSortingConfiguration.class),
        @JsonSubTypes.Type(name = "STRING", value = SortingConfiguration.StringSortingConfiguration.class)
})
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "compare-as",
        visible = true
)
public abstract class SortingConfiguration {
    private final String regexp;
    private final boolean reversed;
    private final CompareAs compareAs;

    public SortingConfiguration(String regexp, CompareAs compareAs, @JsonProperty(defaultValue = "false") boolean reversed) {
        this.regexp = Objects.requireNonNull(regexp, "'Regexp' parameter");
        this.compareAs = Objects.requireNonNull(compareAs, "'Compare as' parameter");
        this.reversed = reversed;
    }

    public String getRegexp() {
        return regexp;
    }

    public boolean isReversed() {
        return reversed;
    }

    public CompareAs getCompareAs() {
        return compareAs;
    }

    public enum CompareAs {
        NUMBER, TIME, STRING
    }

    public static class NumberSortingConfiguration extends SortingConfiguration {
        public NumberSortingConfiguration(String regexp, @JsonProperty(defaultValue = "false") boolean reversed) {
            super(regexp, CompareAs.NUMBER, reversed);
        }
    }

    public static class TimeSortingConfiguration extends SortingConfiguration {
        private final String timeFormat;

        public TimeSortingConfiguration(String regexp, @JsonProperty(defaultValue = "false") boolean reversed, String timeFormat) {
            super(regexp, CompareAs.TIME, reversed);
            this.timeFormat = Objects.requireNonNull(timeFormat, "'Time format' parameter");
        }

        public String getTimeFormat() {
            return timeFormat;
        }
    }

    public static class StringSortingConfiguration extends SortingConfiguration {
        public StringSortingConfiguration(String regexp, @JsonProperty(defaultValue = "false")  boolean reversed) {
            super(regexp, CompareAs.STRING, reversed);
        }
    }
}
