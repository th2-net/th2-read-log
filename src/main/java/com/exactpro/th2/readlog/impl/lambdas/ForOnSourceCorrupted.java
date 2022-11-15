package com.exactpro.th2.readlog.impl.lambdas;

import com.exactpro.th2.read.file.common.StreamId;
import kotlin.Unit;

import java.nio.file.Path;

public interface ForOnSourceCorrupted {
    Unit action(StreamId id, Path path, Exception ex);
}
