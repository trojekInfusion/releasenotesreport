package com.infusion.relnotesgen;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.rest.client.domain.Issue;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.infusion.relnotesgen.Configuration.Element;

/**
 * @author trojek
 *
 */
public class Main {

    private final static Logger logger = LoggerFactory.getLogger(Configuration.LOGGER_NAME);

    public static final String CONFIGURATION_FILE = "./configuration.properties";

    public static void main(final String[] args) throws IOException {
        logger.info("Reading program parameters...");
        ProgramParameters programParameters = new ProgramParameters();
        new JCommander(programParameters, args);

        Configuration configuration = readConfiguration(programParameters);
        logger.info("Build configuration: {}", configuration);

        //1. Getting git log messages
        SCMFacade gitFacade = new GitFacade(configuration);
        SCMFacade.Response gitInfo = getGitInfo(programParameters, gitFacade);

        //2. Matching issue ids from git log
        Set<String> jiraIssueIds = new JiraIssueIdMatcher(configuration.getJiraIssuePattern()).findJiraIds(gitInfo.messages);

        //3. Quering jira for issues
        Collection<Issue> issues = new JiraIssueDao(configuration).findIssues(jiraIssueIds);

        //4. Creating report
        File report = createReport(configuration, gitInfo, issues);

        //5. Pushing release notes to repo
        if(programParameters.pushReleaseNotes) {
            logger.info("Pushing release notes to remote repository");
            gitFacade.pushReleaseNotes(report, gitInfo.version);
        }
        gitFacade.close();

        logger.info("Release notes generated under {}", report.getAbsolutePath());
    }

    private static SCMFacade.Response getGitInfo(final ProgramParameters programParameters, final SCMFacade gitFacade) {
        if(isNotEmpty(programParameters.tag1) || isNotEmpty(programParameters.tag2)) {
            logger.info("Reading scm history by tags '{}' and '{}'", programParameters.tag1, programParameters.tag2);
            return gitFacade.readByTag(programParameters.tag1, programParameters.tag2);
        } else if(isNotEmpty(programParameters.tag1) || isNotEmpty(programParameters.tag2)) {
            logger.info("Reading scm history by commit ids '{}' and '{}'", programParameters.commitId1, programParameters.commitId2);
            return gitFacade.readByCommit(programParameters.commitId1, programParameters.commitId2);
        } else {
            logger.info("No commit id or tag parameter provided, reading scm history by two latests tags.");
            return gitFacade.readLatestReleasedVersion();
        }
    }

    private static File createReport(final Configuration configuration, final SCMFacade.Response gitInfo, final Collection<Issue> issues)
            throws IOException {
        File reportDirectory = null;
        if(StringUtils.isEmpty(configuration.getReportDirectory())) {
            logger.info("No report directory defined, creating it under temp directory.");
            reportDirectory = Files.createTempDirectory("ReportDirectory").toFile();
        } else {
            logger.info("Creating report under defined directory in {}", configuration.getReportDirectory());
            reportDirectory = new File(configuration.getReportDirectory());
        }
        return new ReleaseNotesGenerator(configuration).generate(issues, reportDirectory, gitInfo.version);
    }

    private static Configuration readConfiguration(final ProgramParameters programParameters) throws IOException, FileNotFoundException {
        String path = programParameters.configurationFilePath;
        Properties properties = new Properties();

        if(StringUtils.isNotEmpty(path)) {
            logger.info("Using configuration file under {}", path);
            properties.load(new FileInputStream(new File(path)));
        } else {
            logger.info("Configuration file path parameter is note defined using only program parameters to biuld configuration");
        }

        return new Configuration(properties, programParameters);
    }

    public static class ProgramParameters {

        @Parameter(names = { "-configurationFilePath", "-conf" }, description = "Path to configuration file")
        private String configurationFilePath;

        @Parameter(names = { "-commitId1"}, description = "Commit 1 hash delimeter")
        private String commitId1;

        @Parameter(names = { "-commitId2"}, description = "Commit 2 hash delimeter")
        private String commitId2;

        @Parameter(names = { "-tag1"}, description = "Tag 1 delimeter")
        private String tag1;

        @Parameter(names = { "-tag2"}, description = "Tag 2 delimeter")
        private String tag2;

        @Parameter(names = { "-pushReleaseNotes"}, description = "Perform push of release notes to remote repo")
        private boolean pushReleaseNotes = false;

        @Element(Configuration.GIT_DIRECTORY)
        @Parameter(names = { "-gitDirectory"})
        private String gitDirectory;

        @Element(Configuration.GIT_BRANCH)
        @Parameter(names = { "-gitBranch"})
        private String gitBranch;

        @Element(Configuration.GIT_URL)
        @Parameter(names = { "-gitUrl"})
        private String gitUrl;

        @Element(Configuration.GIT_USERNAME)
        @Parameter(names = { "-gitUsername"})
        private String gitUsername;

        @Element(Configuration.GIT_PASSWORD)
        @Parameter(names = { "-gitPassword"})
        private String gitPassword;

        @Element(Configuration.GIT_COMMITTER_NAME)
        @Parameter(names = { "-gitCommitterName"})
        private String gitCommiterName;

        @Element(Configuration.GIT_COMMITTER_MAIL)
        @Parameter(names = { "-gitCommitterMail"})
        private String gitCommitterMail;

        @Element(Configuration.GIT_COMMITMESSAGE_VALIDATIONOMMITER)
        @Parameter(names = { "-gitCommitMessageValidationOmmiter"})
        private String gitCommitMessageValidationOmmiter;

        @Element(Configuration.JIRA_URL)
        @Parameter(names = { "-jiraUrl"})
        private String jiraUrl;

        @Element(Configuration.JIRA_USERNAME)
        @Parameter(names = { "-jiraUsername"})
        private String jiraUsername;

        @Element(Configuration.JIRA_PASSWORD)
        @Parameter(names = { "-jiraPassword"})
        private String jiraPassword;

        @Element(Configuration.JIRA_ISSUEPATTERN)
        @Parameter(names = { "-jiraIssuePattern"})
        private String jiraIssuePattern;

        @Element(Configuration.ISSUE_FILTERBY_COMPONENT)
        @Parameter(names = { "-issueFilterByComponent"})
        private String issueFilterByComponent;

        @Element(Configuration.ISSUE_FILTERBY_TYPE)
        @Parameter(names = { "-issueFilterByType"})
        private String issueFilterByType;

        @Element(Configuration.ISSUE_FILTERBY_LABEL)
        @Parameter(names = { "-issueFilterByLabel"})
        private String issueFilterByLabel;

        @Element(Configuration.ISSUE_FILTERBY_STATUS)
        @Parameter(names = { "-issueFilterByStatus"})
        private String issueFilterByStatus;

        @Element(Configuration.ISSUE_SORT_TYPE)
        @Parameter(names = { "-issueSortType"})
        private String issueSortType;

        @Element(Configuration.ISSUE_SORT_PRIORITY)
        @Parameter(names = { "-issueSortPriority"})
        private String issueSortPriority;

        @Element(Configuration.REPORT_DIRECTORY)
        @Parameter(names = { "-reportDirectory"})
        private String reportDirectory;

        @Element(Configuration.REPORT_TEMPLATE)
        @Parameter(names = { "-reportTemplate"})
        private String reportTemplate;
    }
}
