package com.infusion.relnotesgen;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.Version;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.infusion.relnotesgen.util.CollectionUtils;
import com.infusion.relnotesgen.util.IssueCategorizer;
import com.infusion.relnotesgen.util.JiraIssueSearchType;
import com.infusion.relnotesgen.util.JiraUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReleaseNotesModelFactory {

    private final static Logger logger = LoggerFactory.getLogger(Configuration.LOGGER_NAME);

    private final CommitInfoProvider commitInfoProvider;
    private final JiraConnector jiraConnector;
    private final IssueCategorizer issueCategorizer;
    private final VersionInfoProvider versionInfoProvider;
    private final JiraUtils jiraUtils;
    private final CommitMessageParser commitMessageParser;
    private final SCMFacade.Response gitInfo;
    private final Configuration configuration;
    public final ImmutableSet<String> labelsToSkip;

    private static final String REQUIREMENTS_VA_ID_CUSTOMFIELD_10803 = "customfield_10803";
    private static final String DEFECT_ID_CUSTOMFIELD_11500 = "customfield_11500";

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
        this.labelsToSkip = configuration.getLabelsToSkipSet();
    }

    public ReleaseNotesModel get(final boolean clientFacing) {
        Iterable<CommitWithParsedInfo> commitsWithParsedInfo = generateCommitsWithParsedInfo(commitInfoProvider.getCommits());
        Map<String, Set<String>> prMap = generatePullRequestMap(commitsWithParsedInfo);
        ImmutableSet<String> issueIds = generateIssueIds(commitsWithParsedInfo);
        Map<JiraIssueSearchType, String> errors = generateErrorMessageMap();
		ImmutableMap<String, Issue> combinedJiraIssues = generateCombinedJiraIssues(issueIds, errors);
        Map<String, Issue> combinedJiraIssuesNoSubtasks = filterOutSubtasks(combinedJiraIssues);
        Map<String, Issue> combinedJiraIssuesClientFacing = combinedJiraIssuesNoSubtasks;
        if (clientFacing) {
            combinedJiraIssuesClientFacing = filterForClientFacing(combinedJiraIssuesNoSubtasks, configuration.getClientFacingFilterSet());
        }
        // TODO Refactor return type
        Map<String, List<Issue>> jiraIssuesByType = issueCategorizer.byType(combinedJiraIssuesClientFacing.values());
		ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> issueModelsByType = getIssuesByType(jiraIssuesByType, prMap);
		ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> validatedIssueModelsByType = validateIssuesByType(issueModelsByType);
        ImmutableSet<ReportJiraIssueModel> knownIssues = generateKnownIssues(configuration.getKnownIssues(), errors);
        ImmutableSet<ReportJiraIssueModel> knownIssuesWithFixedIssuesRemoved = removeFixedIssuesFromKnownIssues(knownIssues, combinedJiraIssuesNoSubtasks);
        		
		ReleaseNotesModel model = new ReleaseNotesModel(getIssueTypes(jiraIssuesByType), validatedIssueModelsByType, 
        		getCommitsWithDefectIds(commitsWithParsedInfo, combinedJiraIssues), knownIssuesWithFixedIssuesRemoved, versionInfoProvider.getReleaseVersion(),
                gitInfo.commitTag1, gitInfo.commitTag2, gitInfo.commits.size(), gitInfo.gitBranch, configuration, errors);

        return model;
    }

    private ImmutableSet<ReportJiraIssueModel> removeFixedIssuesFromKnownIssues(final ImmutableSet<ReportJiraIssueModel> knownIssues, 
			final Map<String, Issue> combinedJiraIssuesNoSubtasks) {
		Map<String, ReportJiraIssueModel> knownIssuesMap = new HashMap<String, ReportJiraIssueModel>();

		for (ReportJiraIssueModel model : knownIssues) {
			try {
				if (!combinedJiraIssuesNoSubtasks.containsKey(model.getIssue().getKey())) {
					knownIssuesMap.put(model.getIssue().getKey(), model);
				} else {
					logger.info("{} is fixed, removing from known issues", model.getIssue().getKey());				
				}
			} catch (Exception e) {
				logger.warn("{}", e.getMessage(), e);
			}
		}
		return ImmutableSet.copyOf(knownIssuesMap.values());
	}

	private Map<JiraIssueSearchType, String> generateErrorMessageMap() {
		Map<JiraIssueSearchType, String> errors = new HashMap<JiraIssueSearchType, String>();
        errors.put(JiraIssueSearchType.FIX_VERSION, "");
        errors.put(JiraIssueSearchType.GENERIC, "");
        errors.put(JiraIssueSearchType.KNOWN_ISSUE, "");
		return errors;
	}

	private ImmutableSet<ReportJiraIssueModel> generateKnownIssues(final String knownIssues, final Map<JiraIssueSearchType, String> errors) {
		ImmutableMap<String, Issue> knownIssuesMap = jiraConnector.getKnownIssuesByJql(configuration.getKnownIssues(), errors);
        ImmutableSet<ReportJiraIssueModel> knownIssuesModelSet = generateKnownIssuesModelSet(knownIssuesMap);
        return removeIssuesWithSkipLabels(knownIssuesModelSet);
	}
	
    private ImmutableMap<String, Issue> removeIssuesWithSkipLabels(ImmutableMap<String, Issue> jiraIssuesModelMap) {
		Map<String, Issue> jiraIssuesWithSkipLabelsRemoved = Maps.filterValues(jiraIssuesModelMap, new Predicate<Issue>() {

            @Override
            public boolean apply(Issue issue) {
            	for (String label : issue.getLabels()) {
            		if (labelsToSkip.contains(label)) {
            			logger.info("Issue with label [{}] removed. [{}] - [{}]", label, issue.getKey(), issue.getSelf());
            			return false;
            		}
            	}
                return true;
            }
        });
		return ImmutableMap.copyOf(jiraIssuesWithSkipLabelsRemoved);
    }
    	
	private ImmutableSet<ReportJiraIssueModel> removeIssuesWithSkipLabels(final ImmutableSet<ReportJiraIssueModel> jiraIssuesModelSet) {
		ImmutableSet<ReportJiraIssueModel> jiraIssuesWithSkipLabelsRemoved = FluentIterable
                .from(jiraIssuesModelSet)
                .filter(new Predicate<ReportJiraIssueModel>() {
                    @Override
                    public boolean apply(final ReportJiraIssueModel issueModel) {
                    	for (String label : issueModel.getLabels()) {
                    		if (labelsToSkip.contains(label)) {
                    			logger.info("Issue with label [{}] removed. [{}] - [{}]", label, issueModel.getIssue().getKey(), issueModel.getIssue().getSelf());
                    			return false;
                    		}
                    	}
                        return true;
                    }
                })
                .toSet();
		return jiraIssuesWithSkipLabelsRemoved;
	}

	private ImmutableSet<ReportJiraIssueModel> generateKnownIssuesModelSet(final ImmutableMap<String, Issue> knownIssuesMap) {
		List<Issue> knownIssuesList = knownIssuesMap.values().asList();
		
		
        return ImmutableSet.copyOf(Iterables.transform(knownIssuesList, new Function<Issue, ReportJiraIssueModel>() {
            @Override
            public ReportJiraIssueModel apply(Issue issue) {
                return toJiraIssueModel(issue, null);
            }
        }));
	}

	private Iterable<CommitWithParsedInfo> generateCommitsWithParsedInfo(final ImmutableSet<Commit> commits) {
		Iterable<CommitWithParsedInfo> commitsWithParsedInfo = FluentIterable.from(commits)
                .transform(new Function<Commit, CommitWithParsedInfo>() {

                    @Override
                    public CommitWithParsedInfo apply(final Commit commit) {
                        return new CommitWithParsedInfo(commit,
                                commitMessageParser.getJiraKeys(commit.getMessage()),
                                commitMessageParser.getDefectIds(commit.getMessage()),
                                commitMessageParser.getPullRequestId(commit.getMessage()));
                    }
                });
		return commitsWithParsedInfo;
	}

	private ImmutableSet<String> generateIssueIds(final Iterable<CommitWithParsedInfo> commitsWithParsedInfo) {
		ImmutableSet<String> issueIds = FluentIterable.from(commitsWithParsedInfo)
                .transformAndConcat(new Function<CommitWithParsedInfo, Iterable<String>>() {

                    @Override
                    public Iterable<String> apply(final CommitWithParsedInfo commitMessage) {
                        return commitMessage.getJiraIssueKeys();
                    }
                }).toSet();
		return issueIds;
	}

	private Map<String, Set<String>> generatePullRequestMap(final Iterable<CommitWithParsedInfo> commitsWithParsedInfo) {
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

	private Map<String, Issue> filterOutSubtasks(final ImmutableMap<String, Issue> combinedJiraIssues) {
		Map<String, Issue> combinedJiraIssuesNoSubtasks = Maps.filterValues(combinedJiraIssues, new Predicate<Issue>() {

            @Override
            public boolean apply(Issue issue) {
                return !issue.getIssueType().isSubtask();
            }
        });
		return combinedJiraIssuesNoSubtasks;
	}
	
    private Map<String, Issue> filterForClientFacing(Map<String, Issue> combinedJiraIssuesNoSubtasks, final Set issueFilterIds) {
        Map<String, Issue> combinedJiraIssuesClientFacing = Maps.filterValues(combinedJiraIssuesNoSubtasks, new Predicate<Issue>() {

            @Override
            public boolean apply(Issue issue) {
                Iterable<IssueField> fields = issue.getFields();
                for (IssueField field : fields) {
                    if (field.getName() != null && issueFilterIds.contains(field.getName())) {
                        if (field.getValue() != null) {
                            return true;
                        }
                    }
                }
                return false;
            }
        });
        return combinedJiraIssuesClientFacing;
    }	
    
    public ImmutableMap<String, Issue> generateCombinedJiraIssues(final ImmutableSet<String> issueIds, final Map<JiraIssueSearchType, String> errors) {
        ImmutableMap<String, Issue> fixVersionIssues = jiraConnector.getIssuesByFixVersions(configuration.getFixVersionsSet(), errors);
        ImmutableMap<String, Issue> jiraIssues = jiraConnector.getIssuesIncludeParents(issueIds, errors);
        Map<String, Issue> temp = new HashMap<String, Issue>();
    	temp.putAll(fixVersionIssues);
    	temp.putAll(jiraIssues);
        return removeIssuesWithSkipLabels(ImmutableMap.copyOf(temp));
	}

	private ImmutableSet<ReportCommitModel> getCommitsWithDefectIds(final Iterable<CommitWithParsedInfo> commitMessages,
                                                                    final ImmutableMap<String, Issue> jiraIssues) {

        return FluentIterable.from(commitMessages).filter(new Predicate<CommitWithParsedInfo>() {

            @Override
            public boolean apply(final CommitWithParsedInfo commitWithParsedInfo) {
                ImmutableSet<String> defectIds = commitWithParsedInfo.getDefectIds();
                return !defectIds.isEmpty();
            }
        }).transform(new Function<CommitWithParsedInfo, ReportCommitModel>() {

            @Override
            public ReportCommitModel apply(final CommitWithParsedInfo commitWithParsedInfo) {
                return toCommitModel(commitWithParsedInfo);
            }
        }).toSet();
    }

    private ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> getIssuesByType(
    		final Map<String, List<Issue>> jiraIssuesByType, final Map<String,Set<String>> pullRquestsMap) {

    	Map<String, ImmutableSet<ReportJiraIssueModel>> transformedMap = Maps
                .transformValues(jiraIssuesByType, new Function<List<Issue>, ImmutableSet<ReportJiraIssueModel>>() {

                    @Override
                    public ImmutableSet<ReportJiraIssueModel> apply(final List<Issue> issues) {
                        return ImmutableSet
                                .copyOf(Iterables.transform(issues, new Function<Issue, ReportJiraIssueModel>() {

                                    @Override
                                    public ReportJiraIssueModel apply(final Issue issue) {

                                        if(pullRquestsMap.containsKey(issue.getKey())) {
                                            return toJiraIssueModel(issue, pullRquestsMap.get(issue.getKey()));
                                        }
                                        return toJiraIssueModel(issue, null);
                                    }
                                }));
                    }
                });

        return ImmutableMap.copyOf(transformedMap);
    }
    
	private ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> validateIssuesByType(final ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> issueModelsByType) {

		logger.info("Validating issues");
		Map<String, ImmutableSet<ReportJiraIssueModel>> validatedIssueModelsByType = new HashMap<String, ImmutableSet<ReportJiraIssueModel>>();
		Set<ReportJiraIssueModel> invalidStateIssueModels = new HashSet<ReportJiraIssueModel>();
        Set<ReportJiraIssueModel> invalidFixVersionsIssueModels = new HashSet<ReportJiraIssueModel>();
		
		for (Map.Entry<String, ImmutableSet<ReportJiraIssueModel>> typeModelSet : issueModelsByType.entrySet()) {
			Set<ReportJiraIssueModel> validIssueModels = new HashSet<ReportJiraIssueModel>();
			for (ReportJiraIssueModel model : typeModelSet.getValue()) {
			    boolean isFixVersionValid = validateFixVersions(model);
			    boolean isStateValid = validateState(model);
			    if (!isFixVersionValid) {
			        invalidFixVersionsIssueModels.add(model);
			    }
			    if (!isStateValid) {
			        invalidStateIssueModels.add(model);			        
			    }
			    if (isFixVersionValid && isStateValid) {
                    validIssueModels.add(model);
			    }
		        logger.info("{} is {}", model.getIssue().getKey(), ((isFixVersionValid && isStateValid) ? "valid" : "invalid"));
			}
			validatedIssueModelsByType.put(typeModelSet.getKey(), ImmutableSet.copyOf(validIssueModels));
		}
		if (invalidStateIssueModels!=null && !invalidStateIssueModels.isEmpty()) {
			validatedIssueModelsByType.put(JiraIssueSearchType.INVALID_STATE.title(), ImmutableSet.copyOf(invalidStateIssueModels));			
		}
        if (invalidFixVersionsIssueModels!=null && !invalidFixVersionsIssueModels.isEmpty()) {
            validatedIssueModelsByType.put(JiraIssueSearchType.INVALID_FIX_VERSION.title(), ImmutableSet.copyOf(invalidFixVersionsIssueModels));           
        }
		return ImmutableMap.copyOf(validatedIssueModelsByType);
	}

    /**
     * validate that at least one FixVerion in the Jira Issue is contained in the specified list of FixVersions
     * @param model
     * @return
     */
	private boolean validateFixVersions(ReportJiraIssueModel model) {;
		ImmutableSet<String> searchFixVersions = configuration.getFixVersionsSet();
		if (searchFixVersions.isEmpty()) {
		    return true;
		}

		ImmutableSet<String> modelFixVersions = CollectionUtils.stringToImmutableSet(",", model.getFixVersions());
		if (searchFixVersions.isEmpty() && modelFixVersions.isEmpty()) {
			return true;
		}

        boolean isFixVersionInConfig = false;
		for (String modelFixVersionString : modelFixVersions) {
			if (searchFixVersions.contains(modelFixVersionString)) {
				isFixVersionInConfig = true;
			}
		}
		if (isFixVersionInConfig) {
	        logger.debug("{} is valid, at least one FixVersions for the Jira Issue is included in conguration jira.fixVersions", model.getIssue().getKey());		    
		} else {
            logger.debug("{} is invalid, no FixVersions for the Jira Issue are included in conguration jira.fixVersions", model.getIssue().getKey());           
		}
		return isFixVersionInConfig;
	}

	private boolean validateState(ReportJiraIssueModel model) {
		if (model == null) {
			return false;
		}
		boolean isValidState = model.getIsStatusOk();
		if (!isValidState) {
			logger.debug("{} is invalid, status '{}' not included in conguration jira.completedStatuses '{}'", model.getIssue().getKey(), model.getStatus(), configuration.getCompletedStatuses());			
		}
		return isValidState;
	}

	private ImmutableSet<String> getIssueTypes(final Map<String, List<Issue>> jiraIssuesByType) {
		Set<String> issuesTypes = new HashSet<String>();
		issuesTypes.add(JiraIssueSearchType.INVALID_FIX_VERSION.title());
        issuesTypes.add(JiraIssueSearchType.INVALID_STATE.title());
		issuesTypes.addAll(jiraIssuesByType.keySet());
        return ImmutableSet.copyOf(issuesTypes);
    }

    public static String concatNotNullNotEmpty(final String separator, final String... ss) {
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

    private ReportJiraIssueModel toJiraIssueModel(final Issue issue, final Set<String> pullRequestIds) {
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
            public String apply(final Version version) {
                return version.getName();
            }
        });

        final String status = issue.getStatus().getName();
        final boolean isStatusOk = FluentIterable.from(Arrays.asList(configuration.getCompletedStatuses())).anyMatch(new Predicate<String>() {
            @Override
            public boolean apply(final String s) {
                try {
                    return s.trim().equalsIgnoreCase(status.trim());
                } catch (NullPointerException e) {
                    logger.warn("{}", e.getMessage(), e);
                    return false;
                }
            }
        });

        return new ReportJiraIssueModel(issue, id, url, fixedInVersion, releaseNotes, fixVersions, impact, detailsOfChange, pullRequestIds, isStatusOk, status);
    }

    private ReportCommitModel toCommitModel(final CommitWithParsedInfo commitWithParsedInfo) {
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
                             final ImmutableSet<String> defectIds, final String pullRequestId) {
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
