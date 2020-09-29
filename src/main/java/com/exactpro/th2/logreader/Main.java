package com.exactpro.th2.logreader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Object  {
	
	private final static Logger logger = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) {

	    Properties props = new Properties();
	    
	    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	    
	    try (InputStream configStream = classLoader.getResourceAsStream("logback.xml"))  {
	        props.load(configStream);
	    } catch (IOException e) {
	        System.out.println("Error: can't laod log configuration file ");
	    }    			
            	     
	    RabbitMqClient client = new RabbitMqClient();
        
		try {
			client.connect();
		} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException
				| TimeoutException e) {
			logger.error("{}", e);
			System.exit(-1);
		}
        
		LogReader reader = new LogReader();
		
		client.setSessionAlias(reader.getFileName());
		
		RegexLogParser logParser = new RegexLogParser();
		
		try {
			while (true) {
				String line = reader.getNextLine();
				
				if (line != null) {										
					ArrayList<String> parsedLines = logParser.parse(line);
					for (String parsedLine: parsedLines) {
						client.publish(parsedLine);
					}								
				} else {
					long linesCount = reader.getLinesCount();
					
					Thread.sleep(5000);
					
					reader.close();
					reader.open();
					reader.skip(linesCount);
				}
			}
		
		} catch (IOException| InterruptedException e) {
			logger.error("{}", e);
		}
		
		try {
			reader.close();
			client.close();
		} catch (IOException | TimeoutException e) {
			logger.error("{}", e);
		}
	}
}
