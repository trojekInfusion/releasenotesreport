package com.infusion.relnotesgen.util;

import com.atlassian.jira.rest.client.api.domain.Issue;

public interface JiraUtils {
    String getFieldValueByNameSafe(Issue issue, String fieldName);

    String getIssueUrl(Issue issue);
}
