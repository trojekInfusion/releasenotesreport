package com.infusion.relnotesgen.util;

import com.atlassian.jira.rest.client.domain.Issue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IssueCategorizer {
    Map<String, List<Issue>> byType(Collection<Issue> issues);
}
