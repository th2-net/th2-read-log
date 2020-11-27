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

import static com.exactpro.th2.readlog.cfg.LogReaderConfiguration.NO_LIMIT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.th2.common.metrics.CommonMetrics;
import com.exactpro.th2.common.schema.factory.CommonFactory;
import com.exactpro.th2.readlog.cfg.LogReaderConfiguration;
import com.exactpro.th2.readlog.impl.DirectoryWatchLogReader;
import com.exactpro.th2.readlog.impl.LogReader;

public class Main extends Object  {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
        Deque<AutoCloseable> toDispose = new ArrayDeque<>();
        AtomicBoolean complete = new AtomicBoolean();

        // Probably we should not use shutdown hook in application that does everything in the Main thread.
        // But I believe that it will be changed soon the and reader will become multithreading-reader.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> closeResources(toDispose, complete), "Shutdown hook"));

        CommonMetrics.setLiveness(true);
        CommonFactory commonFactory = CommonFactory.createFromArguments(args);
        toDispose.add(commonFactory);

        LogReaderConfiguration configuration = commonFactory.getCustomConfiguration(LogReaderConfiguration.class);

        LOGGER.info("Deprecated parameter 'logFile'={}", configuration.getLogFile());
        File logFile = configuration.getLogFile();
        String sessionAlias = logFile == null
                ? configuration.getSessionAlias()
                : logFile.getName(); // for backward compatibility
        LogPublisher publisher = new LogPublisher(sessionAlias, commonFactory.getMessageRouterRawBatch());
        toDispose.add(publisher);


		RegexLogParser logParser = new RegexLogParser(configuration.getRegexp(), configuration.getRegexpGroups());
		CommonMetrics.setReadiness(true);

		try {
            ILogReader reader = logFile == null
                    ? new DirectoryWatchLogReader(configuration.getLogDirectory(), configuration.getFileFilterRegexp())
                    : new LogReader(logFile); // for backward compatibility
            toDispose.add(reader);

            int maxBatchesPerSecond = configuration.getMaxBatchesPerSecond();
            boolean limited = maxBatchesPerSecond != NO_LIMIT;
            if (limited) {
                verifyPositive(maxBatchesPerSecond, "'maxBatchesPerSecond' must be a positive integer but was " + maxBatchesPerSecond);
                LOGGER.info("Publication is limited to {} batch(es) per second", maxBatchesPerSecond);
            } else {
                LOGGER.info("Publication is unlimited");
            }

            long lastResetTime = System.currentTimeMillis();
            int batchesPublished = 0;

            while (!complete.get()) {
                if (limited) {
                    if (batchesPublished >= maxBatchesPerSecond) {
                        long currentTime = System.currentTimeMillis();
                        long timeSinceLastReset = Math.abs(currentTime - lastResetTime);
                        if (timeSinceLastReset < 1_000) {
                            LOGGER.trace("Suspend reading. Last time: {} mills, current time: {} mills, batches published: {}", lastResetTime, currentTime,
                                    batchesPublished);
                            Thread.sleep(1_000 - timeSinceLastReset);
                            continue;
                        }
                        lastResetTime = currentTime;
                        batchesPublished = 0;
                    }
                }

                String line = reader.getNextLine();

				if (line != null) {
					List<String> parsedLines = logParser.parse(line);
					for (String parsedLine: parsedLines) {
                        if (publisher.publish(parsedLine)) {
                            batchesPublished++;
                        }
					}
				} else {
					if (!reader.refresh()) {
					    publisher.flush();
					    // wait for file update
						Thread.sleep(5000);
					}
				}
			}

		} catch (IOException| InterruptedException e) {
			LOGGER.error("Cannot read log file: {}", logFile, e);
		}
	}

    private static void closeResources(Deque<AutoCloseable> toDispose, AtomicBoolean complete) {
        CommonMetrics.setReadiness(false);
        complete.set(true);
        toDispose.descendingIterator().forEachRemaining(resource -> {
            try {
                resource.close();
            } catch (Exception e) {
                LOGGER.error("Cannot close resource", e);
            }
        });
        CommonMetrics.setLiveness(false);
    }

    private static int verifyPositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
