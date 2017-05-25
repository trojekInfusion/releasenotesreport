package com.infusion.relnotesgen;

import com.google.common.collect.ImmutableSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommitMessageParserImpl implements CommitMessageParser {
    private final Pattern jiraKeyPattern;
    private final Pattern defectIdPattern;
    private final Pattern prPattern = Pattern.compile("Merge pull request #\\d+");

    public CommitMessageParserImpl(final Configuration configuration) {
        jiraKeyPattern = Pattern.compile(configuration.getJiraIssuePattern());
        defectIdPattern = Pattern.compile(configuration.getDefectPattern(), Pattern.CASE_INSENSITIVE);
    }

    @Override
    public ImmutableSet<String> getJiraKeys(final String text) {
        return matchAll(jiraKeyPattern, text);
    }

    @Override
    public ImmutableSet<String> getDefectIds(final String text) {
        return matchAll(defectIdPattern, text);
    }

    @Override
    public String getPullRequestId(String text) {

        ImmutableSet<String> matches = matchAll(prPattern, text);
        if(matches.size() == 1) {
            // one match for text, second for ID
            return matches.asList().get(0).replace("Merge pull request #","");
        }

        return null;
    }

    private ImmutableSet<String> matchAll(final Pattern pattern, final String text) {

        if(!text.startsWith("Merge pull request"))
            return ImmutableSet.of();

        ImmutableSet.Builder<String> matchesBuilder = ImmutableSet.builder();
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            matchesBuilder.add(matcher.group());
        }

        return matchesBuilder.build();
    }
}
