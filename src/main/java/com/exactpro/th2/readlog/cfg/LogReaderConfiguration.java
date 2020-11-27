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

package com.exactpro.th2.readlog.cfg;

import java.io.File;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class LogReaderConfiguration {
    public static final int NO_LIMIT = -1;

    /**
     * @deprecated use {@link #logDirectory} and {@link #fileFilterRegexp} instead
     */
    @JsonProperty("log-file")
    @Deprecated(forRemoval = true)
    private File logFile;

    @JsonProperty("session-alias")
    private String sessionAlias;

    @JsonProperty("log-directory")
    private File logDirectory;

    @JsonProperty("file-filter-regexp")
    @JsonPropertyDescription("The regexp which will be used to filter files to process from specified directory")
    private String fileFilterRegexp;

    @JsonProperty(required = true)
    private String regexp;

    @JsonProperty("regexp-groups")
    private List<Integer> regexpGroups;

    @JsonProperty("max-batches-per-second")
    private int maxBatchesPerSecond = NO_LIMIT;

    public File getLogFile() {
        return logFile;
    }

    public void setLogFile(File logFile) {
        this.logFile = logFile;
    }

    public String getRegexp() {
        return regexp;
    }

    public void setRegexp(String regexp) {
        this.regexp = regexp;
    }

    public List<Integer> getRegexpGroups() {
        return regexpGroups;
    }

    public void setRegexpGroups(List<Integer> regexpGroups) {
        this.regexpGroups = regexpGroups;
    }

    public int getMaxBatchesPerSecond() {
        return maxBatchesPerSecond;
    }

    public void setMaxBatchesPerSecond(int maxBatchesPerSecond) {
        this.maxBatchesPerSecond = maxBatchesPerSecond;
    }

    public String getSessionAlias() {
        return sessionAlias;
    }

    public void setSessionAlias(String sessionAlias) {
        this.sessionAlias = sessionAlias;
    }

    public File getLogDirectory() {
        return logDirectory;
    }

    public void setLogDirectory(File logDirectory) {
        this.logDirectory = logDirectory;
    }

    public String getFileFilterRegexp() {
        return fileFilterRegexp;
    }

    public void setFileFilterRegexp(String fileFilterRegexp) {
        this.fileFilterRegexp = fileFilterRegexp;
    }
}
