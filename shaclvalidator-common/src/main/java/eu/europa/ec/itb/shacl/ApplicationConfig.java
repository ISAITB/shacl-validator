package eu.europa.ec.itb.shacl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The validator application's configuration.
 */
@Component
@ConfigurationProperties("validator")
public class ApplicationConfig extends eu.europa.ec.itb.validation.commons.config.ApplicationConfig {

    private Set<String> acceptedShaclExtensions;
    private String defaultReportSyntax;
    private Set<String> contentSyntax;
    private final Map<String, String> defaultLabels = new HashMap<>();
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
    private String defaultAddRdfReportToReportDescription;
    private String defaultRdfReportSyntaxDescription;
    private String defaultRdfReportQueryDescription;

    /**
     * @return The default web service input description for adding the SHACL validation report to the TAR report context.
     */
    public String getDefaultAddRdfReportToReportDescription() {
        return defaultAddRdfReportToReportDescription;
    }

    /**
     * @param defaultAddRdfReportToReportDescription  The default web service input description for adding the SHACL validation
     *                                                report to the TAR report context.
     */
    public void setDefaultAddRdfReportToReportDescription(String defaultAddRdfReportToReportDescription) {
        this.defaultAddRdfReportToReportDescription = defaultAddRdfReportToReportDescription;
    }

    /**
     * @return The default web service input description for the RDF report syntax.
     */
    public String getDefaultRdfReportSyntaxDescription() {
        return defaultRdfReportSyntaxDescription;
    }

    /**
     * @param defaultRdfReportSyntaxDescription  The default web service input description for the RDF report syntax.
     */
    public void setDefaultRdfReportSyntaxDescription(String defaultRdfReportSyntaxDescription) {
        this.defaultRdfReportSyntaxDescription = defaultRdfReportSyntaxDescription;
    }

    /**
     * @return The default web service input description for report SPARQL query.
     */
    public String getDefaultRdfReportQueryDescription() {
        return defaultRdfReportQueryDescription;
    }

    /**
     * @param defaultRdfReportQueryDescription  The default web service input description for report SPARQL query.
     */
    public void setDefaultRdfReportQueryDescription(String defaultRdfReportQueryDescription) {
        this.defaultRdfReportQueryDescription = defaultRdfReportQueryDescription;
    }

    /**
     * @return The preferred RDF syntax to request as the result of SPARQL queries.
     */
    public String getQueryPreferredContentType() {
        return queryPreferredContentType;
    }

    /**
     * @param queryPreferredContentType The preferred RDF syntax to request as the result of SPARQL queries.
     */
    public void setQueryPreferredContentType(String queryPreferredContentType) {
        this.queryPreferredContentType = queryPreferredContentType;
    }

    /**
     * @return The default web service input description for the SPARQL query to return the content to validate.
     */
    public String getDefaultContentQueryEndpointDescription() {
        return defaultContentQueryEndpointDescription;
    }

    /**
     * @param defaultContentQueryEndpointDescription The default web service input description for the SPARQL query to
     *                                               return the content to validate.
     */
    public void setDefaultContentQueryEndpointDescription(String defaultContentQueryEndpointDescription) {
        this.defaultContentQueryEndpointDescription = defaultContentQueryEndpointDescription;
    }

    /**
     * @return The default web service input description for the SPARQL query username.
     */
    public String getDefaultContentQueryUsernameDescription() {
        return defaultContentQueryUsernameDescription;
    }

    /**
     * @param defaultContentQueryUsernameDescription The default web service input description for the SPARQL query username.
     */
    public void setDefaultContentQueryUsernameDescription(String defaultContentQueryUsernameDescription) {
        this.defaultContentQueryUsernameDescription = defaultContentQueryUsernameDescription;
    }

    /**
     * @return The default web service input description for the SPARQL query password.
     */
    public String getDefaultContentQueryPasswordDescription() {
        return defaultContentQueryPasswordDescription;
    }

    /**
     * @param defaultContentQueryPasswordDescription The default web service input description for the SPARQL query password.
     */
    public void setDefaultContentQueryPasswordDescription(String defaultContentQueryPasswordDescription) {
        this.defaultContentQueryPasswordDescription = defaultContentQueryPasswordDescription;
    }

    /**
     * @return The accepted file extensions for SHACL shape files looked up from the local filesystem.
     */
    public Set<String> getAcceptedShaclExtensions() {
        return acceptedShaclExtensions;
    }

    /**
     * @param acceptedShaclExtensions The accepted file extensions for SHACL shape files looked up from the local filesystem.
     */
    public void setAcceptedShaclExtensions(Set<String> acceptedShaclExtensions) {
        this.acceptedShaclExtensions = acceptedShaclExtensions;
    }

    /**
     * @return The default web service input description for adding the validated content to the TAR report.
     */
    public String getDefaultAddInputToReportDescription() {
        return defaultAddInputToReportDescription;
    }

    /**
     * @param defaultAddInputToReportDescription The default web service input description for adding the validated content
     *                                           to the TAR report.
     */
    public void setDefaultAddInputToReportDescription(String defaultAddInputToReportDescription) {
        this.defaultAddInputToReportDescription = defaultAddInputToReportDescription;
    }

    /**
     * @return The default web service input description for adding the SHACL shapes to the TAR report.
     */
    public String getDefaultAddRulesToReportDescription() {
        return defaultAddRulesToReportDescription;
    }

    /**
     * @param defaultAddRulesToReportDescription The default web service input description for adding the SHACL shapes to
     *                                           the TAR report.
     */
    public void setDefaultAddRulesToReportDescription(String defaultAddRulesToReportDescription) {
        this.defaultAddRulesToReportDescription = defaultAddRulesToReportDescription;
    }

    /**
     * @return The default SHACL validation report syntax.
     */
    public String getDefaultReportSyntax() {
        return defaultReportSyntax;
    }

    /**
     * @param defaultReportSyntax The default SHACL validation report syntax.
     */
    public void setDefaultReportSyntax(String defaultReportSyntax) {
        this.defaultReportSyntax = defaultReportSyntax;
    }

    /**
     * @return The default web service input description for the content to validate.
     */
    public String getDefaultContentToValidateDescription() {
        return defaultContentToValidateDescription;
    }

    /**
     * @return The default web service input description for the content's explicit embedding method.
     */
    public String getDefaultEmbeddingMethodDescription() {
        return defaultEmbeddingMethodDescription;
    }

    /**
     * @return The default web service input description for the content's RDF syntax.
     */
    public String getDefaultContentSyntaxDescription() {
        return defaultContentSyntaxDescription;
    }

    /**
     * @return The default web service input description for the validation type.
     */
    public String getDefaultValidationTypeDescription() {
        return defaultValidationTypeDescription;
    }

    /**
     * @return The default web service input description for the user-provided SHACL shapes.
     */
    public String getDefaultExternalRulesDescription() {
        return defaultExternalRulesDescription;
    }

    /**
     * @return The default web service input description for the flag on loading OWL imports.
     */
    public String getDefaultLoadImportsDescription() {
        return defaultLoadImportsDescription;
    }

    /**
     * @return The default web service input description for the content SPARQL query.
     */
    public String getDefaultContentQueryDescription() {
        return defaultContentQueryDescription;
    }

    /**
     * @param defaultContentToValidateDescription The default web service input description for the content to validate.
     */
    public void setDefaultContentToValidateDescription(String defaultContentToValidateDescription) {
        this.defaultContentToValidateDescription = defaultContentToValidateDescription;
    }

    /**
     * @param defaultEmbeddingMethodDescription The default web service input description for the content's explicit embedding
     *                                          method.
     */
    public void setDefaultEmbeddingMethodDescription(String defaultEmbeddingMethodDescription) {
        this.defaultEmbeddingMethodDescription = defaultEmbeddingMethodDescription;
    }

    /**
     * @param defaultContentSyntaxDescription The default web service input description for the content's RDF syntax.
     */
    public void setDefaultContentSyntaxDescription(String defaultContentSyntaxDescription) {
        this.defaultContentSyntaxDescription = defaultContentSyntaxDescription;
    }

    /**
     * @param defaultValidationTypeDescription The default web service input description for the validation type.
     */
    public void setDefaultValidationTypeDescription(String defaultValidationTypeDescription) {
        this.defaultValidationTypeDescription = defaultValidationTypeDescription;
    }

    /**
     * @param defaultExternalRulesDescription  The default web service input description for the user-provided SHACL shapes.
     */
    public void setDefaultExternalRulesDescription(String defaultExternalRulesDescription) {
        this.defaultExternalRulesDescription = defaultExternalRulesDescription;
    }

    /**
     * @param defaultLoadImportsDescription  The default web service input description for the flag on loading OWL imports.
     */
    public void setDefaultLoadImportsDescription(String defaultLoadImportsDescription) {
        this.defaultLoadImportsDescription = defaultLoadImportsDescription;
    }

    /**
     * @param defaultContentQueryDescription  The default web service input description for the content SPARQL query.
     */
    public void setDefaultContentQueryDescription(String defaultContentQueryDescription) {
        this.defaultContentQueryDescription = defaultContentQueryDescription;
    }

    /**
     * @return The set of supported RDF syntax values (provided as mime types).
     */
	public Set<String> getContentSyntax() {
		return contentSyntax;
	}

    /**
     * @param contentSyntax The set of supported RDF syntax values (provided as mime types).
     */
	public void setContentSyntax(Set<String> contentSyntax) {
		this.contentSyntax = contentSyntax;
	}

    /**
     * @return The default labels to use for the description of SOAP web service inputs.
     */
    public Map<String, String> getDefaultLabels() {
        return defaultLabels;
    }

    /**
     * Initialise the configuration.
     */
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
        defaultLabels.put("addRdfReportToReport", defaultAddRdfReportToReportDescription);
        defaultLabels.put("rdfReportSyntax", defaultRdfReportSyntaxDescription);
        defaultLabels.put("rdfReportQuery", defaultRdfReportQueryDescription);
    }

}
