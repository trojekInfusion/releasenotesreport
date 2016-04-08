package com.infusion.relnotesgen;

import com.atlassian.jira.rest.client.domain.Issue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ReleaseNotesModel {
    private final ImmutableSet<String> issueCategoryNames;
    private final ImmutableMap<String, ImmutableSet<JiraIssueModel>> issuesByCategory;
    private final ImmutableSet<CommitModel> commitsWithNoIssue;
    private final String releaseVersion;

    public ReleaseNotesModel(final ImmutableSet<String> issueCategoryNames,
                             final ImmutableMap<String, ImmutableSet<JiraIssueModel>> issuesByCategory,
                             final ImmutableSet<CommitModel> commitsWithNoIssue,
                             final String releaseVersion) {
        this.issueCategoryNames = issueCategoryNames;
        this.issuesByCategory = issuesByCategory;
        this.commitsWithNoIssue = commitsWithNoIssue;
        this.releaseVersion = releaseVersion;
    }

    public ImmutableSet<String> getIssueCategoryNames() {
        return issueCategoryNames;
    }

    public ImmutableSet<JiraIssueModel> getIssuesByCategoryName(String categoryName) {
        return issuesByCategory.get(categoryName);
    }

    public ImmutableSet<CommitModel> getCommitsWithNoIssue() {
        return commitsWithNoIssue;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public static class JiraIssueModel {
        private final Issue issue;
        private final String defectId;

        public JiraIssueModel(Issue issue, String defectId) {
            this.issue = issue;
            this.defectId = defectId;
        }

        public Issue getIssue() {
            return issue;
        }

        public String getDefectId() {
            return defectId;
        }
    }

    public static class CommitModel {
        private final String message;
        private final ImmutableSet<String> defectIds;

        public CommitModel(String message, ImmutableSet<String> defectIds) {
            this.message = message;
            this.defectIds = defectIds;
        }

        public ImmutableSet<String> getDefectIds() {
            return defectIds;
        }

        public String getMessage() {
            return message;
        }
    }
}
