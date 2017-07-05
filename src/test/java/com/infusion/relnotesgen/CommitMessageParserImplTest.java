package com.infusion.relnotesgen;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by pkarpala on 4/10/2016.
 */
public class CommitMessageParserImplTest {

    private Configuration configuration;
    private CommitMessageParserImpl commitMessageParser;

    @Before
    public void cloneRepo() throws IOException {
        Properties properties = new Properties() {
            private static final long serialVersionUID = -8206630758757439576L;
        {
            put(Configuration.JIRA_ISSUEPATTERN, "((HA)|(CP))-\\d\\d+");
        }};

        //When
        configuration = new Configuration(properties);
        commitMessageParser = new CommitMessageParserImpl(configuration);
    }

    @Test
    public void getJiraKeys_should_return_keys_when_semicolon() {
        //Given
        final String commitMessage = "Merge pull request #12 in EN/harmony\n HA-4935: casting check. ";

        //When
        ImmutableSet<String> keys= commitMessageParser.getJiraKeys(commitMessage);

        //Then
        assertTrue("key from commit message should be returned", keys.contains("HA-4935"));
    }

    @Test
    public void getJiraKeys_should_skip_commit_messages_that_are_not_merges() {
        //Given
        final String commitMessage = "HA-4935 HA-1 commit for my work";

        //When
        ImmutableSet<String> keys= commitMessageParser.getJiraKeys(commitMessage);

        //Then
        assertEquals("no ids should be returned because it's not a merge commit", 0, keys.size());
    }

    @Test
    public void getJiraKeys_should_not_return_excluded_keys() {
        //Given
        final String commitMessage = "Merge pull request #710 in EN/harmony from bugfix/HA-9779-view-plan-header-is-too-big-on-ie11 to release/1.2\n" +
                "\n" +
                "    * commit 'c8661066b7cf4f11e662712fab9ba9305da34419':\n" +
                "      CP-45 HA-1 : remove overflow visible from iE\n"+
                "      HA-1 : some other work";

        //When
        ImmutableSet<String> keys= commitMessageParser.getJiraKeys(commitMessage);

        //Then
        //assertTrue("key from commit message should be returned", keys.contains("HA-4935"));
        assertTrue("key from commit message should be returned", keys.contains("CP-45"));
        assertFalse("key from commit message should not be returned", keys.contains("HA-1"));
    }

    @Test
    public void getPullRequestId_should_return_PR_id() {
        //Given
        final String commitMessage = "Merge pull request #710 in EN/harmony from bugfix/HA-9779-view-plan-header-is-too-big-on-ie11 to release/1.2\n" +
                "\n" +
                "    * commit 'c8661066b7cf4f11e662712fab9ba9305da34419':\n" +
                "      HA-9779 : remove overflow visible from iE";

        //When
        String prId = commitMessageParser.getPullRequestId(commitMessage);

        //Then
        assertEquals("Pull request id should be extracted from commit message","710", prId );
    }
}
