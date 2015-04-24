/**
 *
 */
package com.infusion.relnotesgen;

import java.io.File;
import java.util.Set;


/**
 * @author trojek
 *
 */
public interface SCMFacade {

    Response readByTag(final String tag1, final String tag2);
    Response readLatestReleasedVersion();
    Response readByCommit(final String commitId1, final String commitId2);
    boolean pushReleaseNotes(final File releaseNotes, final String version);
    void close();

    public static class Response {
        public final Set<String> messages;
        public final String version;

        public Response(final Set<String> messages, final String version) {
            this.messages = messages;
            this.version = version;
        }
    }

}
