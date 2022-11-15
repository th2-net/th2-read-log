package com.exactpro.th2.readlog.impl.lambdas;

import com.exactpro.th2.read.file.common.StreamId;
import kotlin.Unit;

public interface ForOnError {
    Unit action(StreamId id, String message, Exception ex);
}
