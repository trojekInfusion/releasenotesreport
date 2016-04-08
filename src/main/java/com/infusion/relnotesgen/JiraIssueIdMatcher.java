package com.infusion.relnotesgen;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.annotation.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author trojek
 *
 */
public class JiraIssueIdMatcher {

    private final static Logger logger = LoggerFactory.getLogger(Configuration.LOGGER_NAME);

    private Pattern pattern;

    public JiraIssueIdMatcher(final String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public ImmutableList<ImmutablePair<String, ImmutableList<String>>> findJiraIds(final Iterable<String> texts) {
        ImmutableList.Builder<ImmutablePair<String, ImmutableList<String>>> resultBuilder = ImmutableList.builder();

        for (String text : texts) {
            ImmutableList.Builder<String> matchesBuilder = ImmutableList.builder();
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                matchesBuilder.add(matcher.group());
            }

            resultBuilder.add(ImmutablePair.of(text, matchesBuilder.build()));
        }

        return resultBuilder.build();
    }
}
