package com.infusion.relnotesgen.util;

import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Test;

public final class TestCollectionUtils {

    @Test
    public void testArrayToImmutableSet() {        
        
    }

    @Test
    public void testStringToArray() {
        testNonEmptyStringToArray("a,b", (new String[] {"a","b"}));
        testNonEmptyStringToArray("a", (new String[] {"a"}));
        testNonEmptyStringToArray("a,", (new String[] {"a"}));
        testNonEmptyStringToArray(",b", (new String[] {"b"}));
        testNonEmptyStringToArray(" a ", (new String[] {"a"}));
        testEmptyStringToArray(",", (new String[] {}));
        testEmptyStringToArray("", (new String[] {}));
        testEmptyStringToArray(null, (new String[] {}));        
    }

    private void testNonEmptyStringToArray(final String testString, final String[] expected) {
        String[] actualResult = CollectionUtils.stringToArray(",", testString);
        String[] expectedResult = expected;
        Arrays.sort(actualResult);
        Arrays.sort(expectedResult);
        assertNotNull("Result is null", actualResult);
        assertFalse("Expected false", actualResult.length==0);
        assertArrayEquals("Expected array is not equal to actual array", actualResult, expectedResult);
    }
    private void testEmptyStringToArray(final String testString, final String[] expected) {
        String[] actualResult = CollectionUtils.stringToArray(",", testString);
        String[] expectedResult = expected;
        Arrays.sort(actualResult);
        Arrays.sort(expectedResult);
        assertNotNull("Result is null", actualResult);
        assertTrue("Expected true", actualResult.length==0);
        assertArrayEquals("Expected array is not equal to actual array", actualResult, expectedResult);
    }

}
