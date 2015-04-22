package com.infusion.relnotesgen;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author trojek
 *
 */
public class JiraIssueIdMatcher {

    private final static Logger logger = LoggerFactory.getLogger(JiraIssueIdMatcher.class);

    private Pattern pattern;

    public JiraIssueIdMatcher(final String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public Set<String> findJiraIds(final Collection<String> texts) {
        logger.info("Searching for jira issue ids with patern '{}'", pattern);

        Set<String> jiraIds = new HashSet<String>();

        for (String text : texts) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                jiraIds.add(matcher.group());
            }
        }


        logger.info("Found {} jira issue's ids", jiraIds.size());

        return jiraIds;
    }
}
