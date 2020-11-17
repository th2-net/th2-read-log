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

package com.exactpro.th2.logreader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.th2.infra.grpc.ConnectionID;
import com.exactpro.th2.infra.grpc.Direction;
import com.exactpro.th2.infra.grpc.MessageID;
import com.exactpro.th2.infra.grpc.RawMessage;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import net.logstash.logback.argument.StructuredArguments;
import com.exactpro.th2.infra.grpc.RawMessageBatch;
import com.exactpro.th2.infra.grpc.RawMessageMetadata;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

public class RabbitMqClient {
	
	private final Logger logger = LoggerFactory.getLogger(RabbitMqClient.class);
	
	private String amqpUri = "";

	private String exchangeName = "demo_exchange";
	private String queueName = "logreader.first.";
	private String queuePostfix = "default";
	private Channel channel = null;
	private Connection conn = null;
	private String sessionAlias = "";
	private long index = 0;
	private List<String> listOfLines = new ArrayList<String>();
	private long size = 0;
	private long lastPublishTs = Clock.systemDefaultZone().instant().getEpochSecond();
	
	public RabbitMqClient() {
        Map<String, String> env = System.getenv();
        
        String host = env.get("RABBITMQ_HOST");
        String port = env.get("RABBITMQ_PORT");
        String vHost = env.get("RABBITMQ_VHOST");
        String user = env.get("RABBITMQ_USER");
        String password = env.get("RABBITMQ_PASS");
        exchangeName = env.get("RABBITMQ_EXCHANGE_NAME_TH2_CONNECTIVITY");
        queuePostfix = env.get("RABBITMQ_QUEUE_POSTFIX");
        
        amqpUri = "amqp://" + user + ":" + password + "@" + host + ":" + port + "/" + vHost;
	}

	private void publish() throws IOException {
		RawMessageBatch.Builder builder = RawMessageBatch.newBuilder();
		
		for (String str: listOfLines) {
			RawMessage.Builder msgBuilder = builder.addMessagesBuilder();
			
			ByteString body = ByteString.copyFrom(str.getBytes());

			msgBuilder.setBody(body);

			RawMessageMetadata.Builder metaData = RawMessageMetadata.newBuilder();
			
			Timestamp.Builder ts = Timestamp.newBuilder();
			
			Clock clock = Clock.systemDefaultZone();
			Instant instant = clock.instant();
			
			ts.setSeconds(instant.getEpochSecond());
			ts.setNanos(instant.getNano());
				
			metaData.setTimestamp(ts);
			
			MessageID.Builder messageId = MessageID.newBuilder();
			
			ConnectionID.Builder connId = ConnectionID.newBuilder();
			
			connId.setSessionAlias(sessionAlias);
			
			messageId.setConnectionId(connId);
			messageId.setSequence(++index);
			messageId.setDirection(Direction.FIRST);
			
			metaData.setId(messageId);
			
			msgBuilder.setMetadata(metaData);						
		}
		
		listOfLines.clear();
		
		byte[] data = builder.build().toByteArray();
		
		channel.basicPublish(exchangeName, queueName, null, data);
		
		logger.trace("publish",
				StructuredArguments.value("exchangeName",exchangeName),
				StructuredArguments.value("queueName",queueName),
				StructuredArguments.value("data",data)
				);												
	}
	
	public void connect() throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException, IOException, TimeoutException {
		
		logger.info("Connecting to RabbitMQ", 
				StructuredArguments.value("URI",amqpUri)
				);
		
		queueName += queuePostfix;
		
		ConnectionFactory factory = new ConnectionFactory();
		
		factory.setUri(amqpUri);
		
		conn = factory.newConnection();
		
		channel = conn.createChannel();
	
		System.out.println("Done");
	}
	
	public void publish(String line) throws IOException {
		
		size += line.length();
		
		listOfLines.add(line);
		
		if (	(listOfLines.size() >= 100) ||
				(size >= 100000000) ||
				(Clock.systemDefaultZone().instant().getEpochSecond() - lastPublishTs > 2)) {
			
			lastPublishTs = Clock.systemDefaultZone().instant().getEpochSecond();
			size = 0;
			
			publish();
		}		
	}
	
	
	public void setSessionAlias(String alias) {
		sessionAlias = alias;
	}
	
	public void close() throws IOException, TimeoutException {
		
		if (!listOfLines.isEmpty()) {
			publish();
		}
		
		channel.close();
		conn.close();
		
		logger.info("Disconecting from RabbitMQ");
	}
}
