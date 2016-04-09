package com.infusion.relnotesgen;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * @author trojek
 *
 */
public class GitFacade implements SCMFacade {

    private static final Logger logger = LoggerFactory.getLogger(Configuration.LOGGER_NAME);
    private static final String RELEASES_DIR = "releases";
    private static final String DEFAULT_VERSION = "1.0";
    private final Authenticator authenticator;

    private Git git;
    private Configuration configuration;

    public GitFacade(final Configuration configuration, Authenticator authenticator) {
        this.authenticator = authenticator;
        logger.info("Reading git repository under {}", configuration.getGitDirectory());
        this.configuration = configuration;
        try {
            File gitRepo = new File(configuration.getGitDirectory());
            if (gitRepo.exists() && searchGit(gitRepo)) {
                logger.info("Found git repository under {}", configuration.getGitDirectory());

                pull();
                fetchTags();
                checkout();
                pull();

            } else {
                logger.info("No git repository under {}", configuration.getGitDirectory());
                if(!gitRepo.exists()) {
                    logger.info("Directory {} doesn't exist, creating it...", configuration.getGitDirectory());
                    gitRepo.mkdirs();
                }

                cloneRepo();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void fetchTags() {
        logger.info("Performing fetch tags...");
        git.fetch().setTagOpt(TagOpt.FETCH_TAGS);
        logger.info("Fetch tags");
    }

    private void checkout() throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException {
        logger.info("Git checkout to branch {}", configuration.getGitBranch());
        git.checkout().setName(configuration.getGitBranch()).call();
    }

    private void pull() throws GitAPIException, WrongRepositoryStateException,
            InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException,
            RefNotFoundException, NoHeadException, TransportException {
        logger.info("Performing pull...");
        PullResult result = authenticator.authenticate(git.pull()).call();
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
        logger.info("Cloning git repository url: {}", configuration.getGitUrl());

        long startTime = System.currentTimeMillis();

        final File localPath = new File(configuration.getGitDirectory());

        try {
            git = authenticator.authenticate(Git.cloneRepository())
                .setURI(configuration.getGitUrl())
                .setDirectory(localPath)
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

    @Override
    public Response readByCommit(final String commitId1, final String commitId2) {
        try {
            Iterable<RevCommit> log = git.log().call();

            Set<Commit> commits = new HashSet<>();
            RevCommit latestCommit = null;
            for (RevCommit commit : log) {
                if (!commits.isEmpty() || (commitId1 == null || commitId2 == null)) {
                    if(latestCommit == null) {
                        latestCommit = commit;
                    }
                    commits.add(new Commit(commit.getFullMessage(), commit.getId().getName(), commit.getAuthorIdent().getName()));
                }

                String commitId = commit.getId().getName();
                if (commitId.equals(commitId1) || commitId.equals(commitId2)) {
                    if (!commits.isEmpty() || (commitId1 == null || commitId2 == null)) {
                        break;
                    }

                    if(latestCommit == null) {
                        latestCommit = commit;
                    }

                    commits.add(new Commit(commit.getFullMessage(), commit.getId().getName(), commit.getAuthorIdent().getName()));

                    if(commitId1.equals(commitId2)) {
                        break;
                    }
                }
            }
            logger.info("Found {} commit commits.", commits.size());
            if(commits.size() == 0) {
                throw new RuntimeException("No commit were found for given commit ids " + commitId1 + ", " + commitId2 + ". Maybe branch is badly chosen.");
            }

            return new Response(commits, getVersion(latestCommit));
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

    @Override
    public Response readByTag(final String tag1, final String tag2) {
        try {
            Iterable<Ref> tags = git.tagList().call();

            String commitId1 = null;
            String commitId2 = null;

            for (Ref tag : tags) {
                if(isNotBlank(tag1) && tag.getName().endsWith(tag1)) {
                    commitId1 = retrieveCommitIdFromTag(tag);
                    logger.info("Found tag '{}' using commit id '{}'.", tag.getName(), commitId1);
                }
                if(isNotBlank(tag2) && tag.getName().endsWith(tag2)) {
                    commitId2 = retrieveCommitIdFromTag(tag);
                    logger.info("Found tag '{}' using commit id '{}'.", tag.getName(), commitId2);
                }
            }

            return readByCommit(commitId1, commitId2);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    private String retrieveCommitIdFromTag(final Ref tag) {
        Ref peeledTag = git.getRepository().peel(tag);
        if(peeledTag.getPeeledObjectId() == null) {
            //http://dev.eclipse.org/mhonarc/lists/jgit-dev/msg01706.html
            //when peeled tag is null it means this is 'lighweight' tag and object id points to commit straight forward
            return peeledTag.getObjectId().getName();
        } else {
            return peeledTag.getPeeledObjectId().getName();
        }
    }

    @Override
    public Response readLatestReleasedVersion() {
        try {
            Iterable<Ref> tags = git.tagList().call();
            final RevWalk walk = new RevWalk(git.getRepository());

            String tag1 = null;
            String tag2 = null;
            Date latestDate = new Date(0);
            for(Ref tag : tags) {
                Date tagDate = getDateFromTag(walk, tag);

                if(latestDate.before(tagDate)) {
                    tag2 = tag1;
                    tag1 = tag.getName();
                    latestDate = tagDate;
                }
            }

            return readByTag(tag1, tag2);
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Date getDateFromTag(final RevWalk walk, final Ref tag) throws MissingObjectException, IncorrectObjectTypeException, IOException {
        try {
            return walk.parseTag(tag.getObjectId()).getTaggerIdent().getWhen();
        } catch (IOException e) {
            //http://dev.eclipse.org/mhonarc/lists/jgit-dev/msg01706.html
            //when peeled tag is null it means this is 'lighweight' tag and object id points to commit straight forward
            return walk.parseCommit(tag.getObjectId()).getCommitterIdent().getWhen();
        }
    }

    @Override
    public boolean pushReleaseNotes(final File releaseNotes, final String version) {
        File notesDirectory = new File(git.getRepository().getDirectory().getParentFile(), RELEASES_DIR);
        boolean directoryCreated = false;
        if(!notesDirectory.exists()) {
            logger.info("Directory with release notes doesn't exist creating it in {}", notesDirectory.getAbsolutePath());
            directoryCreated = notesDirectory.mkdir();
        }
        logger.info("Copying release notes to {} (will overwrite if aleady exists)", notesDirectory.getAbsolutePath());
        File releaseNotesInGit = new File(notesDirectory, releaseNotes.getName());
        try {
            Files.copy(releaseNotes.toPath(), releaseNotesInGit.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }

        logger.info("Pushing release notes {}", releaseNotesInGit.getAbsolutePath());
        try {
            AddCommand addCommand = git.add();
            if(directoryCreated) {
                addCommand.addFilepattern(RELEASES_DIR);
            } else {
                addCommand.addFilepattern(RELEASES_DIR + "/" + releaseNotes.getName());
            }
            addCommand.call();

            Set<String> changes = validateChangesStatusOfReleaseNotes();
            if(changes == null) {
                return false;
            }

            String commitMessage = buildCommitMessage(version);
            logger.info("Committing file '{}' with message '{}', committer name {}, committer mail {}",
                    changes.iterator().next(), commitMessage, configuration.getGitCommitterName(), configuration.getGitCommitterMail());
            git.commit()
                    .setCommitter(configuration.getGitCommitterName(), configuration.getGitCommitterMail())
                    .setMessage(commitMessage)
                    .call();

            logger.info("Pushing changes to remote...");
            Iterable<PushResult> pushResults = authenticator.authenticate(git.push()).call();
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

    private Set<String> validateChangesStatusOfReleaseNotes() throws NoWorkTreeException, GitAPIException {
        Status status = git.status().call();
        Set<String> added = status.getAdded();
        Set<String> modified = status.getModified();
        Set<String> changed = status.getChanged();
        if(added.size() > 1 || modified.size() > 1 || changed.size() > 1) {
            logger.error("There are more than one change [added({}), modified({}), changed({})] to be commited, cancelling pushing release notes.", added.size(), modified.size(), changed.size());
            return null;
        }
        if(added.isEmpty() && modified.isEmpty() && changed.isEmpty()) {
            logger.error("There are no changes to be commited, probably identical release notes has been already generated and pushed to repository.");
            return null;
        }
        if(!added.isEmpty()) {
            return added;
        }
        if(!modified.isEmpty()) {
            return modified;
        }
        if(!changed.isEmpty()) {
            return changed;
        }
        return null;
    }

    private String buildCommitMessage(final String version) {
        StringBuilder messageBuilder = new StringBuilder("[release-notes-generator] Release notes for version ")
                .append(version)
                .append(".");
        if(StringUtils.isNotEmpty(configuration.getGitCommitMessageValidationOmmitter())) {
            messageBuilder.append(" ").append(configuration.getGitCommitMessageValidationOmmitter());
        }
        return messageBuilder.toString();
    }

    @Override
    public void close() {
        git.close();
    }
}
