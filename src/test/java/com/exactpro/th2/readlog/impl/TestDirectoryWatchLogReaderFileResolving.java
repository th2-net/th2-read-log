package com.exactpro.th2.readlog.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import org.junit.jupiter.api.Test;

import com.exactpro.th2.readlog.impl.DirectoryWatchLogReader.FileInfo;

class TestDirectoryWatchLogReaderFileResolving {
    @Test
    void allFilesIfOnlySameFileInTheInput() {
        FileInfo current = createFileInfo("test", Instant.MAX, 10);

        Queue<FileInfo> files = createQueue(createFileInfo("test", Instant.MAX, 10));

        Queue<FileInfo> result = DirectoryWatchLogReader.filterCurrentAndNewer(current, files);
        assertEquals(files, result, () -> "Actual result: " + result);
    }

    @Test
    void allFilesIfOnlyNewerFileInInput() {
        FileInfo current = createFileInfo("test", Instant.EPOCH, 10);

        Queue<FileInfo> files = createQueue(createFileInfo("test1", Instant.EPOCH.plusSeconds(1), 10));

        Queue<FileInfo> result = DirectoryWatchLogReader.filterCurrentAndNewer(current, files);
        assertEquals(files, result, () -> "Actual result: " + result);
    }

    @Test
    void allFilesIfOnlyNewerFilesInInput() {
        FileInfo current = createFileInfo("test", Instant.EPOCH, 10);

        Queue<FileInfo> files = createQueue(
                createFileInfo("test1", Instant.EPOCH.plusSeconds(1), 10),
                createFileInfo("test2", Instant.EPOCH.plusSeconds(1), 10)
        );

        Queue<FileInfo> result = DirectoryWatchLogReader.filterCurrentAndNewer(current, files);
        assertEquals(files, result, () -> "Actual result: " + result);
    }

    @Test
    void allFilesIfIsTheSameWithDiffSize() {
        FileInfo current = createFileInfo("test", Instant.EPOCH, 10);

        Queue<FileInfo> files = createQueue(
                createFileInfo("test", Instant.EPOCH, 20),
                createFileInfo("test1", Instant.EPOCH, 20)
        );

        Queue<FileInfo> result = DirectoryWatchLogReader.filterCurrentAndNewer(current, files);
        assertEquals(files, result, () -> "Actual result: " + result);
    }

    @Test
    void onlyLastWithSameTimeAndAllNewer() {
        FileInfo current = createFileInfo("test1", Instant.EPOCH, 10);

        Queue<FileInfo> files = createQueue(
                createFileInfo("test0", Instant.EPOCH, 10),
                createFileInfo("test1", Instant.EPOCH, 10),
                createFileInfo("test2", Instant.EPOCH.plusSeconds(1), 10),
                createFileInfo("test3", Instant.EPOCH.plusSeconds(1), 10)
        );

        Queue<FileInfo> result = DirectoryWatchLogReader.filterCurrentAndNewer(current, files);

        Queue<FileInfo> expectedResult = createQueue(
                createFileInfo("test1", Instant.EPOCH, 10),
                createFileInfo("test2", Instant.EPOCH.plusSeconds(1), 10),
                createFileInfo("test3", Instant.EPOCH.plusSeconds(1), 10)
        );
        assertEquals(expectedResult, result, () -> "Actual result: " + result);
    }

    @Test
    void currentModifiedAndAllAfter() {
        FileInfo current = createFileInfo("test1", Instant.EPOCH, 10);

        Queue<FileInfo> files = createQueue(
                createFileInfo("test0", Instant.EPOCH, 10),
                createFileInfo("test1", Instant.EPOCH, 20),
                createFileInfo("test2", Instant.EPOCH, 10),
                createFileInfo("test3", Instant.EPOCH, 10)
        );

        Queue<FileInfo> result = DirectoryWatchLogReader.filterCurrentAndNewer(current, files);

        Queue<FileInfo> expectedResult = createQueue(
                createFileInfo("test1", Instant.EPOCH, 20),
                createFileInfo("test2", Instant.EPOCH, 10),
                createFileInfo("test3", Instant.EPOCH, 10)
        );
        assertEquals(expectedResult, result, () -> "Actual result: " + result);
    }

    @Test
    void sameFileAndAllAfter() {
        FileInfo current = createFileInfo("test1", Instant.EPOCH, 10);

        Queue<FileInfo> files = createQueue(
                createFileInfo("test0", Instant.EPOCH, 10),
                createFileInfo("test1", Instant.EPOCH, 10),
                createFileInfo("test2", Instant.EPOCH, 10),
                createFileInfo("test3", Instant.EPOCH, 10)
        );

        Queue<FileInfo> result = DirectoryWatchLogReader.filterCurrentAndNewer(current, files);

        Queue<FileInfo> expectedResult = createQueue(
                createFileInfo("test1", Instant.EPOCH, 10),
                createFileInfo("test2", Instant.EPOCH, 10),
                createFileInfo("test3", Instant.EPOCH, 10)
        );
        assertEquals(expectedResult, result, () -> "Actual result: " + result);
    }

    @Test
    void allLastWithSameTimeAndNewerFiles() {
        FileInfo current = createFileInfo("test", Instant.EPOCH, 10);

        Queue<FileInfo> files = createQueue(
                createFileInfo("test0", Instant.EPOCH, 10),
                createFileInfo("test1", Instant.EPOCH, 10),
                createFileInfo("test2", Instant.EPOCH.plusSeconds(1), 10),
                createFileInfo("test3", Instant.EPOCH.plusSeconds(1), 10)
        );

        Queue<FileInfo> result = DirectoryWatchLogReader.filterCurrentAndNewer(current, files);

        Queue<FileInfo> expectedResult = createQueue(
                createFileInfo("test1", Instant.EPOCH, 10),
                createFileInfo("test2", Instant.EPOCH.plusSeconds(1), 10),
                createFileInfo("test3", Instant.EPOCH.plusSeconds(1), 10)
        );
        assertEquals(expectedResult, result, () -> "Actual result: " + result);
    }

    private FileInfo createFileInfo(String path, Instant modifiedTime, int size) {
        return new FileInfo(Path.of(path), modifiedTime, size);
    }

    @SafeVarargs
    private static <T> Queue<T> createQueue(T... elements) {
        return new LinkedList<>(Arrays.asList(elements));
    }
}