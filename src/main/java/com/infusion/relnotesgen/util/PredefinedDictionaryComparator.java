package com.infusion.relnotesgen.util;

import static java.util.Arrays.asList;

import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;


public class PredefinedDictionaryComparator implements Comparator<String> {

    private List<String> typeOrder = asList("New Feature", "Epic", "Bug");

    public PredefinedDictionaryComparator(final String order) {
        if(StringUtils.isNotEmpty(order)) {
            this.typeOrder = asList(order.split(","));
        }
    }

    public PredefinedDictionaryComparator(final String... order) {
        if(order != null && order.length > 0) {
            this.typeOrder = asList(order);
        }
    }

    @Override
    public int compare(final String a, final String b) {
        Integer indexA = typeOrder.indexOf(a);
        Integer indexB = typeOrder.indexOf(b);

        if(indexA < 0 && indexB < 0) {
            return a.compareTo(b);
        }
        if(indexA < 0) {
            return 1;
        }
        if(indexB < 0) {
            return -1;
        }

        return indexA.compareTo(indexB);
    }
}
