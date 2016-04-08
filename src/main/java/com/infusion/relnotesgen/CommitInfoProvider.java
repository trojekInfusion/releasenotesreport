package com.infusion.relnotesgen;

import com.google.common.collect.ImmutableList;

public interface CommitInfoProvider {
    ImmutableList<String> getCommitMessages();
}
