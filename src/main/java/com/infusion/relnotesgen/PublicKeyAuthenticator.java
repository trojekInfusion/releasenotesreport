package com.infusion.relnotesgen;

import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.*;

public class PublicKeyAuthenticator implements Authenticator {
    SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
        @Override
        protected void configure(OpenSshConfig.Host host, Session session) {
        }
    };

    @Override
    public <T extends TransportCommand<C, ?>, C extends GitCommand> C authenticate(T command) {
        return command.setTransportConfigCallback(new TransportConfigCallback() {
            @Override
            public void configure(Transport transport) {
                SshTransport sshTransport = (SshTransport) transport;
                sshTransport.setSshSessionFactory(sshSessionFactory);
            }
        });
    }
}
