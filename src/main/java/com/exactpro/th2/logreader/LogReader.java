package com.exactpro.th2.logreader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.logstash.logback.argument.StructuredArguments;

public class LogReader implements AutoCloseable {
	private String fileName;
	private Scanner scanner; 
	private Logger logger = LoggerFactory.getLogger(LogReader.class);
	
	private boolean closeState;
	
	public LogReader() {
		this.fileName = System.getenv("LOG_FILE_NAME");
		
		closeState = false;
		
		try {
			scanner = new Scanner(new File(fileName));
		} catch (FileNotFoundException e) {
			logger.error("{}", e.getMessage(), StructuredArguments.value("stacktrace",e.getStackTrace()), e);			
		}
		
		logger.info("Open log file", StructuredArguments.value("fileName",fileName));
	}

	public boolean hasNextLine() {
		return scanner.hasNextLine();
	}
	
	public String getNextLine() {
		return scanner.nextLine();
	}
	
	public String getFileName() {
		return fileName;
	}
	
	
	public boolean isClosed() {
		return closeState;
	}
	
	@Override
	public void close() {
		if (scanner != null) {
			scanner.close();
			closeState=true;
		}
		logger.info("Close log file", StructuredArguments.value("fileName",fileName));
	}
}
