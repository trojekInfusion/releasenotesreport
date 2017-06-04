package com.infusion.relnotesgen.util;

import com.infusion.relnotesgen.Configuration;

import java.util.Properties;


public class TestConfigurationBuilder {

    private String gitDirectory;
    private String branch = "master";
    private String url;

    public TestConfigurationBuilder gitDirectory(final String gitDirectory) {
        this.gitDirectory = gitDirectory;
        return this;
    }

    public TestConfigurationBuilder branch(final String branch) {
        this.branch = branch;
        return this;
    }

    public TestConfigurationBuilder url(final String url) {
        this.url = url;
        return this;
    }

    public Configuration build() {
        Properties properties = new Properties();
        properties.put("git.directory", gitDirectory);
        properties.put("git.branch", branch);
        properties.put("git.url", url);
        properties.put("git.username", "username");
        properties.put("git.password", "password");
        properties.put("git.committer.name", "username");
        properties.put("git.committer.mail", "mail@mail.com");
        properties.put("git.commit.limit","5");
        return new Configuration(properties);
    }
}
