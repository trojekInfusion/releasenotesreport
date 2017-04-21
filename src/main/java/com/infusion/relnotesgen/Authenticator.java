package com.infusion.relnotesgen;

import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.TransportCommand;

public interface Authenticator {
    <T extends TransportCommand<C, ?>, C extends GitCommand> C authenticate(T command);
}
