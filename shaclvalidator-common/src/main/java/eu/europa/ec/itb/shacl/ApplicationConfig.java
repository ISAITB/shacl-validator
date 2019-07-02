package eu.europa.ec.itb.shacl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by simatosc on 21/03/2016.
 * Updated by mfontsan on 26/02/2019.
 */
@Component
@ConfigurationProperties("validator")
public class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

    @Autowired
    private Environment env;

    private String resourceRoot;
    private String tmpFolder;
    private Set<String> acceptedShaclExtensions;
    private Set<String> domain;
    private Map<String, String> domainIdToDomainName = new HashMap<>();
    private Map<String, String> domainNameToDomainId = new HashMap<>();
    private String startupTimestamp;
    private String resourceUpdateTimestamp;
    private String defaultReportSyntax;
    private long cleanupWebRate;
    private Set<String> contentSyntax;

    private Map<String, String> defaultLabels = new HashMap<>();

    private String defaultContentToValidateDescription;
    private String defaultEmbeddingMethodDescription;
    private String defaultContentSyntaxDescription;
    private String defaultValidationTypeDescription;
    private String defaultExternalRulesDescription;

    public String getTmpFolder() {
        return tmpFolder;
    }

    public void setTmpFolder(String tmpFolder) {
        this.tmpFolder = tmpFolder;
    }

    public String getResourceRoot() {
        return resourceRoot;
    }

    public void setResourceRoot(String resourceRoot) {
        this.resourceRoot = resourceRoot;
    }
    
    public Set<String> getAcceptedShaclExtensions() {
        return acceptedShaclExtensions;
    }

    public void setAcceptedShaclExtensions(Set<String> acceptedShaclExtensions) {
        this.acceptedShaclExtensions = acceptedShaclExtensions;
    }

    public Set<String> getDomain() {
        return domain;
    }

    public void setDomain(Set<String> domain) {
        this.domain = domain;
    }

    public String getStartupTimestamp() {
        return startupTimestamp;
    }

    public void setStartupTimestamp(String startupTimestamp) {
        this.startupTimestamp = startupTimestamp;
    }

    public String getResourceUpdateTimestamp() {
        return resourceUpdateTimestamp;
    }

    public void setResourceUpdateTimestamp(String resourceUpdateTimestamp) {
        this.resourceUpdateTimestamp = resourceUpdateTimestamp;
    }

    public String getDefaultReportSyntax() {
        return defaultReportSyntax;
    }

    public void setDefaultReportSyntax(String defaultReportSyntax) {
        this.defaultReportSyntax = defaultReportSyntax;
    }

    public String getDefaultContentToValidateDescription() {
        return defaultContentToValidateDescription;
    }

    public String getDefaultEmbeddingMethodDescription() {
        return defaultEmbeddingMethodDescription;
    }

    public String getDefaultContentSyntaxDescription() {
        return defaultContentSyntaxDescription;
    }

    public String getDefaultValidationTypeDescription() {
        return defaultValidationTypeDescription;
    }

    public String getDefaultExternalRulesDescription() {
        return defaultExternalRulesDescription;
    }

    public void setDefaultContentToValidateDescription(String defaultContentToValidateDescription) {
        this.defaultContentToValidateDescription = defaultContentToValidateDescription;
    }

    public void setDefaultEmbeddingMethodDescription(String defaultEmbeddingMethodDescription) {
        this.defaultEmbeddingMethodDescription = defaultEmbeddingMethodDescription;
    }

    public void setDefaultContentSyntaxDescription(String defaultContentSyntaxDescription) {
        this.defaultContentSyntaxDescription = defaultContentSyntaxDescription;
    }

    public void setDefaultValidationTypeDescription(String defaultValidationTypeDescription) {
        this.defaultValidationTypeDescription = defaultValidationTypeDescription;
    }

    public void setDefaultExternalRulesDescription(String defaultExternalRulesDescription) {
        this.defaultExternalRulesDescription = defaultExternalRulesDescription;
    }

    public Map<String, String> getDomainIdToDomainName() {
        return domainIdToDomainName;
    }

    public Map<String, String> getDomainNameToDomainId() {
        return domainNameToDomainId;
    }

	public Set<String> getContentSyntax() {
		return contentSyntax;
	}

	public void setContentSyntax(Set<String> contentSyntax) {
		this.contentSyntax = contentSyntax;
	}

	public long getCleanupWebRate() {
		return cleanupWebRate;
	}

	public void setCleanupWebRate(long cleanupWebRate) {
		this.cleanupWebRate = cleanupWebRate;
	}

    public Map<String, String> getDefaultLabels() {
        return defaultLabels;
    }

    @PostConstruct
    public void init() {
        if (resourceRoot != null && Files.isDirectory(Paths.get(resourceRoot))) {
            // Setup domain.
            if (domain == null || domain.isEmpty()) {
                File[] directories = new File(resourceRoot).listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory();
                    }
                });
                if (directories == null || directories.length == 0) {
                    throw new IllegalStateException("The resource root directory ["+resourceRoot+"] is empty");
                }
                domain = Arrays.stream(directories).map(File::getName).collect(Collectors.toSet());
            }
        } else {
            throw new IllegalStateException("Invalid resourceRoot configured ["+resourceRoot+"]. Ensure you specify the validator.resourceRoot property correctly.");
        }
        logger.info("Loaded validation domains: "+domain);
        // Load domain names.
        StringBuilder logMsg = new StringBuilder();
        for (String domainFolder: domain) {
            String domainName = StringUtils.defaultIfBlank(env.getProperty("validator.domainName."+domainFolder), domainFolder);
            this.domainIdToDomainName.put(domainFolder, domainName);
            this.domainNameToDomainId.put(domainName, domainFolder);
            logMsg.append('[').append(domainFolder).append("]=[").append(domainName).append("]");
        }
        logger.info("Loaded validation domain names: " + logMsg.toString());
        // Set startup times and resource update times.
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss (XXX)");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss (XXX)");
        startupTimestamp = dtf.format(ZonedDateTime.now());
        resourceUpdateTimestamp = sdf.format(new Date(Paths.get(resourceRoot).toFile().lastModified()));
        // Default labels.
        defaultLabels.put("contentToValidate", defaultContentToValidateDescription);
        defaultLabels.put("contentSyntax", defaultContentSyntaxDescription);
        defaultLabels.put("embeddingMethod", defaultEmbeddingMethodDescription);
        defaultLabels.put("externalRules", defaultExternalRulesDescription);
        defaultLabels.put("validationType", defaultValidationTypeDescription);
    }

}
