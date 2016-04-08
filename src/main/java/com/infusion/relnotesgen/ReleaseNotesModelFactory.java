package com.infusion.relnotesgen;

import com.atlassian.jira.rest.client.domain.Issue;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.infusion.relnotesgen.util.IssueCategorizer;
import com.infusion.relnotesgen.util.JiraUtils;

import java.util.List;
import java.util.Map;

public class ReleaseNotesModelFactory {
    private final CommitInfoProvider commitInfoProvider;
    private final JiraConnector jiraConnector;
    private final IssueCategorizer issueCategorizer;
    private final VersionInfoProvider versionInfoProvider;
    private final JiraUtils jiraUtils;
    private final CommitMessageParser commitMessageParser;

    public ReleaseNotesModelFactory(
            final CommitInfoProvider commitInfoProvider,
            final JiraConnector jiraConnector,
            final IssueCategorizer issueCategorizer,
            final VersionInfoProvider versionInfoProvider,
            final JiraUtils jiraUtils,
            final CommitMessageParser commitMessageParser) {
        this.commitInfoProvider = commitInfoProvider;
        this.jiraConnector = jiraConnector;
        this.issueCategorizer = issueCategorizer;
        this.versionInfoProvider = versionInfoProvider;
        this.jiraUtils = jiraUtils;
        this.commitMessageParser = commitMessageParser;
    }

    public ReleaseNotesModel get() {
        String version = versionInfoProvider.getReleaseVersion();
        ImmutableList<String> commitMessagesAsString = FluentIterable.from(commitInfoProvider.getCommitMessages()).toImmutableList();
        Iterable<Commit> commits = FluentIterable
            .from(commitMessagesAsString)
            .transform(new Function<String, Commit>() {
                @Override
                public Commit apply(String commit) {
                    return new Commit(
                            commit,
                            commitMessageParser.getJiraKeys(commit),
                            commitMessageParser.getDefectIds(commit));
                        }
            });
        ImmutableSet<String> issueIds = FluentIterable
                .from(commits)
                .transformAndConcat(
                    new Function<Commit, Iterable<String>>() {
                        @Override
                        public Iterable<String> apply(Commit commitMessage) {
                            return commitMessage.getJiraIssueKeys();
                        }
                    })
                .toImmutableSet();

        ImmutableMap<String, Issue> jiraIssues = jiraConnector.getIssuesIncludeParents(issueIds);

        // Filtering out subtasks
        Map<String, Issue> jiraIssuesNoSubtasks = Maps.filterValues(
                jiraIssues,
                new Predicate<Issue>() {
                    @Override
                    public boolean apply(Issue issue) {
                        return !issue.getIssueType().isSubtask();
                    }
                });

        // TODO Refactor return type
        Map<String, List<Issue>> jiraIssuesByType = issueCategorizer.byType(jiraIssuesNoSubtasks.values());

        ReleaseNotesModel model = new ReleaseNotesModel(
                getIssueTypes(jiraIssuesByType),
                getIssuesByType(jiraIssuesByType),
                getCommitsWithNoJiraIssues(commits, jiraIssues),
                version);

        return model;
    }

    private ImmutableSet<ReportCommitModel> getCommitsWithNoJiraIssues(
            final Iterable<Commit> commitMessages,
            final ImmutableMap<String, Issue> jiraIssues) {

        return FluentIterable
                .from(commitMessages)
                .filter(new Predicate<Commit>() {
                    @Override
                    public boolean apply(Commit commit) {
                        ImmutableSet<String> issueKeys = commit.getJiraIssueKeys();
                        return issueKeys.isEmpty() || Iterables.all(issueKeys, new Predicate<String>() {
                            @Override
                            public boolean apply(String jiraId) {
                                return !jiraIssues.containsKey(jiraId);
                            }
                        });
                    }
                })
                .transform(new Function<Commit, ReportCommitModel>() {
                    @Override
                    public ReportCommitModel apply(Commit commit) {
                        return toCommitModel(commit);
                    }
                })
                .toImmutableSet();
    }

    private ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> getIssuesByType(
            Map<String, List<Issue>> jiraIssuesByType) {
        Map<String, ImmutableSet<ReportJiraIssueModel>> transformedMap =
            Maps.transformValues(
                jiraIssuesByType,
                new Function<List<Issue>, ImmutableSet<ReportJiraIssueModel>>() {
                    @Override
                    public ImmutableSet<ReportJiraIssueModel> apply(List<Issue> issues) {
                        return ImmutableSet.copyOf(Iterables.transform(issues, new Function<Issue, ReportJiraIssueModel>() {
                            @Override
                            public ReportJiraIssueModel apply(Issue issue) {
                                return toJiraIssueModel(issue);
                            }
                        }));
                    }
                });

        return ImmutableMap.copyOf(transformedMap);
    }

    private ImmutableSet<String> getIssueTypes(
            Map<String, List<Issue>> jiraIssuesByType) {
        return ImmutableSet.copyOf(jiraIssuesByType.keySet());
    }

    private ReportJiraIssueModel toJiraIssueModel(Issue issue) {
        return new ReportJiraIssueModel(
                issue,
                jiraUtils.getFieldValueByNameSafe(issue, "Defect_Id"),
                jiraConnector.getIssueUrl(issue));
    }

    private ReportCommitModel toCommitModel(Commit commit) {
        return new ReportCommitModel(commit.getMessage(), commit.getDefectIds());
    }

    public static class Commit {
        private final String message;
        private final ImmutableSet<String> jiraIssueKeys;
        private final ImmutableSet<String> defectIds;

        private Commit(
                final String message,
                final ImmutableSet<String> jiraIssueKeys,
                final ImmutableSet<String> defectIds) {
            this.message = message;
            this.jiraIssueKeys = jiraIssueKeys;
            this.defectIds = defectIds;
        }

        public String getMessage() {
            return message;
        }

        public ImmutableSet<String> getJiraIssueKeys() {
            return jiraIssueKeys;
        }

        public ImmutableSet<String> getDefectIds() {
            return defectIds;
        }
    }
}
