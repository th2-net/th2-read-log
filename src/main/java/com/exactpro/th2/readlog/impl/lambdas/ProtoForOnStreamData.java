/*
 * Copyright 2022-2023 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.readlog.impl.lambdas;

import com.exactpro.th2.common.grpc.RawMessage;
import com.exactpro.th2.read.file.common.StreamId;
import kotlin.Unit;

import java.util.List;

public interface ProtoForOnStreamData {
    Unit action(StreamId id, List<? extends RawMessage.Builder> builder);
}