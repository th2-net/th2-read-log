package com.exactpro.th2.logreader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.logstash.logback.argument.StructuredArguments;

public class Main extends Object  {
	
	private final static Logger logger = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) {
		
		try {		
   			
		    Properties props = new Properties();
		    
		    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		    
		    try (InputStream configStream = classLoader.getResourceAsStream("logback.xml"))  {
		        props.load(configStream);
		    } catch (IOException e) {
		        System.out.println("Errornot laod configuration file ");
		    }    			
	            	     
		    RabbitMqClient client = new RabbitMqClient();
	        
	        client.connect();
	        
			LogReader reader = new LogReader();
			
			client.setSessionAlias(reader.getFileName());
			
			RegexLogParser logParser = new RegexLogParser();
			
			while (reader.hasNextLine()) {
				String line = reader.getNextLine();
				String parsedLine = logParser.parse(line);
				logger.trace("RawLogLine",StructuredArguments.value("RawLogLine",line));
				logger.trace("ParsedLogLine",StructuredArguments.value("ParsedLogLine",line));
				client.publish(parsedLine);
			}
			
			reader.close();
			
			client.close();
		} catch (Exception e) {		
			logger.error("{}", e);
		}
	}
}
