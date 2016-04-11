package com.infusion.relnotesgen;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ReleaseNotesModel {
    private final ImmutableSet<String> issueCategoryNames;
    private final ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> issuesByCategory;
    private final ImmutableSet<ReportCommitModel> commitsWithDefectIds;
    private final String releaseVersion;
    private final SCMFacade.GitCommitTag commitTag1;
    private final SCMFacade.GitCommitTag commitTag2;
    private final int commitsCount;
    private final String gitBranch;

    public ReleaseNotesModel(final ImmutableSet<String> issueCategoryNames, final ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> issuesByCategory,
            final ImmutableSet<ReportCommitModel> commitsWithDefectIds, final String releaseVersion,
            final SCMFacade.GitCommitTag commitTag1, final SCMFacade.GitCommitTag commitTag2, final int commitsCount,
            final String gitBranch) {
        this.issueCategoryNames = issueCategoryNames;
        this.issuesByCategory = issuesByCategory;
        this.commitsWithDefectIds = commitsWithDefectIds;
        this.releaseVersion = releaseVersion;
        this.commitTag1 = commitTag1;
        this.commitTag2 = commitTag2;
        this.commitsCount = commitsCount;
        this.gitBranch = gitBranch;
    }

    public ImmutableSet<String> getIssueCategoryNames() {
        return issueCategoryNames;
    }

    public ImmutableSet<ReportJiraIssueModel> getIssuesByCategoryName(String categoryName) {
        return issuesByCategory.get(categoryName);
    }

    public ImmutableSet<ReportCommitModel> getCommitsWithDefectIds() {
        return commitsWithDefectIds;
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

    public String getGitBranch() {
        return gitBranch;
    }
}
