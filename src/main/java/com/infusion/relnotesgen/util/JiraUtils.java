package com.infusion.relnotesgen.util;

import com.atlassian.jira.rest.client.domain.Issue;

public interface JiraUtils {
    String getFieldValueByNameSafe(Issue issue, String fieldName);
}
