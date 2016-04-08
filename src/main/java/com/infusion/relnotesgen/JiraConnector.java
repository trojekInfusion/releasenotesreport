package com.infusion.relnotesgen;

import com.atlassian.jira.rest.client.domain.Issue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public interface JiraConnector {
    // TODO Log any inconsistencies (no jira issue for a key) or even throw an Exception
    ImmutableMap<String, Issue> getIssuesIncludeParents(ImmutableSet<String> issueIds);

    String getIssueUrl(Issue issue);
}
