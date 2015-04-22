package com.infusion.relnotesgen;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;
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
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author trojek
 *
 */
public class GitMessageReader {

    private final static Logger logger = LoggerFactory.getLogger(GitMessageReader.class);

    private static final String DEFAULT_VERSION = "1.0";

    private Git git;
    private Configuration configuration;

    public GitMessageReader(final Configuration configuration) {
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

    public Set<String> read(final String commitId1, final String commitId2) {
        try {
            Iterable<RevCommit> log = git.log().call();

            Set<String> messages = new HashSet<String>();
            for (RevCommit commit : log) {
                if (!messages.isEmpty()) {
                    messages.add(commit.getFullMessage());
                }

                String commitId = commit.getId().getName();
                if (commitId.equals(commitId1) || commitId.equals(commitId2)) {
                    if (!messages.isEmpty()) {
                        break;
                    }
                    messages.add(commit.getFullMessage());

                    if(commitId1.equals(commitId2)) {
                        break;
                    }
                }
            }

            return messages;
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    public String getLatestVersion(final String commitId1, final String commitId2) {
        logger.info("Searching for version...");
        try {
            RevCommit commit = findLatestCommit(commitId1, commitId2);

            if (commit != null) {
                logger.info("Checkout to latest commit {}", commit.getId().getName());
                git.checkout().setName(commit.getId().getName()).call();

                File pomXmlParent = git.getRepository().getDirectory().getParentFile();
                logger.info("Searching for pom.xml in directory {}", pomXmlParent.getAbsolutePath());

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
            }
            logger.warn("Coulnd't find version using default version {}", DEFAULT_VERSION);
            return DEFAULT_VERSION;
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

    private RevCommit findLatestCommit(final String commitId1, final String commitId2) {
        try {
            Iterable<RevCommit> log = git.log().call();

            RevCommit latestCommit = null;
            Date latestCommitDate = new Date(0L);
            for (RevCommit commit : log) {
                String commitId = commit.getId().getName();
                if (commitId.equals(commitId1) || commitId.equals(commitId2)) {
                    PersonIdent authorIdent = commit.getAuthorIdent();
                    Date authorDate = authorIdent.getWhen();

                    if(latestCommitDate.before(authorDate)) {
                        latestCommitDate = authorDate;
                        latestCommit = commit;
                    }
                }
            }
            return latestCommit;
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    public void close() {
        git.close();
    }
}
