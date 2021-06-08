/*
 * Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.readlog.impl;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Pattern;

import com.exactpro.th2.readlog.cfg.SortingConfiguration;

public class AbstractRegexpComparator implements Comparator<Path> {
    private final Pattern pattern;

    public AbstractRegexpComparator(SortingConfiguration configuration) {
        pattern = Pattern.compile(configuration.getRegexp());
    }

    @Override
    public int compare(Path o1, Path o2) {
        String firstName = o1.getFileName().toString();
        String secondName = o2.getFileName().toString();
        boolean firstFind;
        boolean secondFind;
        do {
            firstFind = pattern.matcher(firstName).find();
            secondFind = pattern.matcher(secondName).find();


        } while (firstFind && secondFind);
        return firstFind ^ secondFind ? (firstFind ? 1 : -1) : 0;
    }
}
