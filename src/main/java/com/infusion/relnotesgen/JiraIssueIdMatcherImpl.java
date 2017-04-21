package com.infusion.relnotesgen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author trojek
 *
 */
public class JiraIssueIdMatcherImpl implements JiraIssueIdMatcher {

    private final static Logger logger = LoggerFactory.getLogger(Configuration.LOGGER_NAME);

    private Pattern pattern;

    public JiraIssueIdMatcherImpl(final String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    @Override
    public ImmutableList<ImmutablePair<String, ImmutableSet<String>>> findJiraIds(final Iterable<String> texts) {
        ImmutableList.Builder<ImmutablePair<String, ImmutableSet<String>>> resultBuilder = ImmutableList.builder();

        for (String text : texts) {
            ImmutableSet.Builder<String> matchesBuilder = ImmutableSet.builder();
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                matchesBuilder.add(matcher.group());
            }

            resultBuilder.add(ImmutablePair.of(text, matchesBuilder.build()));
        }

        return resultBuilder.build();
    }
}
