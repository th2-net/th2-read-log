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
