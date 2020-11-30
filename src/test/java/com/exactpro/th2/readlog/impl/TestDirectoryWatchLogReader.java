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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.th2.readlog.ILogReader;

@TestInstance(Lifecycle.PER_METHOD)
class TestDirectoryWatchLogReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDirectoryWatchLogReader.class);
    private File logDirectory;

    @BeforeEach
    void setUp() throws IOException {
        logDirectory = Files.createTempDirectory("read-log").toFile();
        LOGGER.info("Directory created: {}", logDirectory);
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(logDirectory);
        LOGGER.info("Directory {} deleted", logDirectory);
    }

    @Test
    void readsWholeFile() throws Exception {
        createFile("test_file", List.of("line A", "line B", "line C"));

        try (ILogReader reader = getReader()) {
            List<String> lines = readAllLines(reader);

            assertEquals(List.of("line A", "line B"), lines);
            assertCanRead(reader);
            List<String> lastLine = readAllLines(reader);

            assertEquals(List.of("line C"), lastLine);

            assertCannotRead(reader);
        }
    }

    @Test
    void readsAllFiles() throws Exception {
        createFile("test_file1", List.of("line A", "line B", "line C"));
        createFile("test_file2", List.of("line A1", "line B1", "line C1"));

        try (ILogReader reader = getReader()) {
            List<String> lines = readAllLines(reader);

            assertEquals(List.of("line A", "line B", "line C", "line A1", "line B1"), lines);
            assertCanRead(reader);
            List<String> lastLine = readAllLines(reader);

            assertEquals(List.of("line C1"), lastLine);

            assertCannotRead(reader);
        }
    }

    @Test
    void readsAllWithDelayFiles() throws Exception {
        createFile("test_file1", List.of("line A", "line B", "line C"));
        Thread.sleep(100);
        createFile("test_file2", List.of("line A1", "line B1", "line C1"));

        try (ILogReader reader = getReader()) {
            List<String> lines = readAllLines(reader);

            assertEquals(List.of("line A", "line B", "line C", "line A1", "line B1"), lines);
            assertCanRead(reader);
            List<String> lastLine = readAllLines(reader);

            assertEquals(List.of("line C1"), lastLine);

            assertCannotRead(reader);
        }
    }

    @Test
    void readsFromRecentlyCreatedFile() throws Exception {
        createFile("test_file1", List.of("line A", "line B", "line C"));

        try (ILogReader reader = getReader()) {
            List<String> lines = readAllLines(reader);

            assertEquals(List.of("line A", "line B"), lines);

            createFile("test_file2", List.of("line A1", "line B1", "line C1"));
            assertCanRead(reader);

            assertEquals(List.of("line C", "line A1", "line B1"), readAllLines(reader));

            assertCanRead(reader);

            assertEquals(List.of("line C1"), readAllLines(reader));

            assertCannotRead(reader);
        }
    }

    @Test
    void readsFromRecentlyCreatedAndUpdatedFile() throws Exception {
        createFile("test_file1", List.of("line A", "line B", "line C"));

        try (ILogReader reader = getReader()) {
            List<String> lines = readAllLines(reader);

            assertEquals(List.of("line A", "line B"), lines);

            createFile("test_file2", List.of("line A1", "line B1", "line C1"));
            assertCanRead(reader);
            appendToFile("test_file2", List.of("line D1"));

            assertEquals(List.of("line C", "line A1", "line B1", "line C1"), readAllLines(reader));

            assertCanRead(reader);

            assertEquals(List.of("line D1"), readAllLines(reader));

            assertCannotRead(reader);
        }
    }

    @Test
    void readsFromRecentlyCreatedAndUpdatedFileAndUpdatedCurrentFile() throws Exception {
        createFile("test_file1", List.of("line A", "line B", "line C"));

        try (ILogReader reader = getReader()) {
            List<String> lines = readAllLines(reader);

            assertEquals(List.of("line A", "line B"), lines);

            appendToFile("test_file1", List.of("new line"));
            createFile("test_file2", List.of("line A1", "line B1", "line C1"));
            assertCanRead(reader); // because a new line added to the file 'test_file1'
            appendToFile("test_file2", List.of("line D1"));

            assertEquals(List.of("line C", "new line", "line A1", "line B1", "line C1"), readAllLines(reader));

            assertCanRead(reader);

            assertEquals(List.of("line D1"), readAllLines(reader));

            assertCannotRead(reader);
        }
    }

    @Test
    @DisplayName("reads normal in case the current file's last line is modified and new file created after that")
    void readsFromRecentlyCreatedAndUpdatedFileAndUpdatedLastCurrentFile() throws Exception {
        createFileWithoutNewLineAtTheEnd("test_file1", List.of("line A", "line B", "line C"));

        try (ILogReader reader = getReader()) {
            List<String> lines = readAllLines(reader);

            assertEquals(List.of("line A", "line B"), lines);

            appendToFile("test_file1", List.of(" and new line"));
            createFile("test_file2", List.of("line A1", "line B1", "line C1"));
            assertCannotRead(reader); // because the last line were modified in 'test_file1'
            appendToFile("test_file2", List.of("line D1"));

            assertCanRead(reader);
            assertEquals(List.of("line C and new line", "line A1", "line B1", "line C1"), readAllLines(reader));

            assertCanRead(reader);

            assertEquals(List.of("line D1"), readAllLines(reader));

            assertCannotRead(reader);
        }
    }

    @Test
    void readsFileWithChangingLastLine() throws Exception {
        String fileName = "test_file1";
        createFileWithoutNewLineAtTheEnd(fileName, List.of("line A", "line B", "line C"));

        try (ILogReader reader = getReader()) {
            List<String> lines = readAllLines(reader);

            assertEquals(List.of("line A", "line B"), lines);
            appendToFileWithoutNewLineAtTheEnd(fileName, List.of(" with"));
            assertCannotRead(reader);

            appendToFile(fileName, List.of(" appended info"));
            assertCannotRead(reader);

            // second refresh should consider line finished
            assertCanRead(reader);
            List<String> lastLine = readAllLines(reader);

            assertEquals(List.of("line C with appended info"), lastLine);

            assertCannotRead(reader);
        }
    }

    @Test
    void readsFileWithNewLines() throws Exception {
        String fileName = "test_file1";
        createFileWithoutNewLineAtTheEnd(fileName, List.of("line A", "line B", "line C"));

        try (ILogReader reader = getReader()) {
            List<String> lines = readAllLines(reader);

            assertEquals(List.of("line A", "line B"), lines);
            appendToFile(fileName, List.of("1"));
            assertCannotRead(reader);

            appendToFile(fileName, List.of("new line"));
            assertCanRead(reader);

            assertEquals(List.of("line C1"), readAllLines(reader));

            // second refresh should consider line finished
            assertCanRead(reader);
            List<String> lastLine = readAllLines(reader);

            assertEquals(List.of("new line"), lastLine);

            assertCannotRead(reader);
        }
    }

    @Test
    void readsWhenModifiedTwiceInARow() throws Exception {
        String fileName = "test_file1";
        createFileWithoutNewLineAtTheEnd(fileName, List.of("line A", "line B", "line C"));

        try (ILogReader reader = getReader()) {
            List<String> lines = readAllLines(reader);

            assertEquals(List.of("line A", "line B"), lines);
            appendToFile(fileName, List.of("1"));
            assertCannotRead(reader);

            appendToFileWithoutNewLineAtTheEnd(fileName, List.of("new line"));
            assertCanRead(reader);

            assertEquals(List.of("line C1"), readAllLines(reader));

            appendToFileWithoutNewLineAtTheEnd(fileName, List.of(" and last line"));

            assertCannotRead(reader);

            assertCanRead(reader);

            assertEquals(List.of("new line and last line"), readAllLines(reader));
            assertCannotRead(reader);
        }
    }

    @Test
    void lineWithoutLFAtTheEnd() throws Exception {
        String fileName = "test_file1";
        createFileWithoutNewLineAtTheEnd(fileName, List.of("line A", "line B", "line C"));

        try (ILogReader reader = getReader()) {
            List<String> lines = readAllLines(reader);

            assertEquals(List.of("line A", "line B"), lines);
            appendToFile(fileName, List.of("" /*emulate LF*/, "new line", "new line 2", "last line"));
            assertCanRead(reader); // the new file exists in file

            assertEquals(List.of("line C", "new line", "new line 2"), readAllLines(reader));

            assertCanRead(reader);

            assertEquals(List.of("last line"), readAllLines(reader));

            assertCannotRead(reader);
        }
    }

    @Test
    void newFileIsAdded() throws Exception {
        String fileName = "test_file1";
        createFileWithoutNewLineAtTheEnd(fileName, List.of("line A", "line B", "line C"));

        try (ILogReader reader = getReader()) {
            List<String> lines = readAllLines(reader);

            assertEquals(List.of("line A", "line B"), lines);
            createFile("test_file2", List.of("test", "test1"));
            assertCanRead(reader); // the new file exists in files

            assertEquals(List.of("line C", "test"), readAllLines(reader));

            assertCanRead(reader);

            assertEquals(List.of("test1"), readAllLines(reader));

            assertCannotRead(reader);
        }
    }

    @Test
    void fileReadTillTheEndAndAddOneLine() throws Exception {
        String fileName = "test_file1";
        createFileWithoutNewLineAtTheEnd(fileName, List.of("line A", "line B", "line C"));

        try (ILogReader reader = getReader()) {
            List<String> lines = readAllLines(reader);

            assertEquals(List.of("line A", "line B"), lines);
            assertCanRead(reader); // the last line is not modified

            assertEquals(List.of("line C"), readAllLines(reader));

            assertCannotRead(reader);

            appendToFile(fileName, List.of("" /*to get LN*/, "line D"));
            assertCannotRead(reader); // the last line need way another check
            assertCanRead(reader);

            assertEquals(List.of("line D"), readAllLines(reader));
            assertCannotRead(reader);
        }
    }

    @Test
    void fileReadTillTheEndAndAddMoreThanOneLine() throws Exception {
        String fileName = "test_file1";
        createFileWithoutNewLineAtTheEnd(fileName, List.of("line A", "line B", "line C"));

        try (ILogReader reader = getReader()) {
            List<String> lines = readAllLines(reader);

            assertEquals(List.of("line A", "line B"), lines);
            assertCanRead(reader); // the last line is not modified

            assertEquals(List.of("line C"), readAllLines(reader));

            assertCannotRead(reader);

            appendToFile(fileName, List.of("" /*to get LN*/, "line D", "line E"));
            assertCanRead(reader); // more than one line added

            assertEquals(List.of("line D"), readAllLines(reader));

            assertCanRead(reader); // last line is not modified

            assertEquals(List.of("line E"), readAllLines(reader));
            assertCannotRead(reader);
        }
    }

    private void assertCannotRead(ILogReader reader) throws IOException {
        assertFalse(reader.refresh());
        assertNull(reader.getNextLine());
    }

    private void assertCanRead(ILogReader reader) throws IOException {
        assertTrue(reader.refresh());
    }

    private List<String> readAllLines(ILogReader reader) throws IOException {
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.getNextLine()) != null) {
            lines.add(line);
        }
        return lines;
    }

    private DirectoryWatchLogReader getReader() throws FileNotFoundException {
        return new DirectoryWatchLogReader(logDirectory, ".*");
    }

    private void createFile(String name, Collection<String> content) throws Exception {
        writeToFile(name, false, content, true);
    }

    private void createFileWithoutNewLineAtTheEnd(String name, Collection<String> content) throws Exception {
        writeToFile(name, false, content, false);
    }

    private void appendToFile(String name, Collection<String> content) throws Exception {
        writeToFile(name, true, content, true);
    }

    private void appendToFileWithoutNewLineAtTheEnd(String name, Collection<String> content) throws Exception {
        writeToFile(name, true, content, false);
    }

    private void writeToFile(String name, boolean append, Collection<String> content, boolean addNewLineToTheEnd) throws Exception {
        try (BufferedWriter out = new BufferedWriter(new FileWriter(new File(logDirectory, name), append))) {
            Iterator<String> iterator = content.iterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                out.write(line);
                if (iterator.hasNext() || addNewLineToTheEnd) {
                    out.newLine();
                }
            }
            out.flush();
        }
        try(InputStream in = new FileInputStream(new File(logDirectory, name))) {
            LOGGER.info("Content: {}", new String(in.readAllBytes())); // touch file
        }
    }
}