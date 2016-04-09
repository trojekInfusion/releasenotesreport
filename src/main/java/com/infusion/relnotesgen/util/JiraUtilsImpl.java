package com.infusion.relnotesgen.util;


import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.infusion.relnotesgen.Configuration;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

public class JiraUtilsImpl implements JiraUtils {
    private final Configuration configuration;

    public JiraUtilsImpl(final Configuration configuration) {
        this.configuration = configuration;
    }

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

    @Override
    public String getIssueUrl(Issue issue) {
        try {
            URL baseURL = new URL(configuration.getJiraUrl());
            return new URL(baseURL, MessageFormat.format("/browse/{0}", issue.getKey())).toString();
        } catch (MalformedURLException e) {
            return "#ERROR";
        }
    }

}
