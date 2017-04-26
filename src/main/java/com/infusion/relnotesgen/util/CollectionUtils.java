package com.infusion.relnotesgen.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.infusion.relnotesgen.Configuration;

public class CollectionUtils {
	
	private final static Logger logger = LoggerFactory.getLogger(Configuration.LOGGER_NAME);
	
	public static ImmutableSet<String> arrayToImmutableSet(String[] inputArray) {
		Set<String> temp = new HashSet<String>();
		try {
	    	temp.addAll(Arrays.asList(inputArray));
	    	for (String element : temp) {
	    		if (element.equals("") || element.equals(" ")) {
	    			element = null;
	    		}
	    	}
		} catch (Exception e) {
			logger.warn("{}", e.getMessage(), e);
		}
        return ImmutableSet.copyOf(temp);
	}
	
	public static String[] stringToArray(String delimiter, String inputString) {
		try {
        	if (inputString==null || inputString.isEmpty()) {
                return new String[]{};
        	}
            String[] inputStringSplit = inputString.split(delimiter);
            Set<String> inputStringsTrimmed = new HashSet<String>();
            for (String curr : inputStringSplit) {
            	if (!curr.trim().isEmpty()) {
            		inputStringsTrimmed.add(curr.trim());
            	}
            }
			String[] myArray = inputStringsTrimmed.toArray(new String[inputStringsTrimmed.size()]);
			return myArray;
        }
        catch(Exception e) {
			logger.warn("{}", e.getMessage(), e);
            return new String[]{};
        }
	}
	
	public static ImmutableSet<String> stringToImmutableSet(String delimiter, String inputString) {
	    try {
	        return arrayToImmutableSet(stringToArray(delimiter, inputString));
        } catch (Exception e) {
            logger.warn("{}", e.getMessage(), e);
        }
        return ImmutableSet.copyOf(new HashSet<String>());
	}

}
