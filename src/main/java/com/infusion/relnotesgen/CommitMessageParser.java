package com.infusion.relnotesgen;

import com.google.common.collect.ImmutableSet;

public interface CommitMessageParser {
    ImmutableSet<String> getJiraKeys(String text);

    ImmutableSet<String> getDefectIds(String text);
}
