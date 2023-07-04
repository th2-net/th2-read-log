/*
 * Copyright 2022-2023. Exactpro (Exactpro Systems Limited)
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

import com.exactpro.cradle.BookId
import com.exactpro.cradle.CradleStorage
import com.exactpro.cradle.Order
import com.exactpro.cradle.messages.GroupedMessageFilter
import com.exactpro.th2.read.file.common.StreamId
import com.exactpro.th2.read.file.common.state.ReaderState
import com.exactpro.th2.read.file.common.state.StreamData
import com.exactpro.th2.read.file.common.state.impl.InMemoryReaderState
import com.google.protobuf.ByteString
import com.google.protobuf.UnsafeByteOperations
import java.time.Instant

class CradleReaderState private constructor(
    private val cradleStorage: CradleStorage,
    private val delegate: ReaderState,
    private val bookSupplier: (StreamId) -> String,
): ReaderState by delegate {
    constructor(cradleStorage: CradleStorage, bookSupplier: (StreamId) -> String)
            : this(cradleStorage, InMemoryReaderState(), bookSupplier)

    override fun get(streamId: StreamId): StreamData? {
        return delegate[streamId] ?: cradleStorage.getGroupedMessageBatches(
            GroupedMessageFilter.builder()
                .groupName(streamId.sessionAlias)
                .bookId(BookId(bookSupplier(streamId)))
                .timestampTo().isLessThanOrEqualTo(Instant.now())
                .limit(1)
                .order(Order.REVERSE)
                .build()
        ).asSequence().firstOrNull()?.lastMessage?.run {
            StreamData(
                timestamp,
                sequence,
                content?.let(UnsafeByteOperations::unsafeWrap) ?: ByteString.EMPTY,
            )
        }
    }
}