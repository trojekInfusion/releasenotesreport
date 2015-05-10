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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

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
    public void checkPushOfNewFileWasPerformed() throws IOException {
        // Given
        gitMessageReader = new GitFacade(
                testGitRepo.configuration()
                .gitDirectory(tempRepo.getAbsolutePath())
                .build());
        String version = "1.2";
        String content = "Test release notes for version " + version;
        File tempReleaseNotes = File.createTempFile("ReleaseNotes", null);
        BufferedWriter bw = new BufferedWriter(new FileWriter(tempReleaseNotes));
        bw.write(content);
        bw.close();

        // When
        boolean successfull = gitMessageReader.pushReleaseNotes(tempReleaseNotes, version);

        // Then
        assertInNewRepo(content, tempReleaseNotes, successfull);

        //modification test
        version = "1.3";
        content = "New test release notes for version " + version;
        bw = new BufferedWriter(new FileWriter(tempReleaseNotes));
        bw.write(content);
        bw.close();

        successfull = gitMessageReader.pushReleaseNotes(tempReleaseNotes, version);

        assertInNewRepo(content, tempReleaseNotes, successfull);
    }

    private void assertInNewRepo(final String content, final File tempReleaseNotes, final boolean successfull) throws IOException {
        assertThat(successfull, equalTo(true));

        File repoWithNotes = Files.createTempDirectory("TestCloneGitRepoWithReleaseNotes").toFile();
        try {
            GitFacade newGitRepo = new GitFacade(testGitRepo.configuration()
                    .gitDirectory(repoWithNotes.getAbsolutePath())
                    .build());
            newGitRepo.close();
            File releaseNotes = new File(repoWithNotes, "releases/" + tempReleaseNotes.getName());
            assertThat(releaseNotes.exists(), equalTo(true));
            String report = Files.readAllLines(releaseNotes.toPath(), Charset.forName("UTF-8")).get(0);
            assertThat(report, equalTo(content));
        } finally {
            FileUtils.deleteDirectory(repoWithNotes);
        }
    }

}
