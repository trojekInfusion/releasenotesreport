package com.infusion.relnotesgen.util;

import com.atlassian.jira.rest.client.domain.Field;
import com.atlassian.jira.rest.client.domain.Issue;

public class JiraUtilsImpl implements JiraUtils {
    @Override
    public String getFieldValueByNameSafe(Issue issue, String fieldName) {
        Field field = issue.getFieldByName(fieldName);

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
