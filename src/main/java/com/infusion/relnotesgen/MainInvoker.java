/**
 *
 */
package com.infusion.relnotesgen;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author trojek
 *
 */
public class MainInvoker {

    private String commitId1;
    private String commitId2;
    private String tag1;
    private String tag2;
    private String gitDirectory;
    private String gitBranch;
    private String gitUrl;
    private String gitUsername;
    private String gitPassword;
    private String gitCommitterName;
    private String gitCommitterMail;
    private String gitCommitMessageValidationOmmiter;
    private boolean pushReleaseNotes;
    private String jiraUrl;
    private String jiraUsername;
    private String jiraPassword;
    private String jiraIssuePattern;
    private String issueFilterByComponent;
    private String issueFilterByType;
    private String issueFilterByLabel;
    private String issueFilterByStatus;
    private String issueSortType;
    private String issueSortPriority;
    private String reportDirectory;
    private String reportTemplate;

    public void invoke() {
        List<String> arguments = new ArrayList<>();

        for (Field field : MainInvoker.class.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(this);
                if(value != null) {
                    if(value instanceof String) {
                        arguments.add("-" + field.getName());
                        arguments.add(value.toString());
                    } else if (value instanceof Boolean && ((Boolean) value)) {
                        arguments.add("-" + field.getName());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        String[] args = arguments.toArray(new String[0]);
        try {
            Main.main(args);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MainInvoker commitStart(final String commitStart) {
        this.commitId1 = commitStart;
        return this;
    }

    public MainInvoker commitEnd(final String commitEnd) {
        this.commitId2 = commitEnd;
        return this;
    }

    public MainInvoker tagStart(final String tagStart) {
        this.tag1 = tagStart;
        return this;
    }

    public MainInvoker tagEnd(final String tagEnd) {
        this.tag2 = tagEnd;
        return this;
    }

    public MainInvoker gitDirectory(final String gitDirectory) {
        this.gitDirectory = gitDirectory;
        return this;
    }

    public MainInvoker gitBranch(final String gitBranch) {
        this.gitBranch = gitBranch;
        return this;
    }

    public MainInvoker gitUrl(final String gitUrl) {
        this.gitUrl = gitUrl;
        return this;
    }

    public MainInvoker gitUsername(final String gitUsername) {
        this.gitUsername = gitUsername;
        return this;
    }

    public MainInvoker gitPassword(final String gitPassword) {
        this.gitPassword = gitPassword;
        return this;
    }

    public MainInvoker gitCommitterName(final String gitCommitterName) {
        this.gitCommitterName = gitCommitterName;
        return this;
    }

    public MainInvoker gitCommitterMail(final String gitCommitterMail) {
        this.gitCommitterMail = gitCommitterMail;
        return this;
    }

    public MainInvoker gitCommitMessageValidationOmmiter(final String gitCommitMessageValidationOmmiter) {
        this.gitCommitMessageValidationOmmiter = gitCommitMessageValidationOmmiter;
        return this;
    }

    public MainInvoker pushReleaseNotes(final boolean pushReleaseNotes) {
        this.pushReleaseNotes = pushReleaseNotes;
        return this;
    }

    public MainInvoker jiraUrl(final String jiraUrl) {
        this.jiraUrl = jiraUrl;
        return this;
    }

    public MainInvoker jiraUsername(final String jiraUsername) {
        this.jiraUsername = jiraUsername;
        return this;
    }

    public MainInvoker jiraPassword(final String jiraPassword) {
        this.jiraPassword = jiraPassword;
        return this;
    }

    public MainInvoker jiraIssuePattern(final String jiraIssuePattern) {
        this.jiraIssuePattern = jiraIssuePattern;
        return this;
    }

    public MainInvoker issueFilterByComponent(final String issueFilterByComponent) {
        this.issueFilterByComponent = issueFilterByComponent;
        return this;
    }

    public MainInvoker issueFilterByType(final String issueFilterByType) {
        this.issueFilterByType = issueFilterByType;
        return this;
    }

    public MainInvoker issueFilterByLabel(final String issueFilterByLabel) {
        this.issueFilterByLabel = issueFilterByLabel;
        return this;
    }

    public MainInvoker issueFilterByStatus(final String issueFilterByStatus) {
        this.issueFilterByStatus = issueFilterByStatus;
        return this;
    }

    public MainInvoker issueSortType(final String issueSortType) {
        this.issueSortType = issueSortType;
        return this;
    }

    public MainInvoker issueSortPriority(final String issueSortPriority) {
        this.issueSortPriority = issueSortPriority;
        return this;
    }

    public MainInvoker reportDirectory(final String reportDirectory) {
        this.reportDirectory = reportDirectory;
        return this;
    }

    public MainInvoker reportTemplate(final String reportTemplate) {
        this.reportTemplate = reportTemplate;
        return this;
    }

}
