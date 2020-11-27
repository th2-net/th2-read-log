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

package com.exactpro.th2.readlog.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.th2.readlog.ILogReader;

import net.logstash.logback.argument.StructuredArguments;

public class LogReader implements ILogReader {
    private static final Logger logger = LoggerFactory.getLogger(LogReader.class);
    private final File file;
    private BufferedReader reader;

	private boolean closeState;
	private long processedLinesCount;

	public LogReader(File file) throws IOException {
		this.file = Objects.requireNonNull(file, "'File' parameter");
		open();
	}

    public void open() throws IOException {
        closeState = false;
        reader = new BufferedReader(new FileReader(file));
        logger.info("Open log file {}", StructuredArguments.value("file", file));
        processedLinesCount = 0;
    }

	public long getLineCount() throws IOException {

	    try (Stream<String> lines = Files.lines(file.toPath())) {
	        return lines.count();
	    }
	}

	public void skip(long lineNumber) throws IOException {
		logger.trace("Skipping {}",StructuredArguments.value("LinesToSkip",lineNumber));

		for (long i=0; i<lineNumber; ++i) {
			reader.readLine();
			++processedLinesCount;
		}
	}

	public long getProcessedLinesCount() {
		return processedLinesCount;
	}

	@Override
    @Nullable
    public String getNextLine() throws IOException {
		String result = reader.readLine();
		logger.trace("RawLogLine {}",StructuredArguments.value("RawLogLine",result));
		if (result != null) {
			++processedLinesCount;
		}
		return result;
	}

    @Override
    public boolean refresh() throws IOException {
        long linesCount = getLineCount();
        long processedLinesCount = getProcessedLinesCount();

        if (linesCount == processedLinesCount) {
            return false;
        }

        close();
        open();
        if (linesCount > processedLinesCount) {
            skip(processedLinesCount);
        }
        return true;
    }

    public boolean isClosed() {
		return closeState;
	}

	@Override
	public void close() throws IOException {
		if (reader != null) {
			reader.close();
			closeState=true;
		}
		logger.info("Close log file {}", StructuredArguments.value("fileName", file));
	}
}
