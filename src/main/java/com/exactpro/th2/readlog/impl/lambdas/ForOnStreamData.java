package com.exactpro.th2.readlog.impl.lambdas;

import com.exactpro.th2.common.grpc.RawMessage;
import com.exactpro.th2.read.file.common.StreamId;
import kotlin.Unit;

import java.util.List;

public interface ForOnStreamData {
    Unit action(StreamId id, List<RawMessage.Builder> builder);
}
