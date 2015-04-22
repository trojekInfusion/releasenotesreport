package com.infusion.relnotesgen;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.atlassian.jira.rest.client.domain.BasicIssueType;
import com.atlassian.jira.rest.client.domain.BasicPriority;
import com.atlassian.jira.rest.client.domain.Issue;

public class ReleaseNotesGeneratorTest {

    private static final String JIRA_URL = "http://localhost/jira";
	private ReleaseNotesGenerator releaseNotesGenerator;

	@Before
	public void prepareConfiguration() {
	    Configuration configuration = mock(Configuration.class, Mockito.RETURNS_SMART_NULLS);
	    releaseNotesGenerator = new ReleaseNotesGenerator(configuration);
	    when(configuration.getIssueSortType()).thenReturn("Feature,Bug");
        when(configuration.getIssueSortPriority()).thenReturn("Big,Medium,Small");
        when(configuration.getIssueSortPriority()).thenReturn("Big,Medium,Small");
        when(configuration.getJiraUrl()).thenReturn(JIRA_URL);
	}

	@Test
	public void reportHasIssueIds() throws IOException, XPathExpressionException, ParserConfigurationException, SAXException {
		//Given
		Collection<Issue> issues = new ArrayList<Issue>();
		issues.add(addIssue("SYM-731", "Bug", "Small"));
		issues.add(addIssue("SYM-732", "Bug", "Big"));
        issues.add(addIssue("SYM-667", "Technical Task", "Big"));
		issues.add(addIssue("SYM-737", "Feature", "Medium"));
        issues.add(addIssue("SYM-736", "Feature", "Big"));
        issues.add(addIssue("SYM-666", "Task", "Big"));
        String version = "1.0.0";
		File report = new File("target/report.html");

		//When
		releaseNotesGenerator.generate(issues, report, version);

		//Then
        assertXpath(report, "/html/head/title", "Release notes for version " + version);
        assertXpath(report, "/html/body/h1", "Release notes for version " + version);

		assertXpath(report, "count(/html/body/p)", "4");

		assertXpath(report, "/html/body/p[1]/text()", "Feature (2)");
		assertXpath(report, "count(/html/body/ul[1]/li)", "2");
        assertXpath(report, "/html/body/ul[1]/li[1]/a/text()", "SYM-736: Summary summary");
        assertXpath(report, "/html/body/ul[1]/li[1]/a/@href", JIRA_URL + "/browse/SYM-736");
        assertXpath(report, "/html/body/ul[1]/li[2]/a/text()", "SYM-737");

		assertXpath(report, "/html/body/p[2]/text()", "Bug");
		assertXpath(report, "count(/html/body/ul[2]/li)", "2");
        assertXpath(report, "/html/body/ul[2]/li[1]/a/text()", "SYM-732");
        assertXpath(report, "/html/body/ul[2]/li[2]/a/text()", "SYM-731");

        assertXpath(report, "/html/body/p[3]/text()", "Task");
        assertXpath(report, "count(/html/body/ul[3]/li)", "1");
        assertXpath(report, "/html/body/ul[3]/li[1]/a/text()", "SYM-666");

        assertXpath(report, "/html/body/p[4]/text()", "Technical Task");
        assertXpath(report, "count(/html/body/ul[4]/li)", "1");
        assertXpath(report, "/html/body/ul[4]/li[1]/a/text()", "SYM-667");
	}

    private void assertXpath(final File report, final String xPath, final String expectedValue) throws ParserConfigurationException, SAXException, IOException,
            XPathExpressionException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(report);
		XPathFactory xPathfactory = XPathFactory.newInstance();
		XPath xpath = xPathfactory.newXPath();
		XPathExpression expr = xpath.compile(xPath);
		String actualValue = expr.evaluate(doc);
		Assert.assertThat(actualValue, StringContains.containsString(expectedValue));
    }

	private Issue addIssue(final String key, final String type, final String priority) {
		Issue issue = mock(Issue.class);
		when(issue.getKey()).thenReturn(key);
        when(issue.getSummary()).thenReturn("Summary summary summary, summary summary summary.");

		BasicIssueType issueType = mock(BasicIssueType.class);
		when(issueType.getName()).thenReturn(type);
		when(issue.getIssueType()).thenReturn(issueType);

		BasicPriority issuePriority = mock(BasicPriority.class);
        when(issuePriority.getName()).thenReturn(priority);
        when(issue.getPriority()).thenReturn(issuePriority);

		return issue;
	}

}
