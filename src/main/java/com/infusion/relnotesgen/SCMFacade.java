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
    Response readByCommit(final GitCommitTag commitId1, final GitCommitTag commitId2);
    Response readByCommit(String commitId1, String commitId2);
    boolean pushReleaseNotes(final File releaseNotes, final String version);
    void close();

    public static class Response {
        public final Set<Commit> commits;
        public final String version;
        public final GitCommitTag commitTag1;
        public final GitCommitTag commitTag2;

        public Response(final Set<Commit> commits, final String version, final GitCommitTag commitTag1,
                final GitCommitTag commitTag2) {
            this.commits = commits;
            this.version = version;
            this.commitTag1 = commitTag1;
            this.commitTag2 = commitTag2;
        }
    }

    public static class GitCommitTag
    {
        private final String commit;
        private final String tag;

        public GitCommitTag(String commit, String tag) {
            this.commit = commit;
            this.tag = tag;
        }

        public String getCommit() {
            return commit;
        }

        public String getTag() {
            return tag;
        }
    }
}
