package com.infusion.relnotesgen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Map.Entry;
import java.util.Properties;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;


/**
 * @author trojek
 *
 */
public class Configuration {

    public static final String LOGGER_NAME = "com.infusion.relnotesgen.log.ReleaseNotesLogger";
    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    static final String GIT_DIRECTORY = "git.directory";
    static final String GIT_BRANCH = "git.branch";
    static final String GIT_URL = "git.url";
    static final String GIT_BROWSE_PRS_URL = "git.browsePrs.url";
    static final String GIT_USERNAME = "git.username";
    static final String GIT_PASSWORD = "git.password";
    static final String GIT_COMMITTER_NAME = "git.committer.name";
    static final String GIT_COMMITTER_MAIL = "git.committer.mail";
    static final String GIT_COMMITMESSAGE_VALIDATIONOMMITER = "git.commitmessage.validationommiter";
    static final String GIT_COMMIT_LIMIT = "git.commit.limit";
    static final String JIRA_URL = "jira.url";
    static final String JIRA_USERNAME = "jira.username";
    static final String JIRA_PASSWORD = "jira.password";
    static final String JIRA_ISSUEPATTERN = "jira.issuepattern";
    static final String ISSUE_FILTERBY_COMPONENT = "issue.filterby.component";
    static final String ISSUE_FILTERBY_TYPE = "issue.filterby.type";
    static final String ISSUE_FILTERBY_LABEL = "issue.filterby.label";
    static final String ISSUE_FILTERBY_STATUS = "issue.filterby.status";
    static final String ISSUE_SORT_TYPE = "issue.sort.type";
    static final String ISSUE_SORT_PRIORITY = "issue.sort.priority";
    static final String REPORT_DIRECTORY = "report.directory";
    static final String REPORT_TEMPLATE = "report.template";
    static final String RELEASE_VERSION = "version.release";

    private Properties properties;

    public Configuration(final Properties properties) {
        this.properties = properties;
    }

    public Configuration(final Properties properties, final Object configurationContainer) {
        this.properties = properties;

        for (Field field : configurationContainer.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Element.class)) {
                try {
                    String key = field.getAnnotation(Element.class).value();
                    field.setAccessible(true);
                    String value = (String) field.get(configurationContainer);
                    if(isNotEmpty(value)) {
                        properties.put(key, value);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public String getGitDirectory() {
        return properties.getProperty(GIT_DIRECTORY);
    }

    public String getGitBranch() {
        return properties.getProperty(GIT_BRANCH);
    }

    public String getGitUrl() {
        return properties.getProperty(GIT_URL);
    }

    public String getGitBrowsePrsUrl() {
        return properties.getProperty(GIT_BROWSE_PRS_URL);
    }

    public String getGitUsername() {
        return properties.getProperty(GIT_USERNAME);
    }

    public String getGitPassword() {
        return properties.getProperty(GIT_PASSWORD);
    }

    public String getGitCommitterName() {
        return properties.getProperty(GIT_COMMITTER_NAME);
    }

    public String getGitCommitterMail() {
        return properties.getProperty(GIT_COMMITTER_MAIL);
    }

    public String getGitCommitMessageValidationOmmitter() {
        return properties.getProperty(GIT_COMMITMESSAGE_VALIDATIONOMMITER);
    }

    public String getJiraUrl() {
        return properties.getProperty(JIRA_URL);
    }

    public String getJiraUsername() {
        return properties.getProperty(JIRA_USERNAME);
    }

    public String getJiraPassword() {
        return properties.getProperty(JIRA_PASSWORD);
    }

    public String getJiraIssuePattern() {
        return properties.getProperty(JIRA_ISSUEPATTERN);
    }

    public String getIssueFilterByComponent() {
        return properties.getProperty(ISSUE_FILTERBY_COMPONENT);
    }

    public String getIssueFilterByType() {
        return properties.getProperty(ISSUE_FILTERBY_TYPE);
    }

    public String getIssueFilterByLabel() {
        return properties.getProperty(ISSUE_FILTERBY_LABEL);
    }

    public String getIssueFilterByStatus() {
        return properties.getProperty(ISSUE_FILTERBY_STATUS);
    }

    public String getIssueSortType() {
        return properties.getProperty(ISSUE_SORT_TYPE);
    }

    public String getIssueSortPriority() {
        return properties.getProperty(ISSUE_SORT_PRIORITY);
    }

    public String getReportDirectory() {
        return properties.getProperty(REPORT_DIRECTORY);
    }

    public String getReportTemplate() {
        return properties.getProperty(REPORT_TEMPLATE);
    }

    public String getReleaseVersion() { return properties.getProperty(RELEASE_VERSION); }

    public int getGitCommitLimit() {
        try{
            return Integer.parseInt(properties.getProperty(GIT_COMMIT_LIMIT));
        }
        catch (NumberFormatException e) {
            logger.info("Couldn't parse '{}', defaulting value to 100", GIT_COMMIT_LIMIT);
            return 100;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Configuration[");
        for(Entry<Object, Object> entry : properties.entrySet()) {
            if(!entry.getKey().toString().contains("password")) {
                if(builder.length() > 14) {
                    builder.append("|");
                }
                builder.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        builder.append("]");
        return builder.toString();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface Element {
        public String value();
    }
}
