package com.infusion.relnotesgen;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

/**
 * Created by pkarpala on 4/10/2016.
 */
public class CommitMessageParserImplTest {

    private Configuration configuration;
    private CommitMessageParserImpl commitMessageParser;

    @Before
    public void cloneRepo() throws IOException {
        Properties properties = new Properties() {{
            put(Configuration.JIRA_ISSUEPATTERN, "((HA)|(CP))-\\d+");
        }};

        //When
        configuration = new Configuration(properties);
        commitMessageParser = new CommitMessageParserImpl(configuration);
    }

    @Test
    public void getJiraKeys_should_return_keys_when_semicolon() {
        //Given
        final String commitMessage = "HA-4935: casting check. ";

        //When
        ImmutableSet<String> keys= commitMessageParser.getJiraKeys(commitMessage);

        //Then
        assertTrue("key from commit message should be returned", keys.contains("HA-4935"));
    }
}
