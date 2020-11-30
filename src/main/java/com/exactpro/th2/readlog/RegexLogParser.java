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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegexLogParser {
	private static final Logger logger = LoggerFactory.getLogger(RegexLogParser.class);
    private final Pattern pattern;
	private final List<Integer> regexGroups;

	public RegexLogParser(String regex, List<Integer> regexGroups) {

		this.regexGroups = regexGroups == null ? Collections.emptyList() : regexGroups;
		pattern = Pattern.compile(regex);

		logger.info("Regex expression '{}'", regex);
		logger.info("Regex groups to output '{}'", regexGroups);
	}

	List<String> parse (String raw) {
		List<String> result = new ArrayList<>();

		Matcher matcher = pattern.matcher(raw);

		if (regexGroups.isEmpty()) {
			while (matcher.find()) {
				for (int i = 0; i <= matcher.groupCount(); ++i) {
					String res = matcher.group(i); 
					result.add(res);
					logger.trace("ParsedLogLine: {}",res);
				}
			}
		} else {
			while (matcher.find()) {
				for (int index : regexGroups) {
					String res = matcher.group(index);
					result.add(res);
					logger.trace("ParsedLogLine: {}",res);
				}
			}

		}

		return result;
	}
}
