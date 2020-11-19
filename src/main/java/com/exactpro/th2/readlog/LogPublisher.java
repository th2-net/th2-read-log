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
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.th2.common.grpc.ConnectionID;
import com.exactpro.th2.common.grpc.Direction;
import com.exactpro.th2.common.grpc.MessageID;
import com.exactpro.th2.common.grpc.RawMessage;
import com.exactpro.th2.common.grpc.RawMessageBatch;
import com.exactpro.th2.common.grpc.RawMessageMetadata;
import com.exactpro.th2.common.schema.message.MessageRouter;
import com.exactpro.th2.common.schema.message.QueueAttribute;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;

/**
 * Accumulates lines in batches and publishes them.
 *
 * NOTE: This class is not thread-safe
 */
public class LogPublisher implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(LogPublisher.class);
    private static final int CHARACTER_BATCH_LIMIT = 100000000;
    private final int characterBatchLimit;
    static final int LINES_BATCH_LIMIT = 100;
    private final int linesBatchLimit;
    private final MessageRouter<RawMessageBatch> batchMessageRouter;
    private final String sessionAlias;

	private long index = 0;
	private final List<String> listOfLines = new ArrayList<String>();
	private long size = 0;
	private long lastPublishTs = Clock.systemDefaultZone().instant().getEpochSecond();

    public LogPublisher(String sessionAlias, MessageRouter<RawMessageBatch> batchMessageRouter) {
        this(sessionAlias, batchMessageRouter, LINES_BATCH_LIMIT, CHARACTER_BATCH_LIMIT);
    }

    LogPublisher(String sessionAlias, MessageRouter<RawMessageBatch> batchMessageRouter, int linesLimit, int charactersLimit) {
        this.sessionAlias = Objects.requireNonNull(sessionAlias, "'Session alias' parameter");
        this.batchMessageRouter = Objects.requireNonNull(batchMessageRouter, "'Batch message router' parameter");
        if (linesLimit <= 0) {
            throw new IllegalArgumentException("'linesLimit' must be a positive integer");
        }
        linesBatchLimit = linesLimit;

        if (charactersLimit <= 0) {
            throw new IllegalArgumentException("'charactersLimit' must be a positive integer");
        }
        characterBatchLimit = charactersLimit;
    }

	private void publish() throws IOException {
		RawMessageBatch.Builder builder = RawMessageBatch.newBuilder();

		for (String str: listOfLines) {
			RawMessage.Builder msgBuilder = builder.addMessagesBuilder();

			ByteString body = ByteString.copyFrom(str.getBytes());

			msgBuilder.setBody(body);

			RawMessageMetadata.Builder metaData = RawMessageMetadata.newBuilder();

			Timestamp.Builder ts = Timestamp.newBuilder();

			Clock clock = Clock.systemDefaultZone();
			Instant instant = clock.instant();

			ts.setSeconds(instant.getEpochSecond());
			ts.setNanos(instant.getNano());

			metaData.setTimestamp(ts);

			MessageID.Builder messageId = MessageID.newBuilder();

			ConnectionID.Builder connId = ConnectionID.newBuilder();

			connId.setSessionAlias(sessionAlias);

			messageId.setConnectionId(connId);
			messageId.setSequence(++index);
			messageId.setDirection(Direction.FIRST);

			metaData.setId(messageId);

			msgBuilder.setMetadata(metaData);						
		}

		listOfLines.clear();

        RawMessageBatch batch = builder.build();

        if (batch.getMessagesCount() > 0) {
            batchMessageRouter.sendAll(batch, QueueAttribute.PUBLISH.toString(), QueueAttribute.RAW.toString());

            logger.trace("Raw batch published: {}", JsonFormat.printer().omittingInsignificantWhitespace().print(batch));
        } else {
            logger.trace("Skip publishing empty batch");
        }
    }

    public boolean publish(String line) throws IOException {
        int lineLength = line.getBytes().length;
        if (lineLength > characterBatchLimit) {
            throw new IllegalArgumentException("The input line must not be longer than " + characterBatchLimit + " but was " + lineLength);
        }

        boolean published = false;

        if (size + lineLength > characterBatchLimit) {
            resetAndPublish();
            published = true;
        }
		size += lineLength;

		listOfLines.add(line);

		if (	(listOfLines.size() >= linesBatchLimit) ||
				(Clock.systemDefaultZone().instant().getEpochSecond() - lastPublishTs > 2)) {

            resetAndPublish();
            return true;
        }
        return published;
	}

    private void resetAndPublish() throws IOException {
        lastPublishTs = Clock.systemDefaultZone().instant().getEpochSecond();
        size = 0;

        publish();
    }

    @Override
    public void close() throws IOException {
        if (!listOfLines.isEmpty()) {
            // we need to publish all data left
            publish();
        }

        logger.info("Publisher closed");
    }
}
