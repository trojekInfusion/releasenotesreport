package com.infusion.relnotesgen;

import java.util.Map;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.infusion.relnotesgen.util.JiraIssueSearchType;

public interface JiraConnector {
    // TODO Log any inconsistencies (no jira issue for a key) or even throw an Exception
    ImmutableMap<String, Issue> getIssuesIncludeParents(final ImmutableSet<String> issueIds, final Map<JiraIssueSearchType, String> errors);
    ImmutableMap<String, Issue> getIssuesByFixVersions(final ImmutableSet<String> fixVersions, final Map<JiraIssueSearchType, String> errors);
	ImmutableMap<String, Issue> getKnownIssuesByJql(final String jqlQuery, final Map<JiraIssueSearchType, String> errors);
}
