package com.infusion.relnotesgen;

import com.google.common.collect.ImmutableSet;

public class ReportCommitModel {
    private final String message;
    private final ImmutableSet<String> defectIds;

    public ReportCommitModel(final String message, final ImmutableSet<String> defectIds) {
        this.message = message;
        this.defectIds = defectIds;
    }

    public ImmutableSet<String> getDefectIds() {
        return defectIds;
    }

    public String getMessage() {
        return message;
    }
}
