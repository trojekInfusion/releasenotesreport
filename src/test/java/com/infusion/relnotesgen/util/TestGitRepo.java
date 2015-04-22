package com.infusion.relnotesgen.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.junit.Assert;

import com.infusion.relnotesgen.GitMessageReadingTest;

public class TestGitRepo {

    private File originTempRepo;
    private File testTempRepo;
    private Git gitRepo;

    public TestGitRepo() {
        try {
            createOriginRepository();
            cloneOriginRepository();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createOriginRepository() throws IOException, URISyntaxException {
        URL testrepo = GitMessageReadingTest.class.getResource("/testrepo");
        originTempRepo = Files.createTempDirectory("TestOriginGitRepo").toFile();
        FileUtils.copyDirectory(new File(testrepo.toURI()), originTempRepo);

        File[] gitDirectory = originTempRepo.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(final File dir, final String name) {
                return "_git".equals(name);
            }
        });
        originTempRepo = new File(gitDirectory[0].getParentFile(), ".git");
        Assert.assertTrue(gitDirectory[0].renameTo(originTempRepo));
    }

    private void cloneOriginRepository() throws IOException, InvalidRemoteException, TransportException, GitAPIException {
        gitRepo = Git.cloneRepository()
            .setURI("file:///" + originTempRepo.getAbsolutePath())
            .setDirectory(Files.createTempDirectory("TestGitRepo").toFile())
            .call();
        testTempRepo = gitRepo.getRepository().getDirectory();
    }

    public File gitDirectory() {
        return testTempRepo;
    }

    public void clean() {
        try {
            gitRepo.close();

            FileUtils.deleteDirectory(testTempRepo);
            FileUtils.deleteDirectory(originTempRepo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TestConfigurationBuilder configuration() {
        return new TestConfigurationBuilder()
            .gitDirectory(testTempRepo.getAbsolutePath())
            .url("file:///" + originTempRepo.getAbsolutePath());
    }
}
