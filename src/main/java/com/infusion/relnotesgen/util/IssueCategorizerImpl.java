package com.infusion.relnotesgen.util;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.infusion.relnotesgen.Configuration;

import java.util.*;

public class IssueCategorizerImpl implements IssueCategorizer {
    private final Configuration configuration;

    public IssueCategorizerImpl(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Map<String, List<Issue>> byType(final Collection<Issue> issues) {
        TreeMap<String, List<Issue>> issuesByType = new TreeMap<>(new PredefinedDictionaryComparator(configuration.getIssueSortType()));
        for (Issue issue : issues) {
            String issueType = issue.getIssueType().getName();
            List<Issue> typedIssues = issuesByType.get(issueType);
            if (typedIssues == null) {
                typedIssues = new ArrayList<>();
                issuesByType.put(issueType, typedIssues);
            }
            typedIssues.add(issue);
        }

        IssuePriorityComparator priorityComparator = new IssuePriorityComparator(configuration.getIssueSortPriority());
        for(List<Issue> issuesByTypeList : issuesByType.values()) {
            Collections.sort(issuesByTypeList, priorityComparator);
        }
        return issuesByType;
    }

}
