/**
 *
 */
package com.infusion.relnotesgen;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Action.status;
import static com.xebialabs.restito.semantics.Condition.alwaysTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.infusion.relnotesgen.util.StubedJiraIssue;
import com.infusion.relnotesgen.util.TestGitRepo;
import com.xebialabs.restito.server.StubServer;


/**
 * Integration test for whole generator module with local git and jira mocked by restito
 *
 * @author trojek
 *
 */
public class MainITTest {

    private static TestGitRepo originGit;
    private static File localGit;
    private static StubServer jira;

    @BeforeClass
    public static void startJira() throws IOException, URISyntaxException {
        jira = new StubServer().run();
        whenHttp(jira).match(alwaysTrue()).then(status(HttpStatus.NOT_FOUND_404));
        StubedJiraIssue.stubAllExistingIssue(jira);
    }

    @BeforeClass
    public static void prepareGit() throws IOException {
        originGit = new TestGitRepo();
        localGit = Files.createTempDirectory("TestGitRepo").toFile();
    }

    @AfterClass
    public static void stopJira() {
        jira.stop();
    }

    @AfterClass
    public static void cleanGitRepos() throws IOException {
        FileUtils.deleteDirectory(localGit);
        originGit.clean();
    }

    @Test
    public void reportIsCreated() throws IOException {
        //Given
        String[] args = {
                "-tag1", "1.3",
                "-pushReleaseNotes",
                "-gitDirectory", localGit.getAbsolutePath(),
                "-gitBranch", "master",
                "-gitUrl", originGit.getOriginUrl(),
                "-gitUsername", "username",
                "-gitPassword", "password",
                "-gitCommitterName", "username",
                "-gitCommitterMail", "username@mail.com",
                "-gitCommitMessageValidationOmmiter", "",
                "-jiraUrl", "http://localhost:" + jira.getPort(),
                "-jiraUsername", "username",
                "-jiraPassword", "password",
                "-jiraIssuePattern", "SYM-\\d+",
                "-issueFilterByComponent", "",
                "-issueFilterByType", "",
                "-issueFilterByLabel", "",
                "-issueFilterByStatus", "",
                "-issueSortType", "",
                "-issueSortPriority", "",
                "-reportDirectory", ""};

        //When
        Main.main(args);

        //Then
        File reportFile = new File(localGit.getAbsoluteFile(), "/releases/1_4.html");
        MainIT.assertTestReport(reportFile, "SYM-43", "SYM-42", "SYM-41");
    }

    @Test
    public void reportIsCreatedForLatestVersionFilteredByTypeAndComponentAndStatus() throws IOException {
        //Given
        MainInvoker mainInvoker = new MainInvoker()
                .pushReleaseNotes(true)
                .gitDirectory(localGit.getAbsolutePath())
                .gitBranch("master")
                .gitUrl(originGit.getOriginUrl())
                .gitUsername("username")
                .gitPassword("password")
                .gitCommitterName("username")
                .gitCommitterMail("username@mail.com")
                .gitCommitMessageValidationOmmiter("")
                .jiraUrl("http://localhost:" + jira.getPort())
                .jiraUsername("username")
                .jiraPassword("password")
                .jiraIssuePattern("SYM-\\d+")
                .issueFilterByComponent("node")
                .issueFilterByType("Bug")
                .issueFilterByStatus("Ready for QA");

        //When
        mainInvoker.invoke();

        //Then
        File reportFile = new File(localGit.getAbsoluteFile(), "/releases/1_4.html");
        MainIT.assertTestReport(reportFile, "SYM-43", "SYM-42");
    }
}
