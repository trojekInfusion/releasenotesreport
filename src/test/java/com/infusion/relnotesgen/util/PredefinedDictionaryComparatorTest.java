package com.infusion.relnotesgen.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.util.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author trojek
 *
 */
public class PredefinedDictionaryComparatorTest {


    @Test
    public void orderAsInDictionaryWordsOnlyFromDictionary() {
        //Given
        String[] dictionary = {"Cthulu", "Babadook", "Freddy", "Jason", "StewieGriffin"};
        PredefinedDictionaryComparator fearComparator = new PredefinedDictionaryComparator(StringUtils.join(Arrays.asList(dictionary), ","));
        List<String> fears = Arrays.asList("Freddy", "Babadook", "StewieGriffin", "Jason", "Cthulu");

        //When
        Collections.sort(fears, fearComparator);

        //Then
        Assert.assertThat(fears, Matchers.contains(dictionary));
    }

    @Test
    public void orderAsInDictionaryOthersAlfabeticalWordsFromDictionaryAndOthers() {
        //Given
        String[] dictionary = {"Cthulu", "Freddy", "StewieGriffin", "Pinky"};
        PredefinedDictionaryComparator fearComparator = new PredefinedDictionaryComparator(StringUtils.join(Arrays.asList(dictionary), ","));
        List<String> fears = Arrays.asList("Freddy", "Babadook", "Damian", "Thing", "StewieGriffin", "Jason", "Cthulu");

        //When
        Collections.sort(fears, fearComparator);

        //Then
        Assert.assertThat(fears, Matchers.contains("Cthulu", "Freddy", "StewieGriffin", "Babadook", "Damian", "Jason", "Thing"));
    }

    @Test
    public void orderAsInDictionaryOthersAlfabeticalOneWordFromDictionaryAndOthers() {
        //Given
        String[] dictionary = {"StewieGriffin", "PeterGriffin"};
        PredefinedDictionaryComparator fearComparator = new PredefinedDictionaryComparator(StringUtils.join(Arrays.asList(dictionary), ","));
        List<String> fears = Arrays.asList("Freddy", "Babadook", "StewieGriffin", "Jason", "Cthulu");

        //When
        Collections.sort(fears, fearComparator);

        //Then
        Assert.assertThat(fears, Matchers.contains("StewieGriffin", "Babadook", "Cthulu","Freddy", "Jason"));
    }
}
