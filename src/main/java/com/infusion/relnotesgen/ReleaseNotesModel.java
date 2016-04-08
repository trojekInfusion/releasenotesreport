package com.infusion.relnotesgen;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ReleaseNotesModel {
    private final ImmutableSet<String> issueCategoryNames;
    private final ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> issuesByCategory;
    private final ImmutableSet<ReportCommitModel> commitsWithNoIssue;
    private final String releaseVersion;

    public ReleaseNotesModel(final ImmutableSet<String> issueCategoryNames,
                             final ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> issuesByCategory,
                             final ImmutableSet<ReportCommitModel> commitsWithNoIssue,
                             final String releaseVersion) {
        this.issueCategoryNames = issueCategoryNames;
        this.issuesByCategory = issuesByCategory;
        this.commitsWithNoIssue = commitsWithNoIssue;
        this.releaseVersion = releaseVersion;
    }

    public ImmutableSet<String> getIssueCategoryNames() {
        return issueCategoryNames;
    }

    public ImmutableSet<ReportJiraIssueModel> getIssuesByCategoryName(String categoryName) {
        return issuesByCategory.get(categoryName);
    }

    public ImmutableSet<ReportCommitModel> getCommitsWithNoIssue() {
        return commitsWithNoIssue;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

}
