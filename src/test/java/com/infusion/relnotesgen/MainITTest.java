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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
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

    private static TestGitRepo git;
    private static StubServer jira;

    @BeforeClass
    public static void startJira() throws IOException, URISyntaxException {
        jira = new StubServer().run();
        whenHttp(jira).match(alwaysTrue()).then(status(HttpStatus.NOT_FOUND_404));
        StubedJiraIssue.stubAllExistingIssue(jira);
    }

    @AfterClass
    public static void stopJira() {
        jira.stop();
    }
    
    @Before
    public void prepareGit() throws IOException {
        git = new TestGitRepo();
    }

    @After
    public void cleanGitRepos() throws IOException {
        git.clean();
    }

    @Test
    public void reportIsCreatedByTag() throws IOException {
        //Given
        String[] args = {
                "-tag1", "1.3",
                "-pushReleaseNotes",
                "-gitDirectory", git.getGitDirectory(),
                "-gitBranch", "master",
                "-gitUrl", git.getOriginUrl(),
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
        File reportFile = new File(git.getGitDirectory(), "/releases/1_4.html");
        MainIT.assertTestReport(reportFile, "SYM-43", "SYM-42", "SYM-41");
    }
    
    @Test
    public void reportIsCreatedByCommit() throws IOException {
        //Given
        String[] args = {
                "-commitId1", "459643f30fea11f0e0e2791c5b8b247c19df8eca",
                "-commitId2", "f911f2f4db67fec386190df1abb0a3c38b457358",
                "-pushReleaseNotes",
                "-gitDirectory", git.getGitDirectory(),
                "-gitBranch", "master",
                "-gitUrl", git.getOriginUrl(),
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
        File reportFile = new File(git.getGitDirectory(), "/releases/1_3.html");
        MainIT.assertTestReport(reportFile, "SYM-32");
    }

    @Test
    public void reportIsCreatedForLatestVersionFilteredByTypeAndComponentAndStatus() throws IOException {
        //Given
        MainInvoker mainInvoker = new MainInvoker()
                .pushReleaseNotes(true)
                .gitDirectory(git.getGitDirectory())
                .gitBranch("master")
                .gitUrl(git.getOriginUrl())
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
        File reportFile = new File(git.getGitDirectory(), "/releases/1_4.html");
        MainIT.assertTestReport(reportFile, "SYM-43", "SYM-42");
    }
}
