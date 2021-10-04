package eu.europa.ec.itb.shacl;

import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;

import java.util.List;
import java.util.Map;

/**
 * The configuration for a specific validation domain.
 */
public class DomainConfig extends WebDomainConfig {

    private String defaultReportSyntax;
    private boolean reportsOrdered;
    private boolean mergeModelsBeforeValidation;
    Map<String, Boolean> defaultLoadImportsType;
    Map<String, ExternalArtifactSupport> userInputForLoadImportsType;
    private List<String> webContentSyntax;
    private boolean supportsQueries;
    private String queryEndpoint;
    private ExternalArtifactSupport queryAuthentication;
    private String queryUsername;
    private String queryPassword;
    private String queryContentType;

    /**
     * @return Whether authentication is needed for SPARQL queries.
     */
    public ExternalArtifactSupport getQueryAuthentication() {
        return queryAuthentication;
    }

    /**
     * @param queryAuthentication Whether authentication is needed for SPARQL queries.
     */
    public void setQueryAuthentication(ExternalArtifactSupport queryAuthentication) {
        this.queryAuthentication = queryAuthentication;
    }

    /**
     * @return True if user-provided shapes are supported or expected.
     */
    public boolean supportsExternalArtifacts() {
        for (TypedValidationArtifactInfo info: getArtifactInfo().values()) {
            if (info.getOverallExternalArtifactSupport() != ExternalArtifactSupport.NONE) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return True if users are allowed to specify whether OWL imports will be loaded.
     */
    public boolean supportsUserProvidedLoadImports() {
        for (ExternalArtifactSupport supportType: getUserInputForLoadImportsType().values()) {
            if (supportType != ExternalArtifactSupport.NONE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the shape configuration for a given validation type.
     *
     * @param validationType The validation type.
     * @return The shape configuration.
     */
    public ValidationArtifactInfo getShapeInfo(String validationType) {
        return getArtifactInfo().get(validationType).get();
    }

    /**
     * @return True if SPARQL queries are supported.
     */
    public boolean isSupportsQueries() {
        return supportsQueries;
    }

    /**
     * @return True if SPARQL queries require authentication.
     */
    public boolean isQueryAuthenticationMandatory() {
        return supportsQueries && queryAuthentication == ExternalArtifactSupport.REQUIRED;
    }

    /**
     * @return True if SPARQL queries support or require authentication.
     */
    public boolean supportsQueryCredentials() {
        return supportsQueries && queryAuthentication != ExternalArtifactSupport.NONE;
    }

    /**
     * @param supportsQueries True if SPARQL queries are supported.
     */
    public void setSupportsQueries(boolean supportsQueries) {
        this.supportsQueries = supportsQueries;
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
     * @return True if validation reports should be ordered.
     */
	public boolean isReportsOrdered() {
        return reportsOrdered;
    }

    /**
     * @param reportsOrdered True if validation reports should be ordered.
     */
    public void setReportsOrdered(boolean reportsOrdered) {
        this.reportsOrdered = reportsOrdered;
    }

    /**
     * @return The mapping of validation type to whether loading of OWL imports from the content is made by default.
     */
    public Map<String, Boolean> getDefaultLoadImportsType() {
		return defaultLoadImportsType;
	}

    /**
     * @param loadImportsType The mapping of validation type to whether loading of OWL imports from the content is made by default.
     */
	public void setDefaultLoadImportsType(Map<String, Boolean> loadImportsType) {
		this.defaultLoadImportsType = loadImportsType;
	}

    /**
     * @return True if the input graph should be merged with the shape graph before validation.
     */
    public boolean isMergeModelsBeforeValidation() {
		return mergeModelsBeforeValidation;
	}

    /**
     * @param mergeModelsBeforeValidation True if the input graph should be merged with the shape graph before validation.
     */
	public void setMergeModelsBeforeValidation(boolean mergeModelsBeforeValidation) {
		this.mergeModelsBeforeValidation = mergeModelsBeforeValidation;
	}

    /**
     * @return The mapping from validation type to the support level for users providing the flag on whether or not OWL
     * imports should be loaded from the input.
     */
	public Map<String, ExternalArtifactSupport> getUserInputForLoadImportsType() {
		return userInputForLoadImportsType;
	}

    /**
     * @param userInputForLoadImportsType The mapping from validation type to the support level for users providing the
     *                                    flag on whether or not OWL imports should be loaded from the input.
     */
	public void setUserInputForLoadImportsType(Map<String, ExternalArtifactSupport> userInputForLoadImportsType) {
		this.userInputForLoadImportsType = userInputForLoadImportsType;
	}

    /**
     * @return The accepted syntax mime types for the user-provided RDF content to validate.
     */
	public List<String> getWebContentSyntax() {
		return webContentSyntax;
	}

    /**
     * @param webContentSyntax The accepted syntax mime types for the user-provided RDF content to validate.
     */
	public void setWebContentSyntax(List<String> webContentSyntax) {
		this.webContentSyntax = webContentSyntax;
	}

    /**
     * @return The SPARQL endpoint URL to query for the content to validate.
     */
	public String getQueryEndpoint() {
		return queryEndpoint;
	}

    /**
     * @param queryEndpoint The SPARQL endpoint URL to query for the content to validate.
     */
	public void setQueryEndpoint(String queryEndpoint) {
		this.queryEndpoint = queryEndpoint;
	}

    /**
     * @return The username for the SPARQL endpoint to query for the content to validate.
     */
	public String getQueryUsername() {
		return queryUsername;
	}

    /**
     * @param queryUsername The username for the SPARQL endpoint to query for the content to validate.
     */
	public void setQueryUsername(String queryUsername) {
		this.queryUsername = queryUsername;
	}

    /**
     * @return The password for the SPARQL endpoint to query for the content to validate.
     */
	public String getQueryPassword() {
		return queryPassword;
	}

    /**
     * @param queryPassword The username for the SPARQL endpoint to query for the content to validate.
     */
	public void setQueryPassword(String queryPassword) {
		this.queryPassword = queryPassword;
	}

    /**
     * @return The RDF syntax (as a mime type) to request as the result of SPARQL queries for the validator's content.
     */
	public String getQueryContentType() {
		return queryContentType;
	}

    /**
     * @param queryContentType The RDF syntax (as a mime type) to request as the result of SPARQL queries for the validator's content.
     */
	public void setQueryContentType(String queryContentType) {
		this.queryContentType = queryContentType;
	}

}
