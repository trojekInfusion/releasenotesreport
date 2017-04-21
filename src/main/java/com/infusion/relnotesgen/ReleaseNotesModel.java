package com.infusion.relnotesgen;

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.infusion.relnotesgen.util.JiraIssueSearchType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReleaseNotesModel {
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

    public ReleaseNotesModel(final ImmutableSet<String> issueCategoryNames, final ImmutableMap<String, ImmutableSet<ReportJiraIssueModel>> issuesByCategory,
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

        uniqueDefects = generateUniqueDefects(issuesByCategory, commitsWithDefectIds);
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
                }).append(FluentIterable.from(commitsWithDefectIds)
	                    .transformAndConcat(new Function<ReportCommitModel, ImmutableSet<String>>() {
	                        @Override
	                        public ImmutableSet<String> apply(ReportCommitModel reportCommitModel) {
	                            return reportCommitModel.getDefectIds();
	                        }
	                    })
                )
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

    public ImmutableSet<ReportJiraIssueModel> getIssuesByCategoryName(final String categoryName) {
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

}
