package com.infusion.relnotesgen;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.header;
import static com.xebialabs.restito.semantics.Action.ok;
import static com.xebialabs.restito.semantics.Action.resourceContent;
import static com.xebialabs.restito.semantics.Condition.get;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.glassfish.grizzly.http.util.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.atlassian.jira.rest.client.domain.Issue;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.server.StubServer;

/**
 * Https is not used because http client in jira client demands to have trusted certificate
 *
 * @author trojek
 *
 */
public class JiraIssueDaoTest {

    private StubServer server;

    private JiraIssueDao jiraIssueDao;
    private Configuration configuration;

    @Test
    public void searchForExistingIssues() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"BUG-666", "BUG-667"};
        stubExistingIssue(issueIds);

        //When
        Collection<Issue> issues = jiraIssueDao.findIssues(new HashSet<String>(Arrays.asList(issueIds)));

        //Then
        verifyIssueWasRequested(issueIds);

        assertThat(issues, is(notNullValue()));
        assertThat(issues, hasSize(issueIds.length));
    }

    private void verifyIssueWasRequested(final String... issueIds) {
        for(String issueId : issueIds) {
            verifyHttp(server).once(get("/rest/api/latest/issue/" + issueId));
        }
    }

    @Test
    public void whenIssueReturns404ProceedWithExecution() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"BUG-666", "NOT-EXISTING"};
        stubExistingIssue(issueIds[0]);
        stubNotExistingIssue(issueIds[1]);

        //When
        Collection<Issue> issues = jiraIssueDao.findIssues(new HashSet<String>(Arrays.asList(issueIds)));

        //Then
        verifyIssueWasRequested(issueIds);

        assertThat(issues, is(notNullValue()));
        assertThat(issues, hasSize(1));
    }

    private void stubNotExistingIssue(final String issueId) {
        whenHttp(server)
            .match(get("/rest/api/latest/issue/" + issueId))
            .then(Action.status(HttpStatus.NOT_FOUND_404));
    }

    @Test
    public void filterByType() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"BUG-666", "BUG-667", "FEATURE-666"};
        stubExistingIssue(issueIds);

        when(configuration.getIssueFilterByType()).thenReturn("Feature");

        //When
        Collection<Issue> issues = jiraIssueDao.findIssues(new HashSet<String>(Arrays.asList(issueIds)));

        //Then
        verifyIssueWasRequested(issueIds);

        assertThat(issues, is(notNullValue()));
        assertThat(issues, hasSize(1));
        assertThat(issues.iterator().next().getKey(), Matchers.is(Matchers.equalTo("FEATURE-666")));
    }

    @Test
    public void filterByTypeMultipleValues() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"BUG-666", "BUG-667", "FEATURE-666", "TASK-666"};
        stubExistingIssue(issueIds);

        when(configuration.getIssueFilterByType()).thenReturn("Feature,Task");

        //When
        Collection<Issue> issues = jiraIssueDao.findIssues(new HashSet<String>(Arrays.asList(issueIds)));

        //Then
        verifyIssueWasRequested(issueIds);

        assertThat(issues, is(notNullValue()));
        assertThat(issues, hasSize(2));
        List<Issue> issueList = new ArrayList(issues);
        assertThat(issueList.get(0).getKey(), Matchers.is(Matchers.equalTo("FEATURE-666")));
        assertThat(issueList.get(1).getKey(), Matchers.is(Matchers.equalTo("TASK-666")));
    }

    @Test
    public void filterByComponent() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"BUG-666", "BUG-667", "FEATURE-666"};
        stubExistingIssue(issueIds);

        when(configuration.getIssueFilterByComponent()).thenReturn("Symphony Node");

        //When
        Collection<Issue> issues = jiraIssueDao.findIssues(new HashSet<String>(Arrays.asList(issueIds)));

        //Then
        verifyIssueWasRequested(issueIds);

        assertThat(issues, is(notNullValue()));
        assertThat(issues, hasSize(1));
        assertThat(issues.iterator().next().getKey(), Matchers.is(Matchers.equalTo("BUG-666")));
    }

    @Test
    public void filterByComponentMultipleValues() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"BUG-666", "BUG-667", "FEATURE-666", "TASK-666"};
        stubExistingIssue(issueIds);

        when(configuration.getIssueFilterByComponent()).thenReturn("Symphony Node,Data");

        //When
        Collection<Issue> issues = jiraIssueDao.findIssues(new HashSet<String>(Arrays.asList(issueIds)));

        //Then
        verifyIssueWasRequested(issueIds);

        assertThat(issues, is(notNullValue()));
        assertThat(issues, hasSize(2));
        List<Issue> issueList = new ArrayList(issues);
        assertThat(issueList.get(0).getKey(), Matchers.is(Matchers.equalTo("BUG-666")));
        assertThat(issueList.get(1).getKey(), Matchers.is(Matchers.equalTo("TASK-666")));
    }

    @Test
    public void filterByComponentPartTextIgnoreCase() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"BUG-666", "BUG-667", "FEATURE-666"};
        stubExistingIssue(issueIds);

        when(configuration.getIssueFilterByComponent()).thenReturn("node");

        //When
        Collection<Issue> issues = jiraIssueDao.findIssues(new HashSet<String>(Arrays.asList(issueIds)));

        //Then
        verifyIssueWasRequested(issueIds);

        assertThat(issues, is(notNullValue()));
        assertThat(issues, hasSize(1));
        assertThat(issues.iterator().next().getKey(), Matchers.is(Matchers.equalTo("BUG-666")));
    }

    @Test
    public void filterByLabel() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"BUG-666", "BUG-667", "FEATURE-666"};
        stubExistingIssue(issueIds);

        when(configuration.getIssueFilterByLabel()).thenReturn("BUKA");

        //When
        Collection<Issue> issues = jiraIssueDao.findIssues(new HashSet<String>(Arrays.asList(issueIds)));

        //Then
        verifyIssueWasRequested(issueIds);

        assertThat(issues, is(notNullValue()));
        assertThat(issues, hasSize(1));
        assertThat(issues.iterator().next().getKey(), Matchers.is(Matchers.equalTo("BUG-666")));
    }

    @Test
    public void filterByLabelMultipleValues() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"BUG-666", "BUG-667", "FEATURE-666", "TASK-666"};
        stubExistingIssue(issueIds);

        when(configuration.getIssueFilterByLabel()).thenReturn("BUKA,gagatek");

        //When
        Collection<Issue> issues = jiraIssueDao.findIssues(new HashSet<String>(Arrays.asList(issueIds)));

        //Then
        verifyIssueWasRequested(issueIds);

        assertThat(issues, is(notNullValue()));
        assertThat(issues, hasSize(2));
        List<Issue> issueList = new ArrayList(issues);
        assertThat(issueList.get(0).getKey(), Matchers.is(Matchers.equalTo("BUG-666")));
        assertThat(issueList.get(1).getKey(), Matchers.is(Matchers.equalTo("TASK-666")));
    }

    private void stubExistingIssue(final String... issueIds) throws IOException, URISyntaxException {
        for(String issueId : issueIds) {
            URL responseUrl = JiraIssueDaoTest.class.getResource("/testissues/" + issueId + ".json");

            whenHttp(server)
                .match(get("/rest/api/latest/issue/" + issueId))
                .then(Action.composite(
                        ok(),
                        contentType("application/json"),
                        header("Content-Encoding", "gzip"),
                        header("Content-Type", "application/json"),
                        header("X-Content-Type-Options", "nosniff"),
                        resourceContent(responseUrl))
                     );
        }
    }

    @Before
    public void prepareConfiguration() {
        server = new StubServer().run();

        configuration = Mockito.mock(Configuration.class);
        when(configuration.getJiraUrl()).thenReturn("http://localhost:" + server.getPort());
        when(configuration.getJiraUsername()).thenReturn("trojek");
        when(configuration.getJiraPassword()).thenReturn("password");
        when(configuration.getIssueFilterByComponent()).thenReturn(null);
        when(configuration.getIssueFilterByLabel()).thenReturn(null);
        when(configuration.getIssueFilterByType()).thenReturn(null);

        jiraIssueDao = new JiraIssueDao(configuration);
    }

    @After
    public void stop() {
        server.stop();
    }
}
