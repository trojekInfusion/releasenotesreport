package com.infusion.relnotesgen;

import com.atlassian.jira.rest.client.IssueRestClient;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.RestClientException;
import com.atlassian.jira.rest.client.domain.BasicComponent;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author trojek
 */
public class JiraIssueDao {

    private final static Logger logger = LoggerFactory.getLogger(Configuration.LOGGER_NAME);

    private IssueRestClient issueRestClient;
    private Configuration configuration;
    private Collection<Filter> filters = new ArrayList<>();

    public JiraIssueDao(final Configuration configuration) {
        logger.info("Creating jira rest client with url {} and user {}", configuration.getJiraUrl(),
                configuration.getJiraUsername());

        try {
            this.configuration = configuration;

            JerseyJiraRestClientFactory factory = new JerseyJiraRestClientFactory();
            JiraRestClient restClient = factory.createWithBasicHttpAuthentication(new URI(configuration.getJiraUrl()),
                    configuration.getJiraUsername(), configuration.getJiraPassword());
            issueRestClient = restClient.getIssueClient();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        prepareFilters();
    }

    public Collection<Issue> findIssues(final Set<String> issueIds) {
        try {
            Collection<Issue> issues = new ArrayList<>();
            NullProgressMonitor pm = new NullProgressMonitor();
            for (String issueId : issueIds) {
                logger.info("Quering JIRA for issue {}", issueId);
                try {
                    Issue issue = getAndFilter(pm, issueId);

                    if (issue != null) {

                        if (issue.getIssueType().isSubtask()) {
                            // need to find parent
                            String parentKey = ((JSONObject) issue.getFieldByName("Parent").getValue()).get("key").toString();

                            Issue parent= getAndFilter(pm, parentKey);
                            issues.add(parent);
                        } else {
                            issues.add(issue);
                        }
                    }

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

            return issues;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    private interface FilterPredicate {

        boolean match(final Issue issue, final String value);
    }
}
