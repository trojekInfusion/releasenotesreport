package com.infusion.relnotesgen;

import com.atlassian.jira.rest.client.domain.Issue;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.infusion.relnotesgen.Configuration.Element;
import com.infusion.relnotesgen.util.IssueCategorizer;
import com.infusion.relnotesgen.util.IssueCategorizerImpl;
import com.infusion.relnotesgen.util.JiraUtils;
import com.infusion.relnotesgen.util.JiraUtilsImpl;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Properties;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * @author trojek
 */
public class Main {

    private final static Logger logger = LoggerFactory.getLogger(Configuration.LOGGER_NAME);

    public static void main(final String[] args) throws IOException {
        generateReleaseNotes(args);
    }

    static File generateReleaseNotes(final String[] args) throws IOException {
        logger.info("Reading program parameters...");
        ProgramParameters programParameters = new ProgramParameters();
        new JCommander(programParameters, args);

        final Configuration configuration = readConfiguration(programParameters);
        logger.info("Build configuration: {}", configuration);

        // Get git log commits
        Authenticator authenticator = configuration.getGitUrl().toLowerCase().startsWith("ssh://") ?
                new PublicKeyAuthenticator() :
                new UserCredentialsAuthenticator(configuration);
        SCMFacade gitFacade = new GitFacade(configuration, authenticator);

        // Components
        final SCMFacade.Response gitInfo = getGitInfo(programParameters, gitFacade);
        final JiraIssueDao jiraIssueDao = new JiraIssueDao(configuration);

        // Implementations
        CommitInfoProvider commitInfoProvider = new CommitInfoProvider() {
            @Override
            public ImmutableSet<Commit> getCommits() {
                return FluentIterable.from(gitInfo.commits).toImmutableSet();
            }
        };
        JiraConnector jiraConnector = new JiraConnector() {
            @Override
            public ImmutableMap<String, Issue> getIssuesIncludeParents(ImmutableSet<String> issueIds) {
                return jiraIssueDao.findAllIssuesAsMap(issueIds);
            }

            @Override
            public String getIssueUrl(Issue issue) {
                try {
                    URL baseURL = new URL(configuration.getJiraUrl());
                    return new URL(baseURL, MessageFormat.format("/browse/{0}", issue.getKey())).toString();
                } catch (MalformedURLException e) {
                    return "#ERROR";
                }
            }
        };
        VersionInfoProvider versionInfoProvider = new VersionInfoProvider() {
            @Override
            public String getReleaseVersion() {
                return defaultIfEmpty(configuration.getReleaseVersion(), gitInfo.version);
            }
        };
        IssueCategorizer issueCategorizer = new IssueCategorizerImpl(configuration);
        JiraUtils jiraUtils = new JiraUtilsImpl();
        CommitMessageParser commitMessageParser = new CommitMessageParserImpl(configuration);

        // Generate report model
        ReleaseNotesModelFactory factory = new ReleaseNotesModelFactory(
            commitInfoProvider,
            jiraConnector,
            issueCategorizer,
            versionInfoProvider,
            jiraUtils,
            commitMessageParser);

        ReleaseNotesModel reportModel = factory.get();

        // Generate report
        ReleaseNotesReportGenerator generator = new ReleaseNotesReportGenerator(configuration);
        File reportFile = new File(
                getReportDirectory(configuration),
                versionInfoProvider.getReleaseVersion().replace(".", "_") + ".html");

        logger.info("Creating report in {}", reportFile.getPath());

        try (Writer fileWriter = new FileWriter(reportFile)) {
            generator.generate(reportModel, fileWriter);
        }

        logger.info("Generation of report is finished.");

        return reportFile;
    }

    private static SCMFacade.Response getGitInfo(final ProgramParameters programParameters, final SCMFacade gitFacade) {
        if (isNotEmpty(programParameters.tag1) || isNotEmpty(programParameters.tag2)) {
            logger.info("Reading scm history by tags '{}' and '{}'", programParameters.tag1, programParameters.tag2);
            return gitFacade.readByTag(programParameters.tag1, programParameters.tag2);
        } else if (isNotEmpty(programParameters.commitId1) || isNotEmpty(programParameters.commitId2)) {
            logger.info("Reading scm history by commit ids '{}' and '{}'", programParameters.commitId1,
                    programParameters.commitId2);
            return gitFacade.readByCommit(programParameters.commitId1, programParameters.commitId2);
        } else {
            logger.info("No commit id or tag parameter provided, reading scm history by two latests tags.");
            return gitFacade.readLatestReleasedVersion();
        }
    }

    private static File getReportDirectory(final Configuration configuration) throws IOException {
        return StringUtils.isEmpty(configuration.getReportDirectory()) ?
                Files.createTempDirectory("ReportDirectory").toFile() :
                new File(configuration.getReportDirectory());
    }

    private static Configuration readConfiguration(final ProgramParameters programParameters)
            throws IOException {
        String path = programParameters.configurationFilePath;
        Properties properties = new Properties();

        if (StringUtils.isNotEmpty(path)) {
            logger.info("Using configuration file under {}", path);
            properties.load(new FileInputStream(new File(path)));
        } else {
            logger.info(
                    "Configuration file path parameter is note defined using only program parameters to biuld configuration");
        }

        return new Configuration(properties, programParameters);
    }

    public static class ProgramParameters {

        @Parameter(names = { "-configurationFilePath",
                "-conf" }, description = "Path to configuration file") private String configurationFilePath;

        @Parameter(names = { "-commitId1" }, description = "Commit 1 hash delimeter") private String commitId1;

        @Parameter(names = { "-commitId2" }, description = "Commit 2 hash delimeter") private String commitId2;

        @Parameter(names = { "-tag1" }, description = "Tag 1 delimeter") private String tag1;

        @Parameter(names = { "-tag2" }, description = "Tag 2 delimeter") private String tag2;

        @Parameter(names = {
                "-pushReleaseNotes" }, description = "Perform push of release notes to remote repo") private boolean pushReleaseNotes = false;

        @Element(Configuration.GIT_DIRECTORY)
        @Parameter(names = { "-gitDirectory" })
        private String gitDirectory;

        @Element(Configuration.GIT_BRANCH)
        @Parameter(names = { "-gitBranch" })
        private String gitBranch;

        @Element(Configuration.GIT_URL)
        @Parameter(names = { "-gitUrl" })
        private String gitUrl;

        @Element(Configuration.GIT_USERNAME)
        @Parameter(names = { "-gitUsername" })
        private String gitUsername;

        @Element(Configuration.GIT_PASSWORD)
        @Parameter(names = { "-gitPassword" })
        private String gitPassword;

        @Element(Configuration.GIT_COMMITTER_NAME)
        @Parameter(names = { "-gitCommitterName" })
        private String gitCommiterName;

        @Element(Configuration.GIT_COMMITTER_MAIL)
        @Parameter(names = { "-gitCommitterMail" })
        private String gitCommitterMail;

        @Element(Configuration.GIT_COMMITMESSAGE_VALIDATIONOMMITER)
        @Parameter(names = { "-gitCommitMessageValidationOmmiter" })
        private String gitCommitMessageValidationOmmiter;

        @Element(Configuration.JIRA_URL)
        @Parameter(names = { "-jiraUrl" })
        private String jiraUrl;

        @Element(Configuration.JIRA_USERNAME)
        @Parameter(names = { "-jiraUsername" })
        private String jiraUsername;

        @Element(Configuration.JIRA_PASSWORD)
        @Parameter(names = { "-jiraPassword" })
        private String jiraPassword;

        @Element(Configuration.JIRA_ISSUEPATTERN)
        @Parameter(names = { "-jiraIssuePattern" })
        private String jiraIssuePattern;

        @Element(Configuration.ISSUE_FILTERBY_COMPONENT)
        @Parameter(names = { "-issueFilterByComponent" })
        private String issueFilterByComponent;

        @Element(Configuration.ISSUE_FILTERBY_TYPE)
        @Parameter(names = { "-issueFilterByType" })
        private String issueFilterByType;

        @Element(Configuration.ISSUE_FILTERBY_LABEL)
        @Parameter(names = { "-issueFilterByLabel" })
        private String issueFilterByLabel;

        @Element(Configuration.ISSUE_FILTERBY_STATUS)
        @Parameter(names = { "-issueFilterByStatus" })
        private String issueFilterByStatus;

        @Element(Configuration.ISSUE_SORT_TYPE)
        @Parameter(names = { "-issueSortType" })
        private String issueSortType;

        @Element(Configuration.ISSUE_SORT_PRIORITY)
        @Parameter(names = { "-issueSortPriority" })
        private String issueSortPriority;

        @Element(Configuration.REPORT_DIRECTORY)
        @Parameter(names = { "-reportDirectory" })
        private String reportDirectory;

        @Element(Configuration.REPORT_TEMPLATE)
        @Parameter(names = { "-reportTemplate" })
        private String reportTemplate;

        @Element(Configuration.RELEASE_VERSION)
        @Parameter(names = { "-releaseVersion" })
        private String releaseVersion;
    }
}
