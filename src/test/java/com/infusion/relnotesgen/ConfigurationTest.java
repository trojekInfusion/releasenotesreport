package com.infusion.relnotesgen;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.Properties;

import org.junit.Test;

import com.infusion.relnotesgen.Configuration.Element;


/**
 * @author trojek
 *
 */
public class ConfigurationTest {

    @Test
    public void toStringDoesntContainsPassword() {
        //Given
        final String gitPassword = randomAlphanumeric(30);
        final String jiraPassword = randomAlphanumeric(30);
        Properties properties = new Properties() {
            private static final long serialVersionUID = 1048327365555461089L;
        {
            put(Configuration.GIT_PASSWORD, gitPassword);
            put(Configuration.JIRA_PASSWORD, jiraPassword);
        }};

        //When
        String toString = new Configuration(properties).toString();

        //Then
        assertThat(toString, not(containsString(gitPassword)));
        assertThat(toString, not(containsString(jiraPassword)));
    }

    @Test
    public void readsFromPropertiesFile() {
        //Given
        final String gitBranch = "branch";
        final String gitUrl = "url";
        Properties properties = new Properties() {
            private static final long serialVersionUID = -6626469165778816859L;
        {
            put(Configuration.GIT_BRANCH, gitBranch);
            put(Configuration.GIT_URL, gitUrl);
        }};

        //When
        Configuration configuration = new Configuration(properties);

        //Then
        assertThat(configuration.getGitBranch(), equalTo(gitBranch));
        assertThat(configuration.getGitUrl(), equalTo(gitUrl));
    }

    @Test
    public void configurationElementsContainerOverwritesProperties() {
        //Given
        final String gitBranch = "branch";
        final String gitUrl = "url";
        Properties properties = new Properties() {
            private static final long serialVersionUID = 649778039123087810L;
        {
            put(Configuration.GIT_BRANCH, gitBranch);
            put(Configuration.GIT_URL, gitUrl);
        }};

        //When
        Parameters parameters = new Parameters();
        Configuration configuration = new Configuration(properties, new Parameters());

        //Then
        assertThat(configuration.getGitBranch(), equalTo(parameters.gitBranch));
        assertThat(configuration.getGitUrl(), equalTo(parameters.gitUrl));
    }

    private static class Parameters {

        @Element(Configuration.GIT_BRANCH)
        private String gitBranch = "branczyk";

        @Element(Configuration.GIT_URL)
        private String gitUrl = "urleczek";
    }
}
