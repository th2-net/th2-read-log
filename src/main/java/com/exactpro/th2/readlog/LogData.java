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
package com.exactpro.th2.readlog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.exactpro.th2.readlog.cfg.Group;

public final class LogData {
    public static LogData EMPTY = new LogData(List.of(), null);

    private List<String> body;
    private String rawTimestamp;
    private Instant parsedTimestamp;

    private final Group group;

    private LogData(List<String> body, Group group) {
        this.body = body;
        this.group = group;
    }

    public LogData(Group group) {
        this.group = group;
    }

    public void addBody(String item) {
        initIfNeeded();
        body.add(item);
    }

    public List<String> getBody() {
        return body == null ? Collections.emptyList() : body;
    }

    public String getRawTimestamp() {
        return rawTimestamp;
    }

    public void setRawTimestamp(String rawTimestamp) {
        this.rawTimestamp = rawTimestamp;
    }

    public Instant getParsedTimestamp() {
        return parsedTimestamp;
    }

    public void setParsedTimestamp(Instant localDateTime) {
        this.parsedTimestamp = localDateTime;
    }

    public Group getGroup() {
        return group;
    }

    private void initIfNeeded() {
        if (body == null) {
            body = new ArrayList<>();
        }
    }
}
