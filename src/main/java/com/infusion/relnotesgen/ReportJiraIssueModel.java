package com.infusion.relnotesgen;

import com.atlassian.jira.rest.client.domain.Issue;

public class ReportJiraIssueModel {
    private final Issue issue;
    private final String defectId;
    private final String url;

    public ReportJiraIssueModel(final Issue issue, final String defectId, final String url) {
        this.issue = issue;
        this.defectId = defectId;
        this.url = url;
    }

    public Issue getIssue() {
        return issue;
    }

    public String getDefectId() {
        return defectId;
    }

    public String getUrl() {
        return url;
    }
}
