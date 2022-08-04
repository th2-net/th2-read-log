/*
 * Copyright 2022 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.th2.readlog.cfg.Group.GroupDeserializer;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@JsonSerialize(using = ToStringSerializer.class)
@JsonDeserialize(using = GroupDeserializer.class)
public class Group {
    public static final Group USE_DEFAULT = new Group("default");
    private final String name;

    public Group(String name) {
        this.name = Objects.requireNonNull(name, "'Name' parameter");
        if (name.isBlank()) {
            throw new IllegalArgumentException("group name must not be blank");
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @SuppressWarnings("ObjectEquality")
    public static boolean isDefault(Group group) {
        return group == USE_DEFAULT;
    }

    public static class GroupDeserializer extends FromStringDeserializer<Group> {
        private static final long serialVersionUID = -322858958467896698L;

        protected GroupDeserializer() {
            super(Group.class);
        }

        @Override
        protected Group _deserialize(String value, DeserializationContext ctxt) {
            return new Group(value);
        }
    }
}
