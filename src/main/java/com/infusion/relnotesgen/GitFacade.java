package com.infusion.relnotesgen;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author trojek
 *
 */
public class GitFacade {

    /**
     *
     */
    private static final String RELEASES_DIR = "releases";

    private final static Logger logger = LoggerFactory.getLogger(GitFacade.class);

    private static final String DEFAULT_VERSION = "1.0";

    private Git git;
    private Configuration configuration;

    public GitFacade(final Configuration configuration) {
        logger.info("Reading git repository under {}", configuration.getGitDirectory());
        this.configuration = configuration;
        try {
            File gitRepo = new File(configuration.getGitDirectory());
            if (gitRepo.exists() && searchGit(gitRepo)) {
                logger.info("Found git repository under {}", configuration.getGitDirectory());

                pull();
                checkout();
                pull();

            } else {
                logger.info("No git repository under {}", configuration.getGitDirectory());
                if(!gitRepo.exists()) {
                    logger.info("Directory {} doesn't exists, creating it...", configuration.getGitDirectory());
                    gitRepo.mkdirs();
                }

                cloneRepo();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void checkout() throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException {
        logger.info("Git checkout to branch {}", configuration.getGitBranch());
        git.checkout().setName(configuration.getGitBranch()).call();
    }

    private void pull() throws GitAPIException, WrongRepositoryStateException,
            InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException,
            RefNotFoundException, NoHeadException, TransportException {
        logger.info("Performing pull...");
        PullResult result = git.pull().setCredentialsProvider(credentials()).call();
        if(result.isSuccessful()) {
            logger.info("Pull successfull");
        } else {
            logger.warn("Pull wasn't successfull, Fetch result: {}", result.getFetchResult().getMessages());
            logger.warn("Pull wasn't successfull, Merge conflict count: {}", CollectionUtils.size(result.getMergeResult().getConflicts()));
        }
    }

    private CredentialsProvider credentials() {
        return new UsernamePasswordCredentialsProvider(configuration.getGitUsername(), configuration.getGitPassword());
    }

    private void cloneRepo() {
        logger.info("Cloning git repository url: {}, user: {}, password: {}",
                configuration.getGitUrl(), configuration.getGitUsername(), StringUtils.abbreviate(configuration.getGitPassword(), 6));

        long startTime = System.currentTimeMillis();

        final File localPath = new File(configuration.getGitDirectory());
        try {
            git = Git.cloneRepository()
                .setURI(configuration.getGitUrl())
                .setDirectory(localPath)
                .setCredentialsProvider(credentials())
                .setBranch(configuration.getGitBranch())
                .setCloneAllBranches(false)
                .call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        logger.info("Clone is done. It took {} milis.", System.currentTimeMillis() - startTime);
    }

    private boolean searchGit(final File gitRepo) throws IOException {
        logger.info("Searching for git respoitory under {}", gitRepo.getAbsolutePath());

        try {
            Repository repository = new FileRepositoryBuilder().findGitDir(gitRepo).build();
            git = new Git(repository);
            logger.info("Found git respoitory under {}", repository.getDirectory().getAbsolutePath());
            return true;
        } catch (Exception e) {
            logger.info("Didn't find git respoitory under {}", gitRepo.getAbsolutePath());
            return false;
        }
    }

    public Response readByCommit(final String commitId1, final String commitId2) {
        try {
            Iterable<RevCommit> log = git.log().call();

            Set<String> messages = new HashSet<String>();
            RevCommit latestCommit = null;
            for (RevCommit commit : log) {
                if (!messages.isEmpty() || (commitId1 == null || commitId2 == null)) {
                    if(latestCommit == null) {
                        latestCommit = commit;
                    }
                    messages.add(commit.getFullMessage());
                }

                String commitId = commit.getId().getName();
                if (commitId.equals(commitId1) || commitId.equals(commitId2)) {
                    if (!messages.isEmpty() || (commitId1 == null || commitId2 == null)) {
                        break;
                    }

                    if(latestCommit == null) {
                        latestCommit = commit;
                    }
                    messages.add(commit.getFullMessage());

                    if(commitId1.equals(commitId2)) {
                        break;
                    }
                }
            }

            return new Response(messages, getVersion(latestCommit));
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    private String getVersion(final RevCommit commit) {
        logger.info("Searching for version in commit '{}'", commit.getFullMessage());
        try {
            logger.info("Checkout to commit '{}'", commit.getId().getName());
            git.checkout().setName(commit.getId().getName()).call();

            File pomXmlParent = git.getRepository().getDirectory().getParentFile();
            logger.info("Searching for pom.xml in directory '{}'", pomXmlParent.getAbsolutePath());

            File[] pomXmls = pomXmlParent.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(final File dir, final String name) {
                    return "pom.xml".equals(name);
                }
            });

            if (pomXmls.length == 0) {
                logger.warn("Coulnd't find pom.xml file using default version {}", DEFAULT_VERSION);
                return DEFAULT_VERSION;
            }

            String version = getVersion(pomXmls[0]);
            logger.info("Found version {} in pom.xml {}", version, pomXmls[0].getAbsolutePath());
            return version;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                checkout();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String getVersion(final File pomXml)
            throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(pomXml);
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile("/project/version/text()");
        return expr.evaluate(doc);
    }

    public Response readByTag(final String tag1, final String tag2) {
        try {
            Iterable<Ref> tags = git.tagList().call();

            String commitId1 = null;
            String commitId2 = null;
            for (Ref tag : tags) {
                if(tag1 != null && tag.getName().endsWith(tag1)) {
                    Ref peeledTag = git.getRepository().peel(tag);
                    commitId1 = peeledTag.getPeeledObjectId().getName();
                }
                if(tag2 != null && tag.getName().endsWith(tag2)) {
                    Ref peeledTag = git.getRepository().peel(tag);
                    commitId2 = peeledTag.getPeeledObjectId().getName();
                }
            }

            return readByCommit(commitId1, commitId2);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean pushReleaseNotes(final File releaseNotes, final String version) throws IOException {
        File notesDirectory = new File(git.getRepository().getDirectory().getParentFile(), RELEASES_DIR);
        boolean directoryCreated = false;
        if(!notesDirectory.exists()) {
            logger.info("Directory with release notes doesn't exist creating it in {}", notesDirectory.getAbsolutePath());
            directoryCreated = notesDirectory.mkdir();
        }
        logger.info("Copying release notes to {} (will overwrite if aleady exists)", notesDirectory.getAbsolutePath());
        File releaseNotesInGit = new File(notesDirectory, releaseNotes.getName());
        Files.copy(releaseNotes.toPath(), releaseNotesInGit.toPath(), StandardCopyOption.REPLACE_EXISTING);

        logger.info("Pushing release notes {}", releaseNotesInGit.getAbsolutePath());
        try {
            AddCommand addCommand = git.add();
            if(directoryCreated) {
                addCommand.addFilepattern(RELEASES_DIR);
            } else {
                addCommand.addFilepattern(RELEASES_DIR + "/" + releaseNotes.getName());
            }
            addCommand.call();

            Set<String> added = git.status().call().getAdded();
            if(added.size() != 1) {
                logger.error("There are more than one change ({}) to be commited, cancelling pushing release notes.", added.size());
                return false;
            }

            String commitMessage = "[Release Notes Generator] Release notes for version " + version + ". " + configuration.getGitCommitMessageValidationOmmitter();
            logger.info("Committing file '{}' with message '{}', committer name {}, committer mail {}",
                    added.iterator().next(), commitMessage, configuration.getGitCommitterName(), configuration.getGitCommitterMail());
            git.commit()
                .setCommitter(configuration.getGitCommitterName(), configuration.getGitCommitterMail())
                .setMessage(commitMessage)
                .call();

            logger.info("Pushing changes to remote...");
            Iterable<PushResult> pushResults = git.push()
                    .setCredentialsProvider(credentials())
                    .call();
            logger.info("Push call has ended.");
            for(PushResult pushResult : pushResults) {
                logger.info("Push message: {}", pushResult.getMessages());
            }
            return true;
        } catch (GitAPIException e) {
            logger.error("Error during pushing release notes", e);
            return false;
        }
    }

    public void close() {
        git.close();
    }

    public static class Response {
        public final Set<String> messages;
        public final String version;

        public Response(final Set<String> messages, final String version) {
            this.messages = messages;
            this.version = version;
        }
    }
}
