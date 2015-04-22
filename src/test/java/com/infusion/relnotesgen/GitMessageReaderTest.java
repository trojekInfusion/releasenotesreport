package com.infusion.relnotesgen;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.infusion.relnotesgen.util.TestGitRepo;

/**
 * @author trojek
 *
 */
@RunWith(Parameterized.class)
public class GitMessageReaderTest {

    private static TestGitRepo testGitRepo = new TestGitRepo();
    private static GitMessageReader gitMessageReader = new GitMessageReader(testGitRepo.configuration().build());

    private String commitId1;
    private String commitId2;
    private String[] messages;

    public GitMessageReaderTest(final String commitId1, final String commitId2, final String[] messages) {
        this.commitId1 = commitId1;
        this.commitId2 = commitId2;
        this.messages = messages;
    }

    @Parameters(name = "{index}: search limited by {0} and {1} should give {2}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                //all messages
                { "1c814546893dc5544f86ca87ca58f0d162c9ccd2", "430c3f94a1f2dd4940c547ae8a5ede83910597b9",
                    new String[] {"SYM-1 created initial dummy file\n", "SYM-2 changed dummy file for first time\n",
                        "SYM-2 changed dummy file for second time\n", "SYM-3 changed dummy file for third time\n"} },

                //all messages with reverted search parameters order
                { "430c3f94a1f2dd4940c547ae8a5ede83910597b9", "1c814546893dc5544f86ca87ca58f0d162c9ccd2",
                    new String[] {"SYM-1 created initial dummy file\n", "SYM-2 changed dummy file for first time\n",
                        "SYM-2 changed dummy file for second time\n", "SYM-3 changed dummy file for third time\n"} },

                //one message
                { "948fa8f6cc8a49f08e3c3a426c9e3d7323ce469a", "948fa8f6cc8a49f08e3c3a426c9e3d7323ce469a",
                    new String[] {"SYM-2 changed dummy file for second time\n"} },

                //subcollection of elements
                { "1c814546893dc5544f86ca87ca58f0d162c9ccd2", "948fa8f6cc8a49f08e3c3a426c9e3d7323ce469a",
                    new String[] {"SYM-1 created initial dummy file\n", "SYM-2 changed dummy file for first time\n",
                        "SYM-2 changed dummy file for second time\n"} },

                //subcollection of elements with reversed search parameters order
                { "430c3f94a1f2dd4940c547ae8a5ede83910597b9", "948fa8f6cc8a49f08e3c3a426c9e3d7323ce469a",
                    new String[] {"SYM-3 changed dummy file for third time\n", "SYM-2 changed dummy file for second time\n"} }
        });
    }

    @AfterClass
    public static void removeTestGitRepo() throws IOException {
        gitMessageReader.close();
        testGitRepo.clean();
    }

    @Test
    public void readsCommitMessagesLimitedByTwoCommitIds() {
        // Given commitId1 and commitId2

        // When
        Set<String> messages = gitMessageReader.read(commitId1, commitId2);

        // Then
        assertThat(messages, hasSize(this.messages.length));
        assertThat(messages, hasItems(this.messages));
    }
}
