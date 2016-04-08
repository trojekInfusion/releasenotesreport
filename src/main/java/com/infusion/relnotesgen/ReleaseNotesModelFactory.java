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
    private final JiraIssueIdMatcher jiraIssueIdMatcher;
    private final VersionInfoProvider versionInfoProvider;
    private final JiraUtils jiraUtils;
    private final Parser parser;

    public ReleaseNotesModelFactory(
            final CommitInfoProvider commitInfoProvider,
            final JiraConnector jiraConnector,
            final IssueCategorizer issueCategorizer,
            final JiraIssueIdMatcher jiraIssueIdMatcher,
            final VersionInfoProvider versionInfoProvider,
            final JiraUtils jiraUtils,
            final Parser parser) {
        this.commitInfoProvider = commitInfoProvider;
        this.jiraConnector = jiraConnector;
        this.issueCategorizer = issueCategorizer;
        this.jiraIssueIdMatcher = jiraIssueIdMatcher;
        this.versionInfoProvider = versionInfoProvider;
        this.jiraUtils = jiraUtils;
        this.parser = parser;
    }

    public ReleaseNotesModel get() {
        String version = versionInfoProvider.getReleaseVersion();
        ImmutableList<String> commitMessagesAsStrings = commitInfoProvider.getCommitMessages();
        Iterable<CommitMessage> commitMessages = FluentIterable
            .from(commitMessagesAsStrings)
            .transform(new Function<String, CommitMessage>() {
                @Override
                public CommitMessage apply(String commitMessageString) {
                    return new CommitMessage(
                            commitMessageString,
                            parser.getJiraKeys(commitMessageString),
                            parser.getDefectIds(commitMessageString));
                        }
            });
        FluentIterable<String> issueIds = FluentIterable
                .from(commitMessages)
                .transformAndConcat(
                    new Function<CommitMessage, Iterable<String>>() {
                        @Override
                        public Iterable<String> apply(CommitMessage commitMessage) {
                            return commitMessage.getJiraIssueKeys();
                        }
                    });

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
                getCommitsWithNoJiraIssues(commitMessages, jiraIssues),
                version);

        return model;
    }

    private ImmutableSet<ReleaseNotesModel.CommitModel> getCommitsWithNoJiraIssues(
            final Iterable<CommitMessage> commitMessages,
            final ImmutableMap<String, Issue> jiraIssues) {

        return FluentIterable
                .from(commitMessages)
                .filter(new Predicate<CommitMessage>() {
                    @Override
                    public boolean apply(CommitMessage commitMessage) {
                        ImmutableSet<String> issueKeys = commitMessage.getJiraIssueKeys();
                        return issueKeys.isEmpty() || Iterables.all(issueKeys, new Predicate<String>() {
                            @Override
                            public boolean apply(String jiraId) {
                                return !jiraIssues.containsKey(jiraId);
                            }
                        });
                    }
                })
                .transform(new Function<CommitMessage, ReleaseNotesModel.CommitModel>() {
                    @Override
                    public ReleaseNotesModel.CommitModel apply(CommitMessage commitMessage) {
                        return toCommitModel(commitMessage);
                    }
                })
                .toImmutableSet();
    }

    private ImmutableMap<String, ImmutableSet<ReleaseNotesModel.JiraIssueModel>> getIssuesByType(
            Map<String, List<Issue>> jiraIssuesByType) {
        Map<String, ImmutableSet<ReleaseNotesModel.JiraIssueModel>> transformedMap =
            Maps.transformValues(
                jiraIssuesByType,
                new Function<List<Issue>, ImmutableSet<ReleaseNotesModel.JiraIssueModel>>() {
                    @Override
                    public ImmutableSet<ReleaseNotesModel.JiraIssueModel> apply(List<Issue> issues) {
                        return ImmutableSet.copyOf(Iterables.transform(issues, new Function<Issue, ReleaseNotesModel.JiraIssueModel>() {
                            @Override
                            public ReleaseNotesModel.JiraIssueModel apply(Issue issue) {
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

    private ReleaseNotesModel.JiraIssueModel toJiraIssueModel(Issue issue) {
        return new ReleaseNotesModel.JiraIssueModel(
                issue,
                jiraUtils.getFieldValueByNameSafe(issue, "Defect_Id"));
    }

    private ReleaseNotesModel.CommitModel toCommitModel(CommitMessage commitMessage) {
        return new ReleaseNotesModel.CommitModel(commitMessage.getMessage(), commitMessage.getDefectIds());
    }

    public static class CommitMessage {
        private final String message;
        private final ImmutableSet<String> jiraIssueKeys;
        private final ImmutableSet<String> defectIds;

        private CommitMessage(
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
