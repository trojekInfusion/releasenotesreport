package com.infusion.relnotesgen;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.rest.client.IssueRestClient;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.RestClientException;
import com.atlassian.jira.rest.client.domain.BasicComponent;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;

/**
 * @author trojek
 *
 */
public class JiraIssueDao {

    private final static Logger logger = LoggerFactory.getLogger(JiraIssueDao.class);

    private IssueRestClient issueRestClient;
    private Configuration configuration;

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
                        issues.add(issue);
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
        if (isNotEmpty(configuration.getIssueFilterByType())) {
            String[] acceptableTypes = configuration.getIssueFilterByType().split(",");
            boolean found = false;
            for (String acceptableType : acceptableTypes) {
                if (acceptableType.equalsIgnoreCase(issue.getIssueType().getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                logger.info("Filtered by type issue {} {}", issueId, issue.getSummary());
                return null;
            }
        }
        if (isNotEmpty(configuration.getIssueFilterByComponent())) {
            boolean found = false;
            String[] acceptableComponents = configuration.getIssueFilterByComponent().split(",");
            for (String acceptableComponent : acceptableComponents) {
                for (BasicComponent component : issue.getComponents()) {
                    if (containsIgnoreCase(component.getName(), acceptableComponent)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                logger.info("Filtered by component issue {} {}", issueId, issue.getSummary());
                return null;
            }
        }
        if (isNotEmpty(configuration.getIssueFilterByLabel())) {
            boolean found = false;
            String[] acceptableLabels = configuration.getIssueFilterByLabel().split(",");
            for (String acceptableLabel : acceptableLabels) {
                for (String label : issue.getLabels()) {
                    if (containsIgnoreCase(label, acceptableLabel)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                logger.info("Filtered by label issue {} {}", issueId, issue.getSummary());
                return null;
            }
        }
        return issue;
    }
}
