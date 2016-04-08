package com.infusion.relnotesgen.util;

import com.atlassian.jira.rest.client.domain.Issue;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;

public class IssuePriorityComparator implements Comparator<Issue> {

    private PredefinedDictionaryComparator predefinedDictionaryComparator;
    private String[] typeOrder = {"Highest", "High", "Medium", "Low", "Lowest"};

    public IssuePriorityComparator(final String order) {
        if (StringUtils.isNotEmpty(order)) {
            this.typeOrder = order.split(",");
        }
        predefinedDictionaryComparator = new PredefinedDictionaryComparator(typeOrder);
    }

    @Override
    public int compare(final Issue a, final Issue b) {
        if (b.getPriority() == null) {
            return -1;
        }
        if (a.getPriority() == null) {
            return 1;
        }

        return predefinedDictionaryComparator.compare(a.getPriority().getName(), b.getPriority().getName());
    }
}
