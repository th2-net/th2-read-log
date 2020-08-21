package com.exactpro.th2.logreader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegexLogParser {
	private Logger logger = LoggerFactory.getLogger(LogReader.class);
	private String regex;
	private Pattern pattern;
	
	public RegexLogParser() {
		this.regex = System.getenv("REGEX");
		pattern = Pattern.compile(regex);		
	}
	
	String parse (String raw) {
		String result = "";
		
		Matcher matcher = pattern.matcher(raw);
		StringBuilder sb = new StringBuilder();
		
		while (matcher.find()) {
			for (int i = 0; i < matcher.groupCount(); ++i) {
				sb.append(matcher.group(i));
			}
		}
		
		result = sb.toString();
		
		return result;	
	}
}
