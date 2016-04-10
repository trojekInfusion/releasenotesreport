package com.infusion.relnotesgen;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ReleaseNotesModel {
    private final ImmutableSet<String> issueCategoryNames;
    private final ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> issuesByCategory;
    private final ImmutableSet<ReportCommitModel> commitsWithNoIssue;
    private final String releaseVersion;
    private final SCMFacade.GitCommitTag commitTag1;
    private final SCMFacade.GitCommitTag commitTag2;
    private final int commitsCount;

    public ReleaseNotesModel(final ImmutableSet<String> issueCategoryNames, final ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> issuesByCategory,
            final ImmutableSet<ReportCommitModel> commitsWithNoIssue, final String releaseVersion,
            final SCMFacade.GitCommitTag commitTag1, final SCMFacade.GitCommitTag commitTag2, final int commitsCount) {
        this.issueCategoryNames = issueCategoryNames;
        this.issuesByCategory = issuesByCategory;
        this.commitsWithNoIssue = commitsWithNoIssue;
        this.releaseVersion = releaseVersion;
        this.commitTag1 = commitTag1;
        this.commitTag2 = commitTag2;
        this.commitsCount = commitsCount;
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

    public SCMFacade.GitCommitTag getCommitTag1() {
        return commitTag1;
    }

    public SCMFacade.GitCommitTag getCommitTag2() {
        return commitTag2;
    }

    public int getCommitsCount() {
        return commitsCount;
    }
}
