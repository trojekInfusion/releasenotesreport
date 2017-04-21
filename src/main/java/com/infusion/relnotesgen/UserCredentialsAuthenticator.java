package com.infusion.relnotesgen;

import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class UserCredentialsAuthenticator implements Authenticator {
    private final Configuration configuration;

    public UserCredentialsAuthenticator(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public <T extends TransportCommand<C, ?>, C extends GitCommand> C authenticate(T command) {
        return command.setCredentialsProvider(getCredentials());
    }

    private CredentialsProvider getCredentials() {
        return new UsernamePasswordCredentialsProvider(configuration.getGitUsername(), configuration.getGitPassword());
    }
}
