package com.infusion.relnotesgen;

import static org.hamcrest.core.StringContains.containsString;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;


public class MainIT {

    @Before
    @After
    public void cleanRepoDirectory() throws IOException {
        //FileUtils.deleteDirectory(new File("C:/temp/testsymphony"));
    }

    @Test
    public void releaseNotesAreGenerated() throws IOException {
        //Given
        final String[] args = new String[]{
                "-configurationFilePath", MainIT.class.getResource("/configuration.properties").getFile(),
                "-commitId1", "ccd741f283ba7fae5c91477821e5de297d0ba2c5",
                "-commitId2", "26086575124207454c326a51c870649ccf18f3d9"};

        //When
        Main.main(args);

        //Then
        assertTestReport();

        //Second run with pull
        Main.main(args);
        assertTestReport();
    }

    private File assertTestReport() throws IOException {
        File reportFile = new File(Main.RELEASE_NOTES_FILE);
        String report = Files.toString(reportFile, Charset.forName("UTF-8"));
        Assert.assertThat(report, containsString("SYM-731"));
        Assert.assertThat(report, containsString("SYM-754"));

        return reportFile;
    }

}
