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

package com.exactpro.th2.readlog;

import java.io.IOException;

import javax.annotation.Nullable;

public interface ILogReader extends AutoCloseable {
    /**
     *
     * @return next line without line separators or {@code null} if doesn't have next line
     * @throws IOException
     */
    @Nullable
    String getNextLine() throws IOException;

    /**
     * Tries to refresh the state and check if new data is available.
     *
     * If this method have returned {@code true} the next call of {@link #getNextLine()} must return not null value.
     * @return {@code true} if any new data is available. Otherwise, returns {@code false}
     * @throws IOException
     */
    boolean refresh() throws IOException;
}
