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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final public class LogData {
    private List<String> body;
    private String rawTimestamp;
    private LocalDateTime parsedTimestamp;

    private ZoneId timestampZone;

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

    public LocalDateTime getParsedTimestamp() {
        return parsedTimestamp;
    }

    public void setParsedTimestamp(LocalDateTime localDateTime) {
        this.parsedTimestamp = localDateTime;
    }

    public ZoneId getTimestampZone() {
        return timestampZone;
    }

    public void setTimestampZone(ZoneId timestampZone) {
        this.timestampZone = timestampZone;
    }

    private void initIfNeeded() {
        if (body == null) {
            body = new ArrayList<>();
        }
    }
}
