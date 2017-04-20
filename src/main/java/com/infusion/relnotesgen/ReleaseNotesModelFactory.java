package com.infusion.relnotesgen;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Version;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.infusion.relnotesgen.util.IssueCategorizer;
import com.infusion.relnotesgen.util.JiraUtils;

import java.util.*;

public class ReleaseNotesModelFactory {

    private final CommitInfoProvider commitInfoProvider;
    private final JiraConnector jiraConnector;
    private final IssueCategorizer issueCategorizer;
    private final VersionInfoProvider versionInfoProvider;
    private final JiraUtils jiraUtils;
    private final CommitMessageParser commitMessageParser;
    private final SCMFacade.Response gitInfo;
    private final Configuration configuration;

    public ReleaseNotesModelFactory(final CommitInfoProvider commitInfoProvider, final JiraConnector jiraConnector,
                                    final IssueCategorizer issueCategorizer, final VersionInfoProvider versionInfoProvider,
                                    final JiraUtils jiraUtils, final CommitMessageParser commitMessageParser,
                                    final SCMFacade.Response gitInfo, Configuration configuration) {
        this.commitInfoProvider = commitInfoProvider;
        this.jiraConnector = jiraConnector;
        this.issueCategorizer = issueCategorizer;
        this.versionInfoProvider = versionInfoProvider;
        this.jiraUtils = jiraUtils;
        this.commitMessageParser = commitMessageParser;
        this.gitInfo = gitInfo;
        this.configuration = configuration;
    }

    public ReleaseNotesModel get() {
        Iterable<CommitWithParsedInfo> commitsWithParsedInfo = generateCommitsWithParsedInfo(commitInfoProvider.getCommits());
        Map<String, Set<String>> prMap = generatePullRequestMap(commitsWithParsedInfo);
        ImmutableSet<String> issueIds = generateIssueIds(commitsWithParsedInfo);
        ImmutableMap<String, Issue> combinedJiraIssues = generateCombinedJiraIssues(issueIds);
        Map<String, Issue> combinedJiraIssuesNoSubtasks = filterOutSubtasks(combinedJiraIssues);
        // TODO Refactor return type
        Map<String, List<Issue>> jiraIssuesByType = issueCategorizer.byType(combinedJiraIssuesNoSubtasks.values());
        ImmutableSet<ReportJiraIssueModel> knownIssues = generateKnownIssues(configuration.getKnownIssues());

        ReleaseNotesModel model = new ReleaseNotesModel(getIssueTypes(jiraIssuesByType), getIssuesByType(jiraIssuesByType, prMap), 
        		getCommitsWithDefectIds(commitsWithParsedInfo, combinedJiraIssues), knownIssues, versionInfoProvider.getReleaseVersion(),
                gitInfo.commitTag1, gitInfo.commitTag2, gitInfo.commits.size(), gitInfo.gitBranch, configuration);

        return model;
    }

	private ImmutableSet<ReportJiraIssueModel> generateKnownIssues(String knownIssues) {
		ImmutableMap<String, Issue> knownIssuesMap = jiraConnector.getKnownIssuesByJql(configuration.getKnownIssues());
        ImmutableSet<ReportJiraIssueModel> knownIssuesModelSet = generateKnownIssuesModelSet(knownIssuesMap);
        return knownIssuesModelSet;
	}

	private ImmutableSet<ReportJiraIssueModel> generateKnownIssuesModelSet(ImmutableMap<String, Issue> knownIssuesMap) {
		List<Issue> knownIssuesList = knownIssuesMap.values().asList();
		
		
        return ImmutableSet.copyOf(Iterables.transform(knownIssuesList, new Function<Issue, ReportJiraIssueModel>() {
            @Override
            public ReportJiraIssueModel apply(Issue issue) {
                return toJiraIssueModel(issue, null);
            }
        }));
	}

	private Iterable<CommitWithParsedInfo> generateCommitsWithParsedInfo(ImmutableSet<Commit> commits) {
		Iterable<CommitWithParsedInfo> commitsWithParsedInfo = FluentIterable.from(commits)
                .transform(new Function<Commit, CommitWithParsedInfo>() {

                    @Override
                    public CommitWithParsedInfo apply(Commit commit) {
                        return new CommitWithParsedInfo(commit,
                                commitMessageParser.getJiraKeys(commit.getMessage()),
                                commitMessageParser.getDefectIds(commit.getMessage()),
                                commitMessageParser.getPullRequestId(commit.getMessage()));
                    }
                });
		return commitsWithParsedInfo;
	}

	private ImmutableSet<String> generateIssueIds(Iterable<CommitWithParsedInfo> commitsWithParsedInfo) {
		ImmutableSet<String> issueIds = FluentIterable.from(commitsWithParsedInfo)
                .transformAndConcat(new Function<CommitWithParsedInfo, Iterable<String>>() {

                    @Override
                    public Iterable<String> apply(CommitWithParsedInfo commitMessage) {
                        return commitMessage.getJiraIssueKeys();
                    }
                }).toSet();
		return issueIds;
	}

	private Map<String, Set<String>> generatePullRequestMap(Iterable<CommitWithParsedInfo> commitsWithParsedInfo) {
		Map<String,Set<String>> prMap = new HashMap<>();
        // add items to the map
        for(CommitWithParsedInfo c : commitsWithParsedInfo) {

            if(c.getPullRequestId() != null) {
                for (String jira : c.getJiraIssueKeys()) {

                    if(!prMap.containsKey(jira)) {
                        HashSet<String> set = new HashSet<String>();
                        set.add(c.getPullRequestId());
                        prMap.put(jira, set);
                    } else {
                        prMap.get(jira).add(c.getPullRequestId());
                    }
                }
            }
        }
		return prMap;
	}

	private Map<String, Issue> filterOutSubtasks(ImmutableMap<String, Issue> combinedJiraIssues) {
		Map<String, Issue> combinedJiraIssuesNoSubtasks = Maps.filterValues(combinedJiraIssues, new Predicate<Issue>() {

            @Override
            public boolean apply(Issue issue) {
                return !issue.getIssueType().isSubtask();
            }
        });
		return combinedJiraIssuesNoSubtasks;
	}
    
    public ImmutableMap<String, Issue> generateCombinedJiraIssues(ImmutableSet<String> issueIds) {
        ImmutableMap<String, Issue> fixVersionIssues = jiraConnector.getIssuesByFixVersions(configuration.getFixVersionsSet());
        ImmutableMap<String, Issue> jiraIssues = jiraConnector.getIssuesIncludeParents(issueIds);
        Map<String, Issue> temp = new HashMap<String, Issue>();
    	temp.putAll(fixVersionIssues);
    	temp.putAll(jiraIssues);
        return ImmutableMap.copyOf(temp);
	}

    private ImmutableSet<ReportCommitModel> getCommitsWithDefectIds(final Iterable<CommitWithParsedInfo> commitMessages,
                                                                    final ImmutableMap<String, Issue> jiraIssues) {

        return FluentIterable.from(commitMessages).filter(new Predicate<CommitWithParsedInfo>() {

            @Override
            public boolean apply(CommitWithParsedInfo commitWithParsedInfo) {
                ImmutableSet<String> defectIds = commitWithParsedInfo.getDefectIds();
                return !defectIds.isEmpty();
            }
        }).transform(new Function<CommitWithParsedInfo, ReportCommitModel>() {

            @Override
            public ReportCommitModel apply(CommitWithParsedInfo commitWithParsedInfo) {
                return toCommitModel(commitWithParsedInfo);
            }
        }).toSet();
    }

    private ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> getIssuesByType(
            Map<String, List<Issue>> jiraIssuesByType, final Map<String,Set<String>> pullRquestsMap) {

    	Map<String, ImmutableSet<ReportJiraIssueModel>> transformedMap = Maps
                .transformValues(jiraIssuesByType, new Function<List<Issue>, ImmutableSet<ReportJiraIssueModel>>() {

                    @Override
                    public ImmutableSet<ReportJiraIssueModel> apply(List<Issue> issues) {
                        return ImmutableSet
                                .copyOf(Iterables.transform(issues, new Function<Issue, ReportJiraIssueModel>() {

                                    @Override
                                    public ReportJiraIssueModel apply(Issue issue) {

                                        if(pullRquestsMap.containsKey(issue.getKey())) {
                                            return toJiraIssueModel(issue, pullRquestsMap.get(issue.getKey()));
                                        }
                                        else
                                        return toJiraIssueModel(issue, null);
                                    }
                                }));
                    }
                });

        return ImmutableMap.copyOf(transformedMap);
    }

    private ImmutableSet<String> getIssueTypes(Map<String, List<Issue>> jiraIssuesByType) {
        return ImmutableSet.copyOf(jiraIssuesByType.keySet());
    }

    public static String concatNotNullNotEmpty(String separator, String... ss) {
        StringBuilder sb = new StringBuilder();
        boolean priorAppend = false;
        for (String s : ss) {
            if (s != null && !s.equals("")) {
                if (priorAppend) {
                    sb.append(separator);
                }
                sb.append(s);
                priorAppend = true;
            }
        }
        return sb.toString();
    }

    private ReportJiraIssueModel toJiraIssueModel(Issue issue, Set<String> pullRequestIds) {
        final String defectId = jiraUtils.getFieldValueByNameSafe(issue, "Defect_Id");
        final String requirementId = jiraUtils.getFieldValueByNameSafe(issue, "Requirement VA ID");
        final String id = concatNotNullNotEmpty(" ", defectId, requirementId);
        final String fixedInVersion = jiraUtils.getFieldValueByNameSafe(issue, "FixedInFlowWebVersion");
        final String url = jiraUtils.getIssueUrl(issue);
        final String releaseNotes = jiraUtils.getFieldValueByNameSafe(issue, "Release Notes");
        final String impact = jiraUtils.getFieldValueByNameSafe(issue, "Impact");
        final String detailsOfChange = jiraUtils.getFieldValueByNameSafe(issue, "Details of change");
        final FluentIterable<String> fixVersions = FluentIterable.from(issue.getFixVersions()).transform(new Function<Version, String>() {

            @Override
            public String apply(Version version) {
                return version.getName();
            }
        });

        final String status = issue.getStatus().getName();
        final boolean isStatusOk = FluentIterable.from(Arrays.asList(configuration.getCompletedStatuses())).anyMatch(new Predicate<String>() {
            @Override
            public boolean apply(String s) {
                return s.equals(status);
            }
        });

        return new ReportJiraIssueModel(issue, id, url, fixedInVersion, releaseNotes, fixVersions, impact, detailsOfChange, pullRequestIds, isStatusOk, status);
    }

    private ReportCommitModel toCommitModel(CommitWithParsedInfo commitWithParsedInfo) {
        return new ReportCommitModel(commitWithParsedInfo.getCommit().getId(),
                commitWithParsedInfo.getCommit().getMessage(), commitWithParsedInfo.getDefectIds(),
                commitWithParsedInfo.commit.getAuthor());
    }

    private static class CommitWithParsedInfo {

        private final ImmutableSet<String> jiraIssueKeys;
        private final ImmutableSet<String> defectIds;
        private final String pullRequestId;
        private final Commit commit;

        CommitWithParsedInfo(final Commit commit, final ImmutableSet<String> jiraIssueKeys,
                             final ImmutableSet<String> defectIds, String pullRequestId) {
            this.commit = commit;
            this.jiraIssueKeys = jiraIssueKeys;
            this.defectIds = defectIds;
            this.pullRequestId = pullRequestId;
        }

        public ImmutableSet<String> getJiraIssueKeys() {
            return jiraIssueKeys;
        }

        public ImmutableSet<String> getDefectIds() {
            return defectIds;
        }

        public Commit getCommit() {
            return commit;
        }

        public String getPullRequestId() {
            return pullRequestId;
        }
    }
}
