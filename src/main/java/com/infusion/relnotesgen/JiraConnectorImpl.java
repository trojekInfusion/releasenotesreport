package com.infusion.relnotesgen;

import com.atlassian.httpclient.api.HttpClient;
import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.net.URI;
import java.text.MessageFormat;
import java.util.List;

public class JiraConnectorImpl implements JiraConnector {
    private final JiraRestClient jiraRestClient;

    public JiraConnectorImpl(final Configuration configuration) {
        JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
        AuthenticationHandler authenticationHandler = new BasicHttpAuthenticationHandler(
                configuration.getJiraUsername(),
                configuration.getJiraPassword());

        jiraRestClient = factory.create(
                URI.create(configuration.getJiraUrl()),
                authenticationHandler);
    }

    @Override
    public ImmutableMap<String, Issue> getIssuesIncludeParents(final ImmutableSet<String> issueIds) {
        SearchRestClient searchClient = jiraRestClient.getSearchClient();

        String searchQuery = getSearchJQL(issueIds);
        SearchResult searchResult = searchClient.searchJql(searchQuery).claim();
        List<Issue> issues = Lists.newArrayList(searchResult.getIssues());

        ImmutableSet<String> parentKeysToFetch = FluentIterable
                .from(issues)
                .filter(new Predicate<Issue>() {
                    @Override
                    public boolean apply(Issue issue) {
                        return issue.getIssueType().isSubtask();
                    }
                })
                .transform(new Function<Issue, String>() {
                    @Override
                    public String apply(Issue issue) {
                        try {
                            return ((JSONObject) issue.getFieldByName("Parent").getValue()).get("key").toString();
                        } catch (JSONException e) {
                            throw new RuntimeException("JSON response from JIRA malformed - no parent key in subtask", e);
                        }
                    }
                })
                .filter(new Predicate<String>() {
                    @Override
                    public boolean apply(String parentKey) {
                        return !issueIds.contains(parentKey);
                    }
                })
                .toSet();

        if (!parentKeysToFetch.isEmpty()) {
            String parentSearchQuery = getSearchJQL(parentKeysToFetch);
            SearchResult parentSearchResult = searchClient.searchJql(parentSearchQuery).claim();
            Iterables.addAll(issues, parentSearchResult.getIssues());
        }

        return Maps.uniqueIndex(issues, new Function<Issue, String>() {
            @Override
            public String apply(Issue issue) {
                return issue.getKey();
            }
        });
    }

    private static String getSearchJQL(Iterable<String> issueIds) {
        return MessageFormat.format("key in ({0})", Joiner.on(",").join(issueIds));
    }
}
