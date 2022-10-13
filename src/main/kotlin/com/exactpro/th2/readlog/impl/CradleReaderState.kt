/*
 * Copyright 2022. Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.readlog.impl

import com.exactpro.cradle.CradleStorage
import com.exactpro.cradle.messages.StoredMessageId
import com.exactpro.th2.common.util.toCradleDirection
import com.exactpro.th2.read.file.common.StreamId
import com.exactpro.th2.read.file.common.state.ReaderState
import com.exactpro.th2.read.file.common.state.StreamData
import com.exactpro.th2.read.file.common.state.impl.InMemoryReaderState

class CradleReaderState private constructor(
    private val cradleStorage: CradleStorage,
    private val delegate: ReaderState,
): ReaderState by delegate {
    constructor(cradleStorage: CradleStorage) : this(cradleStorage, InMemoryReaderState())

    override fun get(streamId: StreamId): StreamData? {
        return delegate[streamId] ?: cradleStorage.getLastMessageIndex(
            streamId.sessionAlias,
            streamId.direction.toCradleDirection(),
        ).let { lastSequence ->
            when {
                lastSequence < 0 -> null
                else -> cradleStorage.getMessage(StoredMessageId(
                    streamId.sessionAlias,
                    streamId.direction.toCradleDirection(),
                    lastSequence
                )).run {
                    StreamData(
                        timestamp,
                        index,
                    )
                }
            }
        }
    }
}