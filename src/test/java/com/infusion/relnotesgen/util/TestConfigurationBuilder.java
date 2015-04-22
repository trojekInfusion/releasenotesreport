package com.infusion.relnotesgen.util;

import java.util.Properties;

import com.infusion.relnotesgen.Configuration;


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
        return new Configuration(properties);
    }
}
