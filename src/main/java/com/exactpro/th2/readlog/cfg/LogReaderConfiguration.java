package com.exactpro.th2.readlog.cfg;

import java.io.File;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LogReaderConfiguration {

    @JsonProperty(value = "log-file", required = true)
    private File logFile;

    @JsonProperty(required = true)
    private String regexp;

    @JsonProperty("regexp-groups")
    private List<Integer> regexpGroups;

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
}
