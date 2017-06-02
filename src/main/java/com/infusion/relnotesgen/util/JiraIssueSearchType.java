package com.infusion.relnotesgen.util;

public enum JiraIssueSearchType {
	GENERIC ("Generic"), 
	INVALID ("Invalid"), 
	FIX_VERSION ("FixVersion"), 
	KNOWN_ISSUE ("KnownIssue");
    
    private String title;
    
    JiraIssueSearchType(String title) {
        this.title = title;
    }
    
    public String title() {
        return title;
    }
}
