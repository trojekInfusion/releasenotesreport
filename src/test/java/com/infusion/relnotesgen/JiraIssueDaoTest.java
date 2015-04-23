package com.infusion.relnotesgen;

import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Condition.get;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.atlassian.jira.rest.client.domain.Issue;
import com.infusion.relnotesgen.util.StubedJiraIssue;
import com.xebialabs.restito.server.StubServer;

/**
 * Https is not used because http client in jira client demands to have trusted certificate
 * SYM-43 - BUG
 * SYM-42 - BUG
 * SYM-41 - FEATURE
 * SYM-32 - TASK
 *
 * @author trojek
 *
 */
public class JiraIssueDaoTest {

    private StubServer jira;

    private JiraIssueDao jiraIssueDao;
    private Configuration configuration;

    @Test
    public void searchForExistingIssues() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"SYM-43", "SYM-42"};
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
            verifyHttp(jira).once(get("/rest/api/latest/issue/" + issueId));
        }
    }

    @Test
    public void whenIssueReturns404ProceedWithExecution() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"SYM-43", "NOT-EXISTING"};
        stubExistingIssue(issueIds[0]);
        stubNotExistingIssue(issueIds[1]);

        //When
        Collection<Issue> issues = jiraIssueDao.findIssues(new HashSet<String>(Arrays.asList(issueIds)));

        //Then
        verifyIssueWasRequested(issueIds);
        assertIssueContainsExactly(issues, "SYM-43");
    }

    private void stubNotExistingIssue(final String issueId) {
        StubedJiraIssue.stubNotExistingIssue(jira, issueId);
    }

    @Test
    public void filterByType() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"SYM-43", "SYM-42", "SYM-41"};
        stubExistingIssue(issueIds);

        when(configuration.getIssueFilterByType()).thenReturn("Feature");

        //When
        Collection<Issue> issues = jiraIssueDao.findIssues(new HashSet<String>(Arrays.asList(issueIds)));

        //Then
        verifyIssueWasRequested(issueIds);
        assertIssueContainsExactly(issues, "SYM-41");
    }

    @Test
    public void filterByTypeMultipleValues() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"SYM-43", "SYM-42", "SYM-41", "SYM-32"};
        stubExistingIssue(issueIds);

        when(configuration.getIssueFilterByType()).thenReturn("Feature,Task");

        //When
        Collection<Issue> issues = jiraIssueDao.findIssues(new HashSet<String>(Arrays.asList(issueIds)));

        //Then
        verifyIssueWasRequested(issueIds);
        assertIssueContainsExactly(issues, "SYM-41", "SYM-32");
    }

    @Test
    public void filterByComponent() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"SYM-43", "SYM-42", "SYM-41"};
        stubExistingIssue(issueIds);

        when(configuration.getIssueFilterByComponent()).thenReturn("Symphony Node");

        //When
        Collection<Issue> issues = jiraIssueDao.findIssues(new HashSet<String>(Arrays.asList(issueIds)));

        //Then
        verifyIssueWasRequested(issueIds);
        assertIssueContainsExactly(issues, "SYM-43");
    }

    @Test
    public void filterByComponentMultipleValues() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"SYM-43", "SYM-42", "SYM-41", "SYM-32"};
        stubExistingIssue(issueIds);

        when(configuration.getIssueFilterByComponent()).thenReturn("Symphony Node,Data");

        //When
        Collection<Issue> issues = jiraIssueDao.findIssues(new HashSet<String>(Arrays.asList(issueIds)));

        //Then
        verifyIssueWasRequested(issueIds);
        assertIssueContainsExactly(issues, "SYM-43", "SYM-32");
    }

    @Test
    public void filterByComponentPartTextIgnoreCase() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"SYM-43", "SYM-42", "SYM-41"};
        stubExistingIssue(issueIds);

        when(configuration.getIssueFilterByComponent()).thenReturn("node");

        //When
        Collection<Issue> issues = jiraIssueDao.findIssues(new HashSet<String>(Arrays.asList(issueIds)));

        //Then
        verifyIssueWasRequested(issueIds);
        assertIssueContainsExactly(issues, "SYM-43");
    }

    @Test
    public void filterByLabel() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"SYM-43", "SYM-42", "SYM-41"};
        stubExistingIssue(issueIds);

        when(configuration.getIssueFilterByLabel()).thenReturn("BUKA");

        //When
        Collection<Issue> issues = jiraIssueDao.findIssues(new HashSet<String>(Arrays.asList(issueIds)));

        //Then
        verifyIssueWasRequested(issueIds);
        assertIssueContainsExactly(issues, "SYM-43");
    }

    @Test
    public void filterByLabelMultipleValues() throws IOException, URISyntaxException {
        //Given
        String[] issueIds = {"SYM-43", "SYM-42", "SYM-41", "SYM-32"};
        stubExistingIssue(issueIds);

        when(configuration.getIssueFilterByLabel()).thenReturn("BUKA,gagatek");

        //When
        final Collection<Issue> issues = jiraIssueDao.findIssues(new HashSet<String>(Arrays.asList(issueIds)));

        //Then
        verifyIssueWasRequested(issueIds);
        assertIssueContainsExactly(issues, "SYM-43", "SYM-32");
    }

    private void assertIssueContainsExactly(final Collection<Issue> issues, final String... shouldContain) {
        assertThat(issues, is(notNullValue()));
        assertThat(issues, hasSize(issues.size()));
        List<String> filteredIssueIds = new ArrayList<String>() {{
            Iterator<Issue> iter = issues.iterator();
            while(iter.hasNext()) {
                add(iter.next().getKey());
            }
        }};
        Assert.assertThat(filteredIssueIds, containsInAnyOrder(shouldContain));
    }

    private void stubExistingIssue(final String... issueIds) throws IOException, URISyntaxException {
        StubedJiraIssue.stubExistingIssue(jira, issueIds);
    }

    @Before
    public void prepareConfiguration() {
        jira = new StubServer().run();

        configuration = Mockito.mock(Configuration.class);
        when(configuration.getJiraUrl()).thenReturn("http://localhost:" + jira.getPort());
        when(configuration.getJiraUsername()).thenReturn("trojek");
        when(configuration.getJiraPassword()).thenReturn("password");
        when(configuration.getIssueFilterByComponent()).thenReturn(null);
        when(configuration.getIssueFilterByLabel()).thenReturn(null);
        when(configuration.getIssueFilterByType()).thenReturn(null);

        jiraIssueDao = new JiraIssueDao(configuration);
    }

    @After
    public void stop() {
        jira.stop();
    }
}
