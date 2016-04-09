package com.infusion.relnotesgen;

import com.google.common.collect.ImmutableSet;

public interface CommitInfoProvider {
    ImmutableSet<Commit> getCommits();
}
