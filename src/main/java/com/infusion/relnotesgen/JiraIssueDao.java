package com.infusion.relnotesgen;

import com.atlassian.jira.rest.client.*;
import com.atlassian.jira.rest.client.domain.BasicComponent;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import freemarker.template.utility.NullArgumentException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.annotation.Immutable;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author trojek
 */
public class JiraIssueDao {

    private final static Logger logger = LoggerFactory.getLogger(Configuration.LOGGER_NAME);
    private final JiraRestClient jiraClient;

    private IssueRestClient issueRestClient;
    private Configuration configuration;
    private Collection<Filter> filters = new ArrayList<>();

    public JiraIssueDao(final Configuration configuration) {
        logger.info("Creating jira rest client with url {} and user {}", configuration.getJiraUrl(),
                configuration.getJiraUsername());

        this.configuration = configuration;

        URI jiraUri;

        try {
            jiraUri = new URI(configuration.getJiraUrl());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Configuration invalid: JIRA URL");
        }

        JerseyJiraRestClientFactory factory = new JerseyJiraRestClientFactory();
        JiraRestClient jiraClient = factory.createWithBasicHttpAuthentication(
                jiraUri, configuration.getJiraUsername(), configuration.getJiraPassword());
        issueRestClient = jiraClient.getIssueClient();
        this.jiraClient = jiraClient;
        prepareFilters();
    }

    public ImmutableCollection<Issue> findIssues(final Set<String> issueIds) {
        return findIssuesAsMap(ImmutableSet.copyOf(issueIds)).values();
    }

    public ImmutableMap<String, Issue> findIssuesAsMap(final ImmutableSet<String> issueIds) {
        if (issueIds == null) {
            throw new NullArgumentException("issueIds");
        }

        Map<String, Issue> issuesMap = new HashMap<>();
        final NullProgressMonitor pm = new NullProgressMonitor();

        for (final String issueId : issueIds) {
            logger.info("Quering JIRA for issue {}", issueId);
            try {
                Issue issue = getOrDefault(issuesMap, issueId, new Supplier<Issue>() {
                    @Override
                    public Issue get() {
                        return getAndFilter(pm, issueId);
                    }
                });

                if (issue == null) {
                    continue;
                }

                if (!issue.getIssueType().isSubtask()) {
                    issuesMap.put(issueId, issue);
                    continue;
                }

                // TODO Should put subtasks to the map too and then remove them (optimization)

                // need to find parent
                final String parentKey;

                try {
                    parentKey = ((JSONObject) issue.getFieldByName("Parent").getValue()).get("key").toString();
                } catch (JSONException e) {
                    throw new RuntimeException(MessageFormat.format("Malformed Parent field in issue {0}", issue.getKey()));
                }

                Issue parent = getOrDefault(issuesMap, parentKey, new Supplier<Issue>() {
                    @Override
                    public Issue get() {
                        return getAndFilter(pm, issueId);
                    }
                });

                issuesMap.put(parentKey, parent);
            } catch (RestClientException e) {
                String message = ExceptionUtils.getRootCauseMessage(e);
                if (message.contains("response status: 404")) {
                    logger.warn(StringUtils.repeat('=', 60));
                    logger.warn("--- 404 status returned for issue {}.", issueId);
                    logger.warn("--- Bad pattern definition or issue has been deleted.");
                    logger.warn(StringUtils.repeat('=', 60));
                } else {
                    throw e;
                }
            }
        }

        return ImmutableMap.<String, Issue>builder().putAll(issuesMap).build();
    }

    private Issue getAndFilter(final NullProgressMonitor pm, final String issueId) {
        Issue issue = issueRestClient.getIssue(issueId, pm);

        for (Filter filter : filters) {
            if (filter.filter(issue)) {
                logger.info("Filtered issue '{} {}' with filter '{}'", issue.getKey(), issue.getSummary(), filter);
                return null;
            }
        }

        return issue;
    }

    private void prepareFilters() {
        prepareFilterByType();
        prepareFilterByComponent();
        prepareFilterByLabel();
        prepareFilterByStatus();
    }

    private void prepareFilterByType() {
        filters.add(new Filter(configuration.getIssueFilterByType(), new FilterPredicate() {

            @Override
            public boolean match(final Issue issue, final String type) {
                return type.equalsIgnoreCase(issue.getIssueType().getName());
            }

            @Override
            public String toString() {
                return "filter predicate by type";
            }
        }));
    }

    private void prepareFilterByComponent() {
        filters.add(new Filter(configuration.getIssueFilterByComponent(), new FilterPredicate() {

            @Override
            public boolean match(final Issue issue, final String componentName) {
                for (BasicComponent component : issue.getComponents()) {
                    if (containsIgnoreCase(component.getName(), componentName)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String toString() {
                return "filter predicate by component";
            }
        }));
    }

    private void prepareFilterByLabel() {
        filters.add(new Filter(configuration.getIssueFilterByLabel(), new FilterPredicate() {

            @Override
            public boolean match(final Issue issue, final String labelValue) {
                for (String label : issue.getLabels()) {
                    if (containsIgnoreCase(label, labelValue)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String toString() {
                return "filter predicate by label";
            }
        }));
    }

    private void prepareFilterByStatus() {
        filters.add(new Filter(configuration.getIssueFilterByStatus(), new FilterPredicate() {

            @Override
            public boolean match(final Issue issue, final String status) {
                return status.equalsIgnoreCase(issue.getStatus().getName());
            }

            @Override
            public String toString() {
                return "filter predicate by status";
            }
        }));
    }

    private class Filter {

        final String[] filters;
        final FilterPredicate predicate;

        public Filter(final String filters, final FilterPredicate predicate) {
            this.filters = isBlank(filters) ? null : filters.split(",");
            this.predicate = predicate;
        }

        public boolean filter(final Issue issue) {
            if (filters != null) {
                for (String filter : filters) {
                    if (predicate.match(issue, filter)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return predicate.toString() + " with values " + Arrays.toString(filters);
        }
    }

    private static <K,V> V getOrDefault(Map<K,V> map, K key, Supplier<V> defaultSupplier) {
        return map.containsKey(key) ? map.get(key) : defaultSupplier.get();
    }

    private interface FilterPredicate {

        boolean match(final Issue issue, final String value);
    }
}
