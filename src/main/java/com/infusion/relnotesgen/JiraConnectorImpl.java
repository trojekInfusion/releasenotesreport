package com.infusion.relnotesgen;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class JiraConnectorImpl implements JiraConnector {
    private final static Logger logger = LoggerFactory.getLogger(Configuration.LOGGER_NAME);

    private final Configuration configuration;

    public JiraConnectorImpl(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
	public ImmutableMap<String, Issue> getIssuesIncludeParents(final ImmutableSet<String> issueIds) {
        try(JiraRestClient jiraRestClient = createJiraRestClient()) {
            return getIssuesIncludeParentsInternal(jiraRestClient, issueIds);
        } catch (IOException e) {
            throw new RuntimeException("Exception while contacting JIRA", e);
        }
    }

	@Override
	public ImmutableMap<String, Issue> getIssuesByFixVersions(ImmutableSet<String> fixVersions) {
        try(JiraRestClient jiraRestClient = createJiraRestClient()) {
            return getIssuesByFixVersionsInternal(jiraRestClient, fixVersions);
        } catch (IOException e) {
            throw new RuntimeException("Exception while contacting JIRA", e);
        }
	}

	@Override
	public ImmutableMap<String, Issue> getKnownIssuesByJql(String jqlQuery) {
        try(JiraRestClient jiraRestClient = createJiraRestClient()) {
            return getKnownIssuesByJqlInternal(jiraRestClient, jqlQuery);
        } catch (IOException e) {
            throw new RuntimeException("Exception while contacting JIRA", e);
        }
	}

    private ImmutableMap<String, Issue> getIssuesIncludeParentsInternal(
            final JiraRestClient jiraRestClient,
            final ImmutableSet<String> issueIds) {
        SearchRestClient searchClient = jiraRestClient.getSearchClient();

        String searchQuery = getSearchJQL(issueIds);

        logger.info("Getting issues for keys: {}", issueIds);
        List<Issue> issues = retrieveIssuesFromJiraClient(searchQuery, jiraRestClient);

        addParentIssues(issueIds, searchClient, issues);
        logger.info("Fetching issues from JIRA completed. {} issues fetched.", issues.size());

        return convertIssueListToMap(issues);
    }

	private ImmutableMap<String, Issue> getIssuesByFixVersionsInternal(JiraRestClient jiraRestClient,
			ImmutableSet<String> fixVersions) {
        logger.info("Getting issues for fixVersions: {}", fixVersions);        
        String searchQuery = getFixVersionSearchJQL(fixVersions);
		return convertIssueListToMap(retrieveIssuesFromJiraClient(searchQuery, jiraRestClient));
	}

	private ImmutableMap<String, Issue> getKnownIssuesByJqlInternal(JiraRestClient jiraRestClient, String jqlQuery) {
        logger.info("Getting known issues for query: {}", jqlQuery);
		return convertIssueListToMap(retrieveIssuesFromJiraClient(jqlQuery, jiraRestClient));
	}

    private static String getSearchJQL(Iterable<String> issueIds) {
        return MessageFormat.format("key in ({0})", Joiner.on(",").join(issueIds));
    }

	private String getFixVersionSearchJQL(ImmutableSet<String> fixVersions) {
        if (fixVersions==null || fixVersions.isEmpty()) {
        	return null;
        }
        return MessageFormat.format("fixVersion in (\"{0}\")", Joiner.on("\",\"").join(fixVersions));
	}

	private JiraRestClient createJiraRestClient() {
        JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();

        AuthenticationHandler authenticationHandler = new BasicHttpAuthenticationHandler(
                configuration.getJiraUsername(),
                configuration.getJiraPassword());

        return factory.create(
                URI.create(configuration.getJiraUrl()),
                authenticationHandler);
    }

	private List<Issue> retrieveIssuesFromJiraClient(String searchQuery, JiraRestClient jiraRestClient) {
        SearchRestClient searchClient = jiraRestClient.getSearchClient();
		List<Issue> issues;
		if (searchQuery==null || searchQuery.isEmpty()) {
			issues = new ArrayList<Issue>();
		} else {
			SearchResult searchResult = searchClient.searchJql(searchQuery, 500, 0, null).claim();
			issues = Lists.newArrayList(searchResult.getIssues());			
			if(issues.size() < searchResult.getTotal()) {
			    logger.warn("Reached limit of max results {} - not all JIRA items returned - {} out of {}!!", searchResult.getMaxResults(), issues.size(), searchResult.getTotal());
			}
		}
        logger.info("Fetching issues from JIRA completed. {} issues fetched.", issues.size());
		return issues;
	}

	private ImmutableMap<String, Issue> convertIssueListToMap(List<Issue> issues) {
		return Maps.uniqueIndex(issues, new Function<Issue, String>() {
            @Override
            public String apply(Issue issue) {
                return issue.getKey();
            }
        });
	}

	private void addParentIssues(final ImmutableSet<String> issueIds, SearchRestClient searchClient,
			List<Issue> issues) {
		ImmutableSet<String> parentKeysToFetch = retrieveParentKeysToFetch(issueIds, issues);

        if (!parentKeysToFetch.isEmpty()) {
            logger.info("Fetching subtasks' parents which haven't been fetched already: {}", parentKeysToFetch);

            String parentSearchQuery = getSearchJQL(parentKeysToFetch);
            SearchResult parentSearchResult = searchClient.searchJql(parentSearchQuery).claim();
            Iterables.addAll(issues, parentSearchResult.getIssues());
        }
	}

	private ImmutableSet<String> retrieveParentKeysToFetch(final ImmutableSet<String> issueIds, List<Issue> issues) {
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
                // Eliminate duplicates that have already been fetched
                .filter(new Predicate<String>() {
                    @Override
                    public boolean apply(String parentKey) {
                        return !issueIds.contains(parentKey);
                    }
                })
                .toSet();
		return parentKeysToFetch;
	}

}
