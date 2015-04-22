/**
 *
 */
package com.infusion.relnotesgen;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.infusion.relnotesgen.util.TestGitRepo;

/**
 * @author trojek
 *
 */
public class GitPushingOfReleaseNotesTest {

    private static TestGitRepo testGitRepo = new TestGitRepo();

    private GitFacade gitMessageReader;
    private File tempRepo;

    @Before
    public void cloneRepo() throws IOException {
        tempRepo = Files.createTempDirectory("TestCloneGitRepo").toFile();
    }

    @After
    public void cleanRepo() throws IOException {
        gitMessageReader.close();
        FileUtils.deleteDirectory(tempRepo);
    }

    @AfterClass
    public static void removeTestGitRepo() throws IOException {
        testGitRepo.clean();
    }

    @Test
    public void cloneNewRepositoryWithBranchNameGiven() throws IOException {
        // Given
        gitMessageReader = new GitFacade(
                testGitRepo.configuration()
                .gitDirectory(tempRepo.getAbsolutePath())
                .build());
        String version = "1.2";
        File tempReleaseNotes = File.createTempFile("ReleaseNotes", null);
        BufferedWriter bw = new BufferedWriter(new FileWriter(tempReleaseNotes));
        bw.write("Test release notes for version " + version);
        bw.close();

        // When
        boolean successfull = gitMessageReader.pushReleaseNotes(tempReleaseNotes, version);

        // Then
        assertThat(successfull, equalTo(true));

        File repoWithNotes = Files.createTempDirectory("TestCloneGitRepoWithReleaseNotes").toFile();
        try {
            GitFacade newGitRepo = new GitFacade(testGitRepo.configuration()
                    .gitDirectory(repoWithNotes.getAbsolutePath())
                    .build());
            newGitRepo.close();
            File releaseNotes = new File(repoWithNotes, "releases/" + version.replace('.', '_') + ".html");
            assertThat(releaseNotes.exists(), equalTo(true));
        } finally {
            FileUtils.deleteDirectory(repoWithNotes);
        }
    }

}
