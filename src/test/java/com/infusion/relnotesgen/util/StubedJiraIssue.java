/**
 *
 */
package com.infusion.relnotesgen.util;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.header;
import static com.xebialabs.restito.semantics.Action.ok;
import static com.xebialabs.restito.semantics.Action.resourceContent;
import static com.xebialabs.restito.semantics.Condition.get;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.glassfish.grizzly.http.util.HttpStatus;

import com.infusion.relnotesgen.JiraIssueDaoTest;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.server.StubServer;


/**
 * @author trojek
 *
 */
public class StubedJiraIssue {

    public static void stubAllExistingIssue(final StubServer jira) throws IOException, URISyntaxException {
        stubExistingIssue(jira, "SYM-43", "SYM-42", "SYM-41", "SYM-32");
    }

    public static void stubExistingIssue(final StubServer jira, final String... issueIds) throws IOException, URISyntaxException {
        for(String issueId : issueIds) {
            URL responseUrl = JiraIssueDaoTest.class.getResource("/testissues/" + issueId + ".json");

            whenHttp(jira)
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

    public static void stubNotExistingIssue(final StubServer jira, final String issueId) {
        whenHttp(jira)
            .match(get("/rest/api/latest/issue/" + issueId))
            .then(Action.status(HttpStatus.NOT_FOUND_404));
    }

}
