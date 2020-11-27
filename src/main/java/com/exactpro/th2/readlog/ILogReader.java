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
