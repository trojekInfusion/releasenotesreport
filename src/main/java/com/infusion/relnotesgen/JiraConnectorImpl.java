package com.infusion.relnotesgen;

import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.api.domain.util.ErrorCollection;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.infusion.relnotesgen.util.JiraIssueSearchType;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JiraConnectorImpl implements JiraConnector {
	private final static Logger logger = LoggerFactory.getLogger(Configuration.LOGGER_NAME);
    private final Configuration configuration;

    public JiraConnectorImpl(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
	public ImmutableMap<String, Issue> getIssuesIncludeParents(final ImmutableSet<String> issueIds, final Map<JiraIssueSearchType, String> errors) {
        try(JiraRestClient jiraRestClient = createJiraRestClient()) {
            return getIssuesIncludeParentsInternal(jiraRestClient, issueIds, errors);
        } catch (IOException e) {
            throw new RuntimeException("Exception while contacting JIRA", e);
        }
    }

	@Override
	public ImmutableMap<String, Issue> getIssuesByFixVersions(final ImmutableSet<String> fixVersions, final Map<JiraIssueSearchType, String> errors) {
        try(JiraRestClient jiraRestClient = createJiraRestClient()) {
            return getIssuesByFixVersionsInternal(jiraRestClient, fixVersions, errors);
        } catch (IOException e) {
            throw new RuntimeException("Exception while contacting JIRA", e);
        }
	}

	@Override
	public ImmutableMap<String, Issue> getKnownIssuesByJql(final String jqlQuery, final Map<JiraIssueSearchType, String> errors) {
        try(JiraRestClient jiraRestClient = createJiraRestClient()) {
            return getKnownIssuesByJqlInternal(jiraRestClient, jqlQuery, errors);
        } catch (IOException e) {
            throw new RuntimeException("Exception while contacting JIRA", e);
        }
	}

    private ImmutableMap<String, Issue> getIssuesIncludeParentsInternal(final JiraRestClient jiraRestClient, 
    		final ImmutableSet<String> issueIds, final Map<JiraIssueSearchType, String> errors) {
        SearchRestClient searchClient = jiraRestClient.getSearchClient();

        String searchQuery = getSearchJQL(issueIds);

        logger.info("Getting issues for keys: {}", issueIds);
        List<Issue> issues = retrieveIssuesFromJiraClient(searchQuery, jiraRestClient, errors, JiraIssueSearchType.GENERIC);

        addParentIssues(issueIds, searchClient, issues);
        logger.info("Fetching issues from JIRA completed. {} issues fetched.", issues.size());

        return convertIssueListToMap(issues);
    }

	private ImmutableMap<String, Issue> getIssuesByFixVersionsInternal(final JiraRestClient jiraRestClient,
			final ImmutableSet<String> fixVersions, final Map<JiraIssueSearchType, String> errors) {
        logger.info("Getting issues for fixVersions: {}", fixVersions);        
        String searchQuery = getFixVersionSearchJQL(fixVersions);
		return convertIssueListToMap(retrieveIssuesFromJiraClient(searchQuery, jiraRestClient, errors, JiraIssueSearchType.FIX_VERSION));
	}

	private ImmutableMap<String, Issue> getKnownIssuesByJqlInternal(final JiraRestClient jiraRestClient, final String jqlQuery, final Map<JiraIssueSearchType, String> errors) {
        logger.info("Getting known issues for query: {}", jqlQuery);
		return convertIssueListToMap(retrieveIssuesFromJiraClient(jqlQuery, jiraRestClient, errors, JiraIssueSearchType.KNOWN_ISSUE));
	}

    private static String getSearchJQL(final Iterable<String> issueIds) {
        return MessageFormat.format("key in ({0})", Joiner.on(",").join(issueIds));
    }

	private String getFixVersionSearchJQL(final ImmutableSet<String> fixVersions) {
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

	private List<Issue> retrieveIssuesFromJiraClient(final String searchQuery, final JiraRestClient jiraRestClient, 
			final Map<JiraIssueSearchType, String> errors, final JiraIssueSearchType searchType) {
        SearchRestClient searchClient = jiraRestClient.getSearchClient();
		List<Issue> issues = new ArrayList<Issue>();
		if (searchQuery!=null && !searchQuery.isEmpty()) {
			try {
				SearchResult searchResult = searchClient.searchJql(searchQuery, 500, 0, null).claim();
				issues = Lists.newArrayList(searchResult.getIssues());			
				if(issues.size() < searchResult.getTotal()) {
				    logger.warn("Reached limit of max results {} - not all JIRA items returned - {} out of {}!!", searchResult.getMaxResults(), issues.size(), searchResult.getTotal());
				}
		        logger.info("Fetching issues from JIRA completed. {} issues fetched.", issues.size());
			} catch (RestClientException e) {
				StringBuilder errorSb = new StringBuilder();
				for (ErrorCollection error : e.getErrorCollections()) {
					for (String errorMessage : error.getErrorMessages()) {
						errorSb.append(errorMessage);
					}
				}
				errors.put(searchType, errorSb.toString());
				logger.warn(e.getMessage());
			}
		}
		return issues;
	}

	private ImmutableMap<String, Issue> convertIssueListToMap(final List<Issue> issues) {
		return Maps.uniqueIndex(issues, new Function<Issue, String>() {
            @Override
            public String apply(Issue issue) {
                return issue.getKey();
            }
        });
	}

	private void addParentIssues(final ImmutableSet<String> issueIds, final SearchRestClient searchClient,
			final List<Issue> issues) {
		ImmutableSet<String> parentKeysToFetch = retrieveParentKeysToFetch(issueIds, issues);

        if (!parentKeysToFetch.isEmpty()) {
            logger.info("Fetching subtasks' parents which haven't been fetched already: {}", parentKeysToFetch);

            String parentSearchQuery = getSearchJQL(parentKeysToFetch);
            SearchResult parentSearchResult = searchClient.searchJql(parentSearchQuery).claim();
            Iterables.addAll(issues, parentSearchResult.getIssues());
        }
	}

	private ImmutableSet<String> retrieveParentKeysToFetch(final ImmutableSet<String> issueIds, final List<Issue> issues) {
		ImmutableSet<String> parentKeysToFetch = FluentIterable
                .from(issues)
                .filter(new Predicate<Issue>() {
                    @Override
                    public boolean apply(final Issue issue) {
                        return issue.getIssueType().isSubtask();
                    }
                })
                .transform(new Function<Issue, String>() {
                    @Override
                    public String apply(final Issue issue) {
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
