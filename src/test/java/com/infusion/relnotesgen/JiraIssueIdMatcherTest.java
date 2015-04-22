package com.infusion.relnotesgen;

import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author trojek
 *
 */
@RunWith(Parameterized.class)
public class JiraIssueIdMatcherTest {

    private String pattern;
    private String[] gitCommitMessages;
    private String[] jiraIssueIds;

    public JiraIssueIdMatcherTest(final String pattern, final String[] texts, final String[] jiraIssueIds) {
        super();
        this.pattern = pattern;
        this.gitCommitMessages = texts;
        this.jiraIssueIds = jiraIssueIds;
    }

    @Parameters(name = "{index}: pattern {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                //SYM-[numbers] pattern
                { "SYM-\\d+",
                    new String[] {"SYM-1 created initial dummy file\n", "SYM-2 changed dummy file for first time\n",
                        "SYM-2 changed dummy file for second time\n", "SYM-3 changed dummy file for third time\n"},
                    new String[] {"SYM-1", "SYM-2", "SYM-3"} },

                //SYM-[numbers] pattern in different configuration
                { "SYM-\\d+",
                    new String[] {"[SYM-1] createdSYM-3, initial dummy file\n", "'SYM-2' changed dummy file for first time\n"},
                    new String[] {"SYM-1", "SYM-2", "SYM-3"}  }
        });
    }

    @Test
    public void readsCommitMessagesLimitedByTwoCommitIds() {
        // Given pattern and texts

        // When
        Set<String> jiraIssueIds = new JiraIssueIdMatcher(pattern).findJiraIds(Arrays.asList(gitCommitMessages));

        // Then
        assertThat(jiraIssueIds, Matchers.hasSize(this.jiraIssueIds.length));
        assertThat(jiraIssueIds, Matchers.hasItems(this.jiraIssueIds));
    }

}
