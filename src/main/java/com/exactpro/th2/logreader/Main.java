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

package com.exactpro.th2.logreader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Object  {
	
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final int NOT_LIMIT = -1;
    private static final String BATCH_PER_SECOND_LIMIT_ENV = "BATCH_PER_SECOND_LIMIT";

    public static void main(String[] args) {

	    Properties props = new Properties();
	    
	    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	    
	    try (InputStream configStream = classLoader.getResourceAsStream("logback.xml"))  {
	        props.load(configStream);
	    } catch (IOException e) {
	        System.out.println("Error: can't laod log configuration file ");
	    }    			
            	     
	    RabbitMqClient client = new RabbitMqClient();
        
		try {
			client.connect();
		} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException
				| TimeoutException e) {
			logger.error("{}", e);
			System.exit(-1);
		}
        
		LogReader reader = new LogReader();
		
		client.setSessionAlias(reader.getFileName());
		
		RegexLogParser logParser = new RegexLogParser();

        int maxBatchesPerSecond = getMaxBatchesPerSecond();

        try {
            Instant lastResetTime = Instant.now();
            int batchesPublished = 0;
            boolean limited = maxBatchesPerSecond != NOT_LIMIT;
            if (limited) {
                logger.info("Publication is limited to {} batch(es) per second", maxBatchesPerSecond);
            } else {
                logger.info("Publication is unlimited");
            }

            while (true) {
                if (limited) {
                    Instant currentTime = Instant.now();
                    long timeSinceLastReset = Math.abs(Duration.between(lastResetTime, currentTime).toMillis());
                    if (timeSinceLastReset < 1_000) {
                        if (batchesPublished >= maxBatchesPerSecond) {
                            logger.trace("Suspend reading. Last time: {}, current time: {}, batches published: {}", lastResetTime, currentTime, batchesPublished);
                            Thread.sleep(1_000 - timeSinceLastReset);
                            continue;
                        }
                    } else {
                        lastResetTime = currentTime;
                        batchesPublished = 0;
                    }
                }
                String line = reader.getNextLine();
				
				if (line != null) {										
					ArrayList<String> parsedLines = logParser.parse(line);
					for (String parsedLine: parsedLines) {
                        if (client.publish(parsedLine)) {
                            batchesPublished++;
                        }
					}								
				} else {			
					long linesCount = reader.getLineCount();
					
					long processedLinesCount = reader.getProcessedLinesCount();
					
					if (linesCount > processedLinesCount) {
						reader.close();
						reader.open();
						reader.skip(processedLinesCount);	
					} else if (linesCount < processedLinesCount) {
						reader.close();
						reader.open();						
					} else {
						Thread.sleep(5000);
					}
				}
			}
		
		} catch (IOException| InterruptedException e) {
			logger.error("Error during log processing", e);
		} finally {
            try {
                reader.close();
            } catch (Exception e) {
                logger.error("Cannot close the reader", e);
            }
            try {
                client.close();
            } catch (Exception e) {
                logger.error("Cannot close the client", e);
            }
        }

	}

    private static int getMaxBatchesPerSecond() {
        String limitValue = System.getenv(BATCH_PER_SECOND_LIMIT_ENV);
        return limitValue == null
                ? NOT_LIMIT
                : verifyPositive(Integer.parseInt(limitValue.trim()),
                BATCH_PER_SECOND_LIMIT_ENV + " must be a positive integer but was " + limitValue);
    }

    private static int verifyPositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
