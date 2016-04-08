package com.infusion.relnotesgen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.ImmutablePair;

public interface JiraIssueIdMatcher {
    ImmutableList<ImmutablePair<String, ImmutableSet<String>>> findJiraIds(Iterable<String> texts);
}
