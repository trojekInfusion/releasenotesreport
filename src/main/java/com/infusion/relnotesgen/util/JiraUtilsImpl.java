package com.infusion.relnotesgen.util;


import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.infusion.relnotesgen.Configuration;
import org.codehaus.jettison.json.JSONException;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.LinkedHashMap;

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

        if(fieldValue instanceof org.codehaus.jettison.json.JSONObject) {
            org.codehaus.jettison.json.JSONObject json = ((org.codehaus.jettison.json.JSONObject)fieldValue);

            if(json.has("value")) {
                try {
                    return json.get("value").toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
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
