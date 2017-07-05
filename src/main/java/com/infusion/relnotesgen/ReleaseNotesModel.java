package com.infusion.relnotesgen;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.infusion.relnotesgen.ReportCommitModel.ReportCommitModelBuilder;
import com.infusion.relnotesgen.util.JiraIssueSearchType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReleaseNotesModel {
    public static final String LOGGER_NAME = "com.infusion.relnotesgen.log.ReleaseNotesLogger";
    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private static final String URL_QUOTE = "%22";
	private static final String URL_COMMA = "%2C";
	private static final String URL_SPACE = "%20";
	private static final String ISSUES_JQL_URL = "/issues/?jql=";
	private static final String JQL_BY_ID_URL = "id%20in%20(";
	private static final String URL_COMMA_AND_SPACE = URL_COMMA + URL_SPACE;
	
	private final ImmutableSet<String> issueCategoryNames;
    private final ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> issuesByCategory;
    private final ImmutableSet<ReportCommitModel> commitsWithDefectIds;
    private final String releaseVersion;
    private final SCMFacade.GitCommitTag commitTag1;
    private final SCMFacade.GitCommitTag commitTag2;
    private final int commitsCount;
    private final String gitBranch;
    private final Configuration configuration;
    private final ImmutableSortedSet<String> uniqueDefects;
    private final String jqlLink;
    private final String knownIssuesJqlLink;
    private final ImmutableSet<String> fixVersions;
    private final ImmutableSet<ReportJiraIssueModel> knownIssues;
    private final Map<JiraIssueSearchType, String> errors;

    private ReleaseNotesModel(final ImmutableSet<String> issueCategoryNames, final ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> issuesByCategory,
                             final ImmutableSet<ReportCommitModel> commitsWithDefectIds, final ImmutableSet<ReportJiraIssueModel> knownIssues, 
                             final String releaseVersion, final SCMFacade.GitCommitTag commitTag1, final SCMFacade.GitCommitTag commitTag2, final int commitsCount,
                             final String gitBranch, Configuration configuration, final Map<JiraIssueSearchType,String> errors) {
        this.issueCategoryNames = issueCategoryNames;
        this.issuesByCategory = issuesByCategory;
        this.commitsWithDefectIds = commitsWithDefectIds;
        this.releaseVersion = releaseVersion;
        this.commitTag1 = commitTag1;
        this.commitTag2 = commitTag2;
        this.commitsCount = commitsCount;
        this.gitBranch = gitBranch;
        this.configuration = configuration;
        this.fixVersions = configuration.getFixVersionsSet();
        this.knownIssues = knownIssues;
        this.errors = errors;

        uniqueDefects = generateUniqueDefects(generateValidIssuesByCategory(issuesByCategory), commitsWithDefectIds);
        ImmutableSortedSet<String> uniqueJiras = generateUniqueJiras(issuesByCategory);

        jqlLink = generateUrlEncodedJqlString(generateJqlUrl(uniqueJiras));
        knownIssuesJqlLink = generateUrlEncodedJqlString(generateJqlUrl(configuration.getKnownIssues()));
    }

    private ImmutableSortedSet<String> generateUniqueJiras(final ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> issuesByCategory) {
		return FluentIterable
                .from(issuesByCategory.values())
                .transformAndConcat(new Function<ImmutableSet<ReportJiraIssueModel>, List<String>>() {

                    @Override
                    public List<String> apply(final ImmutableSet<ReportJiraIssueModel> reportJiraIssueModels) {
                        return FluentIterable.from(reportJiraIssueModels)
                            .transform(new Function<ReportJiraIssueModel, String>() {

                                @Override
                                public String apply(final ReportJiraIssueModel reportJiraIssueModel) {
                                    return reportJiraIssueModel.getIssue().getKey();
                                }
                            }).toList();
                    }
                })
                .toSortedSet(new Comparator<String>() {
                     @Override
                     public int compare(String o1, String o2) {
                         return o1.compareTo(o2);
                     }
                 }

                );
	}

    private ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> generateValidIssuesByCategory(
            ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> issuesByCategory) {
        Map<String, ImmutableSet<ReportJiraIssueModel>> validIssuesByCategoryTemp = new HashMap<String, ImmutableSet<ReportJiraIssueModel>>();
        validIssuesByCategoryTemp.putAll(issuesByCategory);
        for (JiraIssueSearchType curr : JiraIssueSearchType.values()) {
            if (!curr.isValid()) {
                validIssuesByCategoryTemp.remove(curr.title());
            }
        }
        return ImmutableMap.copyOf(validIssuesByCategoryTemp);
    }

	private ImmutableSortedSet<String> generateUniqueDefects(final ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> issuesByCategory,
			final ImmutableSet<ReportCommitModel> commitsWithDefectIds) {
		return FluentIterable
                .from(issuesByCategory.values())
                .transformAndConcat(new Function<ImmutableSet<ReportJiraIssueModel>, List<String>>() {
                    @Override
                    public List<String> apply(ImmutableSet<ReportJiraIssueModel> reportJiraIssueModels) {
                        return FluentIterable.from(reportJiraIssueModels)
                            .transformAndConcat(new Function<ReportJiraIssueModel, List<String>>() {
                                @Override
                                public List<String> apply(ReportJiraIssueModel reportJiraIssueModel) {
                                    return new ArrayList<>(
                                            Arrays.asList(reportJiraIssueModel.getDefectIds()));
                                }
                            }).toList();
                    }
                })
                .transform(new Function<String, String>() {
                    @Override
                    public String apply(String s) {
                        return s.toUpperCase().replace("EFECT", "efect");
                    }
                })
                .toSortedSet(new Comparator<String>() {
                     @Override
                     public int compare(String o1, String o2) {
                         return o1.compareTo(o2);
                     }
                 }
                );
	}

	private String generateUrlEncodedJqlString(final String jqlString) {
		StringBuilder sb = new StringBuilder(configuration.getJiraUrl());
		sb.append(ISSUES_JQL_URL);
		sb.append(jqlString);
        return sb.toString();
	}

	private String generateJqlUrl(final String knownIssues) {
		if (knownIssues==null || knownIssues.isEmpty()) {
			return "";
		}
		return knownIssues.replaceAll(",", URL_COMMA).replaceAll(" ", URL_SPACE).replaceAll("\"", URL_QUOTE);
	}

	private String generateJqlUrl(final ImmutableSortedSet<String> uniqueJiras) {
		StringBuilder sb = new StringBuilder(JQL_BY_ID_URL);
		for (String s : uniqueJiras) {
            sb.append(s);
            sb.append(URL_COMMA_AND_SPACE);
        }
        sb.replace(sb.length()-URL_COMMA_AND_SPACE.length(),sb.length(),"");
        sb.append(")");
        return sb.toString();
	}

	public ImmutableSet<String> getIssueCategoryNames() {
        return issueCategoryNames;
    }
	
    public boolean categoryNameIsInvalid(final String categoryName) {
        for (JiraIssueSearchType curr : JiraIssueSearchType.values()) {
            if (curr.title().equals(categoryName) && !curr.isValid()) {
                return true;
            }
        }
        return false;
    }

	public List<String> getIssueCategoryNamesList() {
		List<String> sortedList = new ArrayList<String>();
		for (String categoryName : issueCategoryNames) {
            if (JiraIssueSearchType.INVALID_STATE.title().equals(categoryName)) {
                sortedList.add(0, categoryName);
            } else if (JiraIssueSearchType.INVALID_FIX_VERSION.title().equals(categoryName)) {
				sortedList.add(0, categoryName);
			} else {
				sortedList.add(categoryName);
			}
		}
        return sortedList;
    }

    public int getTotalInvalidIssueCount() {
        int invalidCount = 0;
        for (JiraIssueSearchType curr : JiraIssueSearchType.values()) {
            if (!curr.isValid()) {
                invalidCount += getIssueCountByCategoryName(curr.title());
            }
        }
        return invalidCount;
    }
	
	
    public int getIssueCountByCategoryName(final String categoryName) {
        try {
            return getIssuesByCategoryName(categoryName).size();
        } catch (Exception e) {
            logger.warn("{}", e.getMessage(), e);
            return 0;
        }
    }

    public String getInvalidByStatusCategoryName() {
        return JiraIssueSearchType.INVALID_STATE.title();
    }

    public String getInvalidByFixVersionCategoryName() {
        return JiraIssueSearchType.INVALID_FIX_VERSION.title();
    }

    public ImmutableSet<ReportJiraIssueModel> getIssuesByCategoryName(final String categoryName) {
    	if (issuesByCategory.containsKey(categoryName)) {
    		return issuesByCategory.get(categoryName);
    	} else {
    		return ImmutableSet.copyOf(new HashSet<ReportJiraIssueModel>());
    	}
    }
    
    public ImmutableSet<String> getInvalidStateIssueStatuses() {
        if (issuesByCategory.containsKey(getInvalidByStatusCategoryName())) {
            return retrieveInvalidIssuesStatusNames(issuesByCategory.get(getInvalidByStatusCategoryName()));
        } else {
            return ImmutableSet.copyOf(new HashSet<String>());
        }
    }

    public ImmutableSet<ReportJiraIssueModel> getInvalidStateIssuesByStatus(final String statusName) {
        if (issuesByCategory.containsKey(getInvalidByStatusCategoryName())) {
            return retrieveInvalidIssuesByStatusName(issuesByCategory.get(getInvalidByStatusCategoryName()), statusName);
        } else {
            return ImmutableSet.copyOf(new HashSet<ReportJiraIssueModel>());
        }
    }

    private ImmutableSet<ReportJiraIssueModel> retrieveInvalidIssuesByStatusName(
            ImmutableSet<ReportJiraIssueModel> invalidIssues, String statusName) {
        Set<ReportJiraIssueModel> invalidIssuesByStatusName = new HashSet<ReportJiraIssueModel>();
        for (ReportJiraIssueModel curr : invalidIssues) {
            if (statusName.equalsIgnoreCase(curr.getStatus())) {
                invalidIssuesByStatusName.add(curr);
            }
        }
        return ImmutableSet.copyOf(invalidIssuesByStatusName);
    }

    private ImmutableSet<String> retrieveInvalidIssuesStatusNames(ImmutableSet<ReportJiraIssueModel> invalidIssues) {
        Set<String> statusNames =  new HashSet<String>();
        for (ReportJiraIssueModel curr : invalidIssues) {
            statusNames.add(curr.getStatus());
        }
        return ImmutableSet.copyOf(statusNames);
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

    public ImmutableSortedSet<String> getUniqueDefects() {
        return uniqueDefects;
    }

    public String getJqlLink() {
    	return jqlLink; 
    }

    public Configuration getConfiguration() { 
    	return configuration;
    }

	public ImmutableSet<String> getFixVersions() {
		return fixVersions;
	}

	public ImmutableSet<ReportJiraIssueModel> getKnownIssues() {
		return knownIssues;
	}

	public String getKnownIssuesJqlLink() {
		return knownIssuesJqlLink;
	}

	public Map<JiraIssueSearchType, String> getErrors() {
		return errors;
	}
	
	public String getKnownIssuesErrorMessage() {
		return errors.get(JiraIssueSearchType.KNOWN_ISSUE);
	}

	public String getFixVersionErrorMessage() {
		return errors.get(JiraIssueSearchType.FIX_VERSION);
	}

	public String getGenericErrorMessage() {
		return errors.get(JiraIssueSearchType.GENERIC);
	}

    public static class ReleaseNotesModelBuilder {

        private ImmutableSet<String> nestedIssueCategoryNames;
        private ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> nestedIssuesByCategory;
        private ImmutableSet<ReportCommitModel> nestedCommitsWithDefectIds;
        private ImmutableSet<ReportJiraIssueModel> nestedKnownIssues;
        private String nestedReleaseVersion;
        private SCMFacade.GitCommitTag nestedCommitTag1;
        private SCMFacade.GitCommitTag nestedCommitTag2;
        private int nestedCommitsCount;
        private String nestedGitBranch;
        private Configuration nestedConfiguration;
        private Map<JiraIssueSearchType,String> nestedErrors;
        
        public ReleaseNotesModelBuilder() {}
        
        public ReleaseNotesModelBuilder issueCategoryNames(final ImmutableSet<String> issueCategoryNames) {
            this.nestedIssueCategoryNames = issueCategoryNames;
            return this;
        }

        public ReleaseNotesModelBuilder issuesByCategory(final ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> issuesByCategory) {
            this.nestedIssuesByCategory = issuesByCategory;
            return this;
        }

        public ReleaseNotesModelBuilder commitsWithDefectIds(final ImmutableSet<ReportCommitModel> commitsWithDefectIds) {
            this.nestedCommitsWithDefectIds = commitsWithDefectIds;
            return this;
        }

        public ReleaseNotesModelBuilder knownIssues(final ImmutableSet<ReportJiraIssueModel> knownIssues) {
            this.nestedKnownIssues = knownIssues;
            return this;
        }

        public ReleaseNotesModelBuilder releaseVersion(final String releaseVersion) {
            this.nestedReleaseVersion = releaseVersion;
            return this;
        }

        public ReleaseNotesModelBuilder commitTag1(final SCMFacade.GitCommitTag commitTag1) {
            this.nestedCommitTag1 = commitTag1;
            return this;
        }

        public ReleaseNotesModelBuilder commitTag2(final SCMFacade.GitCommitTag commitTag2) {
            this.nestedCommitTag2 = commitTag2;
            return this;
        }

        public ReleaseNotesModelBuilder commitsCount(final int commitsCount) {
            this.nestedCommitsCount = commitsCount;
            return this;
        }

        public ReleaseNotesModelBuilder gitBranch(final String gitBranch) {
            this.nestedGitBranch = gitBranch;
            return this;
        }

        public ReleaseNotesModelBuilder configuration(final Configuration configuration) {
            this.nestedConfiguration = configuration;
            return this;
        }
        
        public ReleaseNotesModelBuilder errors(final Map<JiraIssueSearchType,String> errors) {
            this.nestedErrors = errors;
            return this;
        }

        public ReleaseNotesModel build() throws IllegalStateException {
            if (!isInitalizedProperly()) {
                throw new IllegalStateException("Required parameters were not initialized");
            }
            return new ReleaseNotesModel(nestedIssueCategoryNames, nestedIssuesByCategory, nestedCommitsWithDefectIds, 
                    nestedKnownIssues, nestedReleaseVersion, nestedCommitTag1, nestedCommitTag2, nestedCommitsCount, 
                    nestedGitBranch, nestedConfiguration, nestedErrors);
        }
        
        private boolean isInitalizedProperly() {
            if (nestedIssueCategoryNames==null || nestedIssuesByCategory==null || nestedCommitsWithDefectIds==null || 
                    nestedKnownIssues==null || nestedReleaseVersion==null || nestedCommitTag1==null || nestedCommitTag2==null 
                    || nestedGitBranch==null || nestedConfiguration==null || nestedErrors==null) {
                return false;
            }
            return true;
        }
    }
}
