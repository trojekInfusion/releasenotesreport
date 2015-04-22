package com.infusion.relnotesgen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.rest.client.domain.Issue;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class Main {

    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static final String CONFIGURATION_FILE = "./configuration.properties";
    public static final String RELEASE_NOTES_FILE = "./release-notes.html";

    public static void main(final String[] args) throws IOException {
        logger.info("Reading program parameters...");
        ProgramParameters programParameters = new ProgramParameters();
        new JCommander(programParameters, args);
        logger.info("Using program parameters: {}", ReflectionToStringBuilder.toString(programParameters));

        Configuration configuration = readConfiguration(programParameters);
        logger.info("Read configuration: {}", ReflectionToStringBuilder.toString(configuration));

        //1. Getting git log messages
        GitMessageReader gitMessageReader = new GitMessageReader(configuration);
        Set<String> gitComments = gitMessageReader.read(programParameters.commitId1, programParameters.commitId2);
        String version = gitMessageReader.getLatestVersion(programParameters.commitId1, programParameters.commitId2);
        gitMessageReader.close();

        //2. Matching issue ids from git log
        Set<String> jiraIssueIds = new JiraIssueIdMatcher(configuration.getJiraIssuePattern()).findJiraIds(gitComments);

        //3. Quering jira for issues
        JiraIssueDao jiraIssueDao = new JiraIssueDao(configuration);
        Collection<Issue> issues = jiraIssueDao.findIssues(jiraIssueIds);

        //4. Creating report
        File report = new File(RELEASE_NOTES_FILE);
        new ReleaseNotesGenerator(configuration).generate(issues, report, version);

        logger.info("Release notes generated under {}", report.getAbsolutePath());
    }

    private static Configuration readConfiguration(final ProgramParameters programParameters) throws IOException, FileNotFoundException {
        String path = programParameters.configurationFilePath;
        File configurationFile = null;
        if(StringUtils.isNotEmpty(path)) {
            logger.info("Using configuration file under {}", path);
            configurationFile = new File(path);
        } else {
            configurationFile = new File(CONFIGURATION_FILE);
            logger.info("Searching for configuration file under default location {}", configurationFile.getAbsolutePath());
        }
        if(!configurationFile.exists()) {
            throw new RuntimeException("Configuration file doesnt exist under " + configurationFile.getAbsolutePath());
        }
        Properties properties = new Properties();
        properties.load(new FileInputStream(configurationFile));
        return new Configuration(properties);
    }

    public static class ProgramParameters {

        @Parameter(required = true, names = { "-configurationFilePath", "-conf" }, description = "Path to configuration file")
        private String configurationFilePath;

        @Parameter(required = true, names = { "-commitId1"}, description = "Commit 1 hash delimeter")
        private String commitId1;

        @Parameter(required = true, names = { "-commitId2"}, description = "Commit 2 hash delimeter")
        private String commitId2;
    }
}
