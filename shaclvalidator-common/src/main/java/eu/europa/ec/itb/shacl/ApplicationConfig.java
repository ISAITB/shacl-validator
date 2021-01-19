package eu.europa.ec.itb.shacl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by simatosc on 21/03/2016.
 * Updated by mfontsan on 26/02/2019.
 */
@Component
@ConfigurationProperties("validator")
public class ApplicationConfig extends eu.europa.ec.itb.validation.commons.config.ApplicationConfig {

    private Set<String> acceptedShaclExtensions;
    private String defaultReportSyntax;
    private Set<String> contentSyntax;
    private Map<String, String> defaultLabels = new HashMap<>();
    private String defaultContentToValidateDescription;
    private String defaultEmbeddingMethodDescription;
    private String defaultContentSyntaxDescription;
    private String defaultValidationTypeDescription;
    private String defaultExternalRulesDescription;
    private String defaultLoadImportsDescription;
    private String defaultAddInputToReportDescription;
    private String defaultAddRulesToReportDescription;

    private String queryPreferredContentType;
    private String defaultContentQueryDescription;
    private String defaultContentQueryEndpointDescription;
    private String defaultContentQueryUsernameDescription;
    private String defaultContentQueryPasswordDescription;

    public String getQueryPreferredContentType() {
        return queryPreferredContentType;
    }

    public void setQueryPreferredContentType(String queryPreferredContentType) {
        this.queryPreferredContentType = queryPreferredContentType;
    }

    public String getDefaultContentQueryEndpointDescription() {
        return defaultContentQueryEndpointDescription;
    }

    public void setDefaultContentQueryEndpointDescription(String defaultContentQueryEndpointDescription) {
        this.defaultContentQueryEndpointDescription = defaultContentQueryEndpointDescription;
    }

    public String getDefaultContentQueryUsernameDescription() {
        return defaultContentQueryUsernameDescription;
    }

    public void setDefaultContentQueryUsernameDescription(String defaultContentQueryUsernameDescription) {
        this.defaultContentQueryUsernameDescription = defaultContentQueryUsernameDescription;
    }

    public String getDefaultContentQueryPasswordDescription() {
        return defaultContentQueryPasswordDescription;
    }

    public void setDefaultContentQueryPasswordDescription(String defaultContentQueryPasswordDescription) {
        this.defaultContentQueryPasswordDescription = defaultContentQueryPasswordDescription;
    }

    public Set<String> getAcceptedShaclExtensions() {
        return acceptedShaclExtensions;
    }

    public void setAcceptedShaclExtensions(Set<String> acceptedShaclExtensions) {
        this.acceptedShaclExtensions = acceptedShaclExtensions;
    }

    public String getDefaultAddInputToReportDescription() {
        return defaultAddInputToReportDescription;
    }

    public void setDefaultAddInputToReportDescription(String defaultAddInputToReportDescription) {
        this.defaultAddInputToReportDescription = defaultAddInputToReportDescription;
    }

    public String getDefaultAddRulesToReportDescription() {
        return defaultAddRulesToReportDescription;
    }

    public void setDefaultAddRulesToReportDescription(String defaultAddRulesToReportDescription) {
        this.defaultAddRulesToReportDescription = defaultAddRulesToReportDescription;
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

    public String getDefaultLoadImportsDescription() {
        return defaultLoadImportsDescription;
    }

    public String getDefaultContentQueryDescription() {
        return defaultContentQueryDescription;
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

    public void setDefaultLoadImportsDescription(String defaultLoadImportsDescription) {
        this.defaultLoadImportsDescription = defaultLoadImportsDescription;
    }

    public void setDefaultContentQueryDescription(String defaultContentQueryDescription) {
        this.defaultContentQueryDescription = defaultContentQueryDescription;
    }

	public Set<String> getContentSyntax() {
		return contentSyntax;
	}

	public void setContentSyntax(Set<String> contentSyntax) {
		this.contentSyntax = contentSyntax;
	}

    public Map<String, String> getDefaultLabels() {
        return defaultLabels;
    }

	@PostConstruct
    public void init() {
        super.init();
        // Default labels.
        defaultLabels.put("contentToValidate", defaultContentToValidateDescription);
        defaultLabels.put("contentSyntax", defaultContentSyntaxDescription);
        defaultLabels.put("embeddingMethod", defaultEmbeddingMethodDescription);
        defaultLabels.put("externalRules", defaultExternalRulesDescription);
        defaultLabels.put("validationType", defaultValidationTypeDescription);
        defaultLabels.put("loadImports", defaultLoadImportsDescription);
        defaultLabels.put("addInputToReport", defaultAddInputToReportDescription);
        defaultLabels.put("addRulesToReport", defaultAddRulesToReportDescription);
        defaultLabels.put("contentQuery", defaultContentQueryDescription);
        defaultLabels.put("contentQueryEndpoint", defaultContentQueryEndpointDescription);
        defaultLabels.put("contentQueryUsername", defaultContentQueryUsernameDescription);
        defaultLabels.put("contentQueryPassword", defaultContentQueryPasswordDescription);
    }

}
