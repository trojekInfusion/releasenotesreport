package com.infusion.relnotesgen;

import com.atlassian.jira.rest.client.domain.Issue;
import com.infusion.relnotesgen.util.IssueCategorizer;
import com.infusion.relnotesgen.util.IssueCategorizerImpl;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * @author trojek
 *
 */
public class ReleaseNotesGenerator {

    private final static Logger logger = LoggerFactory.getLogger(Configuration.LOGGER_NAME);
    private final IssueCategorizer issueCategorizer;
    private Configuration configuration;
    private freemarker.template.Configuration freemarkerConf;
    private String templateName = "report.ftl";

    public ReleaseNotesGenerator(final Configuration configuration) {
        issueCategorizer = new IssueCategorizerImpl(configuration);
        this.configuration = configuration;
        freemarkerConf = new freemarker.template.Configuration();

        if(isNotEmpty(configuration.getReportTemplate())) {
            logger.info("Using template {}", configuration.getReportTemplate());
            File template = new File(configuration.getReportTemplate());
            templateName = template.getName();
            try {
                freemarkerConf.setDirectoryForTemplateLoading(template.getParentFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            logger.info("Using default template.");
            freemarkerConf.setClassForTemplateLoading(ReleaseNotesGenerator.class, "/");
        }
        freemarkerConf.setIncompatibleImprovements(new Version(2, 3, 20));
        freemarkerConf.setDefaultEncoding("UTF-8");
        freemarkerConf.setLocale(Locale.getDefault());
        freemarkerConf.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        BeansWrapper beansWrapper = (BeansWrapper) ObjectWrapper.BEANS_WRAPPER;
        beansWrapper.setExposeFields(true);
        freemarkerConf.setObjectWrapper(beansWrapper);
    }

    public File generate(final Collection<Issue> issues, final File reportDirectory, final String version) throws IOException {
       return generate(issues,reportDirectory,version, new HashSet<String>());
    }

    public File generate(final Collection<Issue> issues, final File reportDirectory, final String version, final Set<String> messages) throws IOException {
        if(!reportDirectory.exists()) {
            logger.info("Report directory {} doesn't exist, creating it.", reportDirectory.getAbsolutePath());
            reportDirectory.mkdirs();
        }

        File report = new File(reportDirectory, version.replace(".", "_") + ".html");

        logger.info("Generating report to file {} with {} issues", report.getAbsolutePath(), issues.size());

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("issues", issueCategorizer.byType(issues));
        input.put("jiraUrl", configuration.getJiraUrl());
        input.put("version", version);
        input.put("messages", messages);

        Template template = freemarkerConf.getTemplate(templateName);

        try (Writer fileWriter = new FileWriter(report)) {
            try {
                template.process(input, fileWriter);
            } catch (TemplateException e) {
                throw new RuntimeException(e);
            }
        }
        logger.info("Generation of report is finished.");

        return report;
    }
}
