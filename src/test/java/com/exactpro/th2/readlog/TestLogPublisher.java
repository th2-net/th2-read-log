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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.exactpro.th2.common.grpc.RawMessageBatch;
import com.exactpro.th2.common.schema.message.MessageRouter;

@DisplayName("Batch published")
public class TestLogPublisher {
    @SuppressWarnings("unchecked")
    private final MessageRouter<RawMessageBatch> routerMock = Mockito.mock(MessageRouter.class);

    @Nested
    @DisplayName("With default parameters")
    class TestDefaultParameters {

        private LogPublisher publisher;

        @BeforeEach
        void setup() {
            publisher = new LogPublisher("test", routerMock);
        }

        @Test
        @DisplayName("When lines count limit excited")
        void publishLinesCountLimit() throws IOException {
            int bound = LogPublisher.LINES_BATCH_LIMIT + 1;
            for (int it = 0; it < bound; it++) {
                publisher.publish(String.valueOf(it));
            }

            ArgumentCaptor<RawMessageBatch> argumentCaptor = ArgumentCaptor.forClass(RawMessageBatch.class);
            Mockito.verify(routerMock).sendAll(argumentCaptor.capture(), ArgumentMatchers.any());

            assertEquals(LogPublisher.LINES_BATCH_LIMIT, argumentCaptor.getValue().getMessagesCount());
        }
    }

    @Nested
    @DisplayName("With custom parameters")
    class TestCustomParameters {

        private LogPublisher publisher;

        @BeforeEach
        void setup() {
            publisher = new LogPublisher("test", routerMock, 10, 1000);
        }

        @Test
        @DisplayName("When lines count limit excited")
        void publishLinesCountLimit() throws IOException {
            int bound = 11;
            for (int it = 0; it < bound; it++) {
                publisher.publish(String.valueOf(it));
            }

            ArgumentCaptor<RawMessageBatch> argumentCaptor = ArgumentCaptor.forClass(RawMessageBatch.class);
            Mockito.verify(routerMock).sendAll(argumentCaptor.capture(), ArgumentMatchers.any());

            assertEquals(10, argumentCaptor.getValue().getMessagesCount());
        }

        @Test
        @DisplayName("When characters limit excited")
        void publishCharsCountLimit() throws IOException {
            int size = 501;
            String lineA = StringUtils.repeat('A', size);
            String lineB = StringUtils.repeat('B', size);
            publisher.publish(lineA);
            publisher.publish(lineB);

            ArgumentCaptor<RawMessageBatch> argumentCaptor = ArgumentCaptor.forClass(RawMessageBatch.class);
            Mockito.verify(routerMock).sendAll(argumentCaptor.capture(), ArgumentMatchers.any());

            RawMessageBatch value = argumentCaptor.getValue();
            assertEquals(1, value.getMessagesCount());
            assertArrayEquals(lineA.getBytes(), value.getMessages(0).getBody().toByteArray());
        }

        @Test
        @DisplayName("When publisher is closed with lines in cache")
        void publishLeftLinesOnClose() throws IOException {
            String line = "some unlucky line";
            publisher.publish(line);

            // should not publish because no limit excited
            Mockito.verify(routerMock, Mockito.never()).sendAll(ArgumentMatchers.any(RawMessageBatch.class), ArgumentMatchers.any());

            publisher.close();

            ArgumentCaptor<RawMessageBatch> argumentCaptor = ArgumentCaptor.forClass(RawMessageBatch.class);
            Mockito.verify(routerMock).sendAll(argumentCaptor.capture(), ArgumentMatchers.any());

            RawMessageBatch value = argumentCaptor.getValue();
            assertEquals(1, value.getMessagesCount());
            assertArrayEquals(line.getBytes(), value.getMessages(0).getBody().toByteArray());
        }
    }
}