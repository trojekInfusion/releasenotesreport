package com.infusion.relnotesgen.util;


import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;

public class JiraUtilsImpl implements JiraUtils {
    @Override
    public String getFieldValueByNameSafe(Issue issue, String fieldName) {
        IssueField field = issue.getFieldByName(fieldName);

        if (field == null) {
            return null;
        }

        Object fieldValue = field.getValue();

        if (fieldValue == null) {
            return null;
        }

        return fieldValue.toString();
    }
}
