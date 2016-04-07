package com.infusion.relnotesgen;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.apache.commons.httpclient.auth.CredentialsProvider;
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
public class GitMessageReadingTest {

    private static TestGitRepo testGitRepo = new TestGitRepo();
    private static Configuration conf = testGitRepo.configuration().build();
    private static Authenticator authenticator = new UserCredentialsAuthenticator(conf);
    private static GitFacade gitMessageReader = new GitFacade(conf, authenticator);

    private String commitId1;
    private String commitId2;
    private String[] messages;
    private String version;

    public GitMessageReadingTest(final String commitId1, final String commitId2, final String[] messages, final String version) {
        this.commitId1 = commitId1;
        this.commitId2 = commitId2;
        this.messages = messages;
        this.version = version;
    }

    @Parameters(name = "{index}: search limited by {0} and {1} should give {2}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                //all messages
                { "1c814546893dc5544f86ca87ca58f0d162c9ccd2", "430c3f94a1f2dd4940c547ae8a5ede83910597b9",
                    new String[] {"SYM-1 created initial dummy file\n", "SYM-2 changed dummy file for first time\n",
                        "SYM-2 changed dummy file for second time\n", "SYM-3 changed dummy file for third time\n"},
                    "1.0"},

                //all messages with reverted search parameters order
                { "430c3f94a1f2dd4940c547ae8a5ede83910597b9", "1c814546893dc5544f86ca87ca58f0d162c9ccd2",
                    new String[] {"SYM-1 created initial dummy file\n", "SYM-2 changed dummy file for first time\n",
                        "SYM-2 changed dummy file for second time\n", "SYM-3 changed dummy file for third time\n"},
                        "1.0" },

                //one message
                { "948fa8f6cc8a49f08e3c3a426c9e3d7323ce469a", "948fa8f6cc8a49f08e3c3a426c9e3d7323ce469a",
                    new String[] {"SYM-2 changed dummy file for second time\n"},
                    "1.0" },

                //subcollection of elements
                { "1c814546893dc5544f86ca87ca58f0d162c9ccd2", "948fa8f6cc8a49f08e3c3a426c9e3d7323ce469a",
                    new String[] {"SYM-1 created initial dummy file\n", "SYM-2 changed dummy file for first time\n",
                        "SYM-2 changed dummy file for second time\n"},
                        "1.0" },

                //subcollection of elements with reversed search parameters order
                { "430c3f94a1f2dd4940c547ae8a5ede83910597b9", "948fa8f6cc8a49f08e3c3a426c9e3d7323ce469a",
                    new String[] {"SYM-3 changed dummy file for third time\n", "SYM-2 changed dummy file for second time\n"},
                    "1.0" },

                //read by one commit second parameter null
                { "459643f30fea11f0e0e2791c5b8b247c19df8eca", null,
                    new String[] {"SYM-33 release of version 1.3\n", "SYM-41 prepare for version 1.4\n",
                        "SYM-42 prepare for version 1.4 part 2\n", "SYM-43 releas of version 1.4\n"},
                        "1.4" },

                //read by one commit first parameter null
                { null, "459643f30fea11f0e0e2791c5b8b247c19df8eca",
                    new String[] {"SYM-33 release of version 1.3\n", "SYM-41 prepare for version 1.4\n",
                        "SYM-42 prepare for version 1.4 part 2\n", "SYM-43 releas of version 1.4\n"},
                        "1.4" }
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
        SCMFacade.Response gitInfo = gitMessageReader.readByCommit(commitId1, commitId2);
        Set<String> messages = gitInfo.messages;

        // Then
        assertThat(messages, hasSize(this.messages.length));
        assertThat(messages, hasItems(this.messages));
        assertThat(gitInfo.version, equalTo(this.version));
    }
}
