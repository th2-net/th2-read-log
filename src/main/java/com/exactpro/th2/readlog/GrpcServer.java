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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class GrpcServer {
    private Logger logger = LoggerFactory.getLogger(getClass().getName());
    //private final Configuration configuration;
    private BindableService bindableService;
    private volatile Server server;
	private final String dataReaderGrpcEndpoint = "0.0.0.0:8080";
	public GrpcServer(BindableService bindableService) {
		this.bindableService = bindableService;
        //String grpcPort = env.get("GRPC_PORT");
	}

	public void start(int port) throws IOException {
		if (server == null) {
			server = ServerBuilder.forPort(port)
					.addService(bindableService)
					.build()
					.start();
			logger.info("GRPC Server started, listening on port '{}'", port);
		} else {
			throw new IllegalStateException("gRPC server already started");
		}
	}

	public void stop() throws InterruptedException {
		if (server == null) {
			throw new IllegalStateException("GRPC server isn't started");
		}

		try {
			logger.info("GRPC server shutdown started");
			server.shutdownNow();
		} finally {
			logger.info("GRPC server shutdown stoped");
		}
	}
}
