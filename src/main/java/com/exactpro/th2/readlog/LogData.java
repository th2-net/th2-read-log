package com.exactpro.th2.readlog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

final public class LogData {
    private final List<String> body = new ArrayList<>();
    private String rawTimestamp;
    private LocalDateTime localDateTime;

    public List<String> getBody() {
        return body;
    }

    public String getRawTimestamp() {
        return rawTimestamp;
    }

    public void setRawTimestamp(String rawTimestamp) {
        this.rawTimestamp = rawTimestamp;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }
}
