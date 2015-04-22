package com.infusion.relnotesgen;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.infusion.relnotesgen.util.TestGitRepo;


/**
 * @author trojek
 *
 */
public class GitMessageReaderWithCloneTest {


    private static TestGitRepo testGitRepo = new TestGitRepo();

    private GitMessageReader gitMessageReader;
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
        String commitId1 = "2ea0809c55657bc528933e6fda3a7772cacf8279";
        String commitId2 = "2ea0809c55657bc528933e6fda3a7772cacf8279";
        gitMessageReader = new GitMessageReader(testGitRepo.configuration()
                .gitDirectory(tempRepo.getAbsolutePath())
                .branch("branch1")
                .build());

        // When
        Set<String> messages = gitMessageReader.read(commitId1, commitId2);

        // Then
        assertThat(messages, hasSize(1));
        assertThat(messages, hasItems("SYM-4 changed dummy file on branch1 branch\n"));
    }

    @Test
    public void cloneNewRepositoryWithoutBranchNameGiven() throws IOException {
        // Given
        String commitId1 = "33589445102fd7b49421006e0447836429d84113";
        String commitId2 = "948fa8f6cc8a49f08e3c3a426c9e3d7323ce469a";
        gitMessageReader = new GitMessageReader(testGitRepo.configuration()
                .gitDirectory(tempRepo.getAbsolutePath())
                .build());

        // When
        Set<String> messages = gitMessageReader.read(commitId1, commitId2);

        // Then
        assertThat(messages, hasSize(2));
        assertThat(messages, hasItems("SYM-2 changed dummy file for second time\n", "SYM-2 changed dummy file for first time\n"));
    }

    @Test
    public void readVersionFromPomXml() throws IOException {
        // Given
        String commitId1 = "1c814546893dc5544f86ca87ca58f0d162c9ccd2";
        String commitId2 = "50dbc466d1fa6ddc714ebabbeae585af7a72524b";
        gitMessageReader = new GitMessageReader(testGitRepo.configuration()
                .gitDirectory(tempRepo.getAbsolutePath())
                .build());

        // When
        String version = gitMessageReader.getLatestVersion(commitId1, commitId2);

        // Then
        assertThat(version, Matchers.equalTo("1.1"));
    }

    @Test
    public void readVersionFromPomXmlSnapshot() throws IOException {
        // Given
        String commitId1 = "1c814546893dc5544f86ca87ca58f0d162c9ccd2";
        String commitId2 = "4f4685dfcff6514558f08d3dd303bda4684f0ffd";
        gitMessageReader = new GitMessageReader(testGitRepo.configuration()
                .gitDirectory(tempRepo.getAbsolutePath())
                .build());

        // When
        String version = gitMessageReader.getLatestVersion(commitId1, commitId2);

        // Then
        assertThat(version, Matchers.equalTo("1.1-SNAPSHOT"));
    }
}
