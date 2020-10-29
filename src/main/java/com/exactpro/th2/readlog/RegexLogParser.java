package com.exactpro.th2.readlog;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.logstash.logback.argument.StructuredArguments;

public class RegexLogParser {
	private Logger logger = LoggerFactory.getLogger(LogReader.class);
	private String regex;
	private Pattern pattern;
	private ArrayList<Integer> regexGroups;
	
	public RegexLogParser() {
		
		regexGroups = new ArrayList<Integer>();
		
		this.regex = System.getenv("REGEX");		
		String regexGroupStr = System.getenv("REGEX_GROUP");
		
		if (!regexGroupStr.isEmpty()) {
			for (String str :regexGroupStr.split(",")) {			
				regexGroups.add(Integer.valueOf(str));
			}			
		}
						
		pattern = Pattern.compile(regex);		
		
		logger.info("Regex expression '{}'", regex);	
		logger.info("Regex groups to output '{}'", regexGroupStr);		
	}
	
	ArrayList<String> parse (String raw) {
		ArrayList<String> result = new ArrayList<String>();
		
		Matcher matcher = pattern.matcher(raw);
		
		if (regexGroups.size() == 0) {
			while (matcher.find()) {
				for (int i = 0; i <= matcher.groupCount(); ++i) {
					String res = matcher.group(i); 
					result.add(res);
					logger.trace("ParsedLogLine",StructuredArguments.value("ParsedLogLine", res));
				}
			}
		} else {
			while (matcher.find()) {
				for (int index : regexGroups) {
					String res = matcher.group(index);
					result.add(res);
					logger.trace("ParsedLogLine",StructuredArguments.value("ParsedLogLine", res));
				}
			}
			
		}
				
		return result;	
	}
}
