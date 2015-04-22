package com.infusion.relnotesgen;

import java.util.Properties;


/**
 * @author trojek
 *
 */
public class Configuration {

    private Properties properties;

    public Configuration(final Properties properties) {
        this.properties = properties;
    }

    public String getGitDirectory() {
        return properties.getProperty("git.directory");
    }

    public String getGitBranch() {
        return properties.getProperty("git.branch");
    }

    public String getGitUrl() {
        return properties.getProperty("git.url");
    }

    public String getGitUsername() {
        return properties.getProperty("git.username");
    }

    public String getGitPassword() {
        return properties.getProperty("git.password");
    }

    public String getGitCommitterName() {
        return properties.getProperty("git.committer.name");
    }

    public String getGitCommitterMail() {
        return properties.getProperty("git.committer.mail");
    }

    public String getGitCommitMessageValidationOmmitter() {
        return properties.getProperty("git.commitmessage.validationommiter");
    }

    public String getJiraUrl() {
        return properties.getProperty("jira.url");
    }

    public String getJiraUsername() {
        return properties.getProperty("jira.username");
    }

    public String getJiraPassword() {
        return properties.getProperty("jira.password");
    }

    public String getJiraIssuePattern() {
        return properties.getProperty("jira.issuepattern");
    }

    public String getIssueFilterByComponent() {
        return properties.getProperty("issue.filterby.component");
    }

    public String getIssueFilterByType() {
        return properties.getProperty("issue.filterby.type");
    }

    public String getIssueFilterByLabel() {
        return properties.getProperty("issue.filterby.label");
    }

    public String getIssueSortType() {
        return properties.getProperty("issue.sort.type");
    }

    public String getIssueSortPriority() {
        return properties.getProperty("issue.sort.priority");
    }
}
