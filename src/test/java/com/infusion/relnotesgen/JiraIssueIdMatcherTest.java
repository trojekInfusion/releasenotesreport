package com.infusion.relnotesgen;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertThat;

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
                    new String[] {"SYM-1", "SYM-2", "SYM-3"}  },

                //SYM-[numbers] pattern in different configuration
                { "((SYM)|(HA))-\\d+",
                        new String[] {"SYM-1:createdSYM-3, initial dummy file\n", "Merge pull request #1284 in EN/harmony-poc from bugfix/HA-5262-strip-context-of-its-auto-creating to sprint/sprint-19-hotfix-1\n"
                                + "\n" + "* commit '3b774605caeb477fc5def2671b17d4d70736c610':\n"
                                + "HA-5067: HA-5070: HA-5262: work around JVM security nonsense\n"
                                + "HA-5067: HA-5070: HA-5262: Safe navigation and script changes in AURA related scripts.\n"
                                + "HA-5067: HA-5070: HA-5262: correct AuraElementScriptIT\n"
                                + "HA-5067: HA-5070: HA-5262: Changes to Context; Changes to Context (removed auto-creation magic), publishing, added null pointer safety and access to context via string path.\n"
                                + "HA-5067: HA-5070: HA-5262: Change error modal to accound for breaking scripts sent in the error message;fold onto 7692"},
                        new String[] {"SYM-1", "SYM-3","HA-5067", "HA-5070", "HA-5262"}  }
        });
    }

    @Test
    public void readsCommitMessagesLimitedByTwoCommitIds() {
        // Given pattern and texts

        // When
        ImmutableList<ImmutablePair<String, ImmutableList<String>>> result =
                new JiraIssueIdMatcher(pattern).findJiraIds(Arrays.asList(gitCommitMessages));

        ImmutableList<String> jiraIssueIds = FluentIterable
                .from(result)
                .transformAndConcat(new Function<ImmutablePair<String, ImmutableList<String>>, Iterable<String>>() {
                    @Override
                    public Iterable<String> apply(ImmutablePair<String, ImmutableList<String>> pair) {
                        return pair.getRight();
                    }
                }).toImmutableList();

        // Then
        assertThat(jiraIssueIds, Matchers.hasSize(this.jiraIssueIds.length));
        assertThat(jiraIssueIds, Matchers.hasItems(this.jiraIssueIds));
    }
}
