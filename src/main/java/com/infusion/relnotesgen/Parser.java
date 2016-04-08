package com.infusion.relnotesgen;

import com.google.common.collect.ImmutableSet;

public interface Parser {
    ImmutableSet<String> getJiraKeys(String str);

    ImmutableSet<String> getDefectIds(String str);
}
