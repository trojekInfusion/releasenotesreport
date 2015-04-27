package com.infusion.relnotesgen;

import static org.hamcrest.core.StringContains.containsString;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;


/**
 * Integration test with real jira and git
 *
 * @author trojek
 *
 */
public class MainIT {

    static File reportDirectory = new File("target/releases");
    static {
        reportDirectory.mkdirs();
    }

    @Before
    @After
    public void cleanRepoDirectory() throws IOException {
        //FileUtils.deleteDirectory(new File("C:/temp/testsymphony"));
    }

    @Test
    public void releaseNotesAreGeneratedByCommit() throws IOException {
        //Given
        final String[] args = new String[]{
                "-configurationFilePath", MainIT.class.getResource("/configuration.properties").getFile(),
                "-commitId1", "ccd741f283ba7fae5c91477821e5de297d0ba2c5",
                "-commitId2", "26086575124207454c326a51c870649ccf18f3d9",
                "-reportDirectory", reportDirectory.getAbsolutePath()};

        //When
        Main.main(args);

        //Then
        assertTestReport(getReport(), "SYM-731", "SYM-754");

        //Second run with pull
        Main.main(args);
        assertTestReport(getReport(), "SYM-731", "SYM-754");
    }

    static File getReport() {
        File[] reports = reportDirectory.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".html");
            }
        });
        File lastModifiedReport = null;
        Date lastModification = new Date(0);
        for(File report : reports) {
            Date reportModificationDate = new Date(report.lastModified());
            if(lastModification.before(reportModificationDate)) {
                lastModifiedReport = report;
                lastModification = reportModificationDate;
            }
        }
        return lastModifiedReport;
    }

    static File assertTestReport(final File reportFile, final String... texts) throws IOException {
        String report = Files.toString(reportFile, Charset.forName("UTF-8"));
        for(String text : texts) {
            Assert.assertThat(report, containsString(text));
        }

        return reportFile;
    }

    @Test
    public void releaseNotesAreGeneratedByTagWithPushOfReleaseNotes() throws IOException {
        //Given
        final String[] args = new String[]{
                "-configurationFilePath", MainIT.class.getResource("/configuration.properties").getFile(),
                "-tag1", "0.19.0.19",
                //"-tag2", "0.19.0.10",
                "-pushReleaseNotes",
                "-reportDirectory", reportDirectory.getAbsolutePath()};

        //When
        Main.main(args);

        //Then
        Assert.assertTrue(getReport().exists());
    }

}
