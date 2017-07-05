package com.infusion.relnotesgen;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class ReportCommitModel {
    private final String id;
    private final String message;
    private final String author;
    private final ImmutableSet<String> defectIds;
    private final ImmutableSet<String> jiraIds;

    private ReportCommitModel(final String id, final String message, final ImmutableSet<String> defectIds, final ImmutableSet<String> jiraIds, final String author) {
        this.id = id;
        this.message = message;
        this.defectIds = defectIds;
        this.jiraIds = jiraIds;
        this.author = author;
    }

    public ImmutableSet<String> getDefectIds() {
        return defectIds;
    }

    public ImmutableSet<String> getJiraIds() {
        return jiraIds;
    }

    public String getMessage() {
        return message;
    }

    public String getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }
    
    public String toString() {
        StringBuilder temp = new StringBuilder("ReportCommitModel: \n");
        temp.append("id: ").append("[").append(id).append("]\n");
        temp.append("message: ").append(message).append("\n");
        temp.append("author: ").append(author).append("\n");
        temp.append("defectIds: ").append(defectIds).append("\n");
        temp.append("jiraIds: ").append(jiraIds).append("\n");
        return temp.toString();
    }
    
    public static class ReportCommitModelBuilder {
        private String nestedId;
        private String nestedMessage;
        private String nestedAuthor;
        private ImmutableSet<String> nestedDefectIds;
        private ImmutableSet<String> nestedJiraIds;
        
        public ReportCommitModelBuilder() {}
        
        public ReportCommitModelBuilder id(final String newId) {
            this.nestedId = newId;
            return this;
        }

        public ReportCommitModelBuilder message(final String newMessage) {
            this.nestedMessage = newMessage;
            return this;
        }
        
        public ReportCommitModelBuilder author(final String newAuthor) {
            this.nestedAuthor = newAuthor;
            return this;
        }
        
        public ReportCommitModelBuilder defectIds(final ImmutableSet<String> newDefectIds) {
            this.nestedDefectIds = newDefectIds;
            return this;
        }
        
        public ReportCommitModelBuilder jiraIds(final ImmutableSet<String> newJiraIds) {
            this.nestedJiraIds = newJiraIds;
            return this;
        }
        
        public ReportCommitModel build() throws IllegalStateException {
            if (!isInitalizedProperly()) {
                throw new IllegalStateException("Required parameters were not initialized");
            }
            return new ReportCommitModel(nestedId, nestedMessage, nestedDefectIds, nestedJiraIds, nestedAuthor);
        }

        private boolean isInitalizedProperly() {
            if (nestedId==null || nestedMessage==null || nestedAuthor==null || nestedDefectIds==null) {
                return false;
            }
            if (nestedJiraIds == null) {
                Set<String> temp = new HashSet<String>();
                nestedJiraIds = ImmutableSet.copyOf(temp);
            }
            return true;
        }       

    }
}
