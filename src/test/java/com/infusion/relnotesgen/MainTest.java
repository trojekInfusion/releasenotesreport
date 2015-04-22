package com.infusion.relnotesgen;

import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import com.beust.jcommander.ParameterException;


public class MainTest {

    @Test(expected = ParameterException.class)
    public void configurationFileParameterIsRequired() throws IOException {
        //Given
        String[] parameters = {"-commitId1", "abc", "-commitId2", "cde"};

        //When
        Main.main(parameters);

        //Then
        fail();
    }

}
