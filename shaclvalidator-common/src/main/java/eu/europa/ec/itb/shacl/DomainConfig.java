package eu.europa.ec.itb.shacl;

import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.LabelConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;

import java.util.List;
import java.util.Map;

/**
 * The configuration for a specific validation domain.
 */
public class DomainConfig extends WebDomainConfig<DomainConfig.Label> {

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
     * Create a new empty instance of a label configuration object.
     *
     * @return The object.
     */
    @Override
    protected Label newLabelConfig() {
        return new Label();
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

    /**
     * Configuration class for the labels of the user interface.
     */
    public static class Label extends LabelConfig {

        private String contentSyntaxLabel;
        private String externalShapesLabel;
        private String optionDownloadReport;
        private String optionDownloadContent;
        private String optionDownloadShapes;
        private String contentSyntaxTooltip;
        private String externalRulesTooltip;
        private String resultLocationLabel;
        private String saveDownload;
        private String saveAs;
        private String reportItemFocusNode;
        private String reportItemResultPath;
        private String reportItemShape;
        private String reportItemValue;
        private String includeExternalShapes;
        private String loadImportsLabel;
        private String loadImportsTooltip;
        private String optionContentQuery;
        private String queryEndpointInputPlaceholder;
        private String queryUsernameInputPlaceholder;
        private String queryPasswordInputPlaceholder;
        private String queryAuthenticateLabel;

        /**
         * @return The label for the option to provide input via SPARQL query.
         */
        public String getOptionContentQuery() {
            return optionContentQuery;
        }

        /**
         * @param optionContentQuery The label for the option to provide input via SPARQL query.
         */
        public void setOptionContentQuery(String optionContentQuery) {
            this.optionContentQuery = optionContentQuery;
        }

        /**
         * @return The placeholder for the option to provide input via SPARQL query.
         */
        public String getQueryEndpointInputPlaceholder() {
            return queryEndpointInputPlaceholder;
        }

        /**
         * @param queryEndpointInputPlaceholder The placeholder for the option to provide input via SPARQL query.
         */
        public void setQueryEndpointInputPlaceholder(String queryEndpointInputPlaceholder) {
            this.queryEndpointInputPlaceholder = queryEndpointInputPlaceholder;
        }

        /**
         * @return The placeholder for the SPARQL endpoint username.
         */
        public String getQueryUsernameInputPlaceholder() {
            return queryUsernameInputPlaceholder;
        }

        /**
         * @param queryUsernameInputPlaceholder The placeholder for the SPARQL endpoint username.
         */
        public void setQueryUsernameInputPlaceholder(String queryUsernameInputPlaceholder) {
            this.queryUsernameInputPlaceholder = queryUsernameInputPlaceholder;
        }

        /**
         * @return The placeholder for the SPARQL endpoint password.
         */
        public String getQueryPasswordInputPlaceholder() {
            return queryPasswordInputPlaceholder;
        }

        /**
         * @param queryPasswordInputPlaceholder The placeholder for the SPARQL endpoint password.
         */
        public void setQueryPasswordInputPlaceholder(String queryPasswordInputPlaceholder) {
            this.queryPasswordInputPlaceholder = queryPasswordInputPlaceholder;
        }

        /**
         * @return The label for the SPARQL endpoint authentication.
         */
        public String getQueryAuthenticateLabel() {
            return queryAuthenticateLabel;
        }

        /**
         * @param queryAuthenticateLabel The label for the SPARQL endpoint authentication.
         */
        public void setQueryAuthenticateLabel(String queryAuthenticateLabel) {
            this.queryAuthenticateLabel = queryAuthenticateLabel;
        }

        /**
         * @return The label for the report item focus node.
         */
        public String getReportItemFocusNode() {
            return reportItemFocusNode;
        }

        /**
         * @param reportItemFocusNode The label for the report item focus node.
         */
        public void setReportItemFocusNode(String reportItemFocusNode) {
            this.reportItemFocusNode = reportItemFocusNode;
        }

        /**
         * @return The label for the report item result path.
         */
        public String getReportItemResultPath() {
            return reportItemResultPath;
        }

        /**
         * @param reportItemResultPath The label for the report item result path.
         */
        public void setReportItemResultPath(String reportItemResultPath) {
            this.reportItemResultPath = reportItemResultPath;
        }

        /**
         * @return The label for the report item shape.
         */
        public String getReportItemShape() {
            return reportItemShape;
        }

        /**
         * @param reportItemShape The label for the report item shape.
         */
        public void setReportItemShape(String reportItemShape) {
            this.reportItemShape = reportItemShape;
        }

        /**
         * @return The label for the report item value.
         */
        public String getReportItemValue() {
            return reportItemValue;
        }

        /**
         * @param reportItemValue The label for the report item value.
         */
        public void setReportItemValue(String reportItemValue) {
            this.reportItemValue = reportItemValue;
        }

        /**
         * @return The label on including user-provided shapes.
         */
        public String getIncludeExternalShapes() {
            return includeExternalShapes;
        }

        /**
         * @param includeExternalShapes The label on including user-provided shapes.
         */
        public void setIncludeExternalShapes(String includeExternalShapes) {
            this.includeExternalShapes = includeExternalShapes;
        }

        /**
         * @return The label for the download text.
         */
        public String getSaveDownload() {
            return saveDownload;
        }

        /**
         * @param saveDownload The label for the download text.
         */
        public void setSaveDownload(String saveDownload) {
            this.saveDownload = saveDownload;
        }

        /**
         * @return The label for the save as text.
         */
        public String getSaveAs() {
            return saveAs;
        }

        /**
         * @param saveAs The label for the save as text.
         */
        public void setSaveAs(String saveAs) {
            this.saveAs = saveAs;
        }

        /**
         * @return The label for the result location.
         */
        public String getResultLocationLabel() {
            return resultLocationLabel;
        }

        /**
         * @param resultLocationLabel The label for the result location.
         */
        public void setResultLocationLabel(String resultLocationLabel) {
            this.resultLocationLabel = resultLocationLabel;
        }

        /**
         * @return The tooltip for the content systax.
         */
        public String getContentSyntaxTooltip() {
            return contentSyntaxTooltip;
        }

        /**
         * @param contentSyntaxTooltip The tooltip for the content systax.
         */
        public void setContentSyntaxTooltip(String contentSyntaxTooltip) {
            this.contentSyntaxTooltip = contentSyntaxTooltip;
        }

        /**
         * @return The tooltip for the user-provided shapes.
         */
        public String getExternalRulesTooltip() {
            return externalRulesTooltip;
        }

        /**
         * @param externalRulesTooltip The tooltip for the user-provided shapes.
         */
        public void setExternalRulesTooltip(String externalRulesTooltip) {
            this.externalRulesTooltip = externalRulesTooltip;
        }

        /**
         * @return The label for the download report option.
         */
        public String getOptionDownloadReport() {
            return optionDownloadReport;
        }

        /**
         * @param optionDownloadReport The label for the download report option.
         */
        public void setOptionDownloadReport(String optionDownloadReport) {
            this.optionDownloadReport = optionDownloadReport;
        }

        /**
         * @return The label for the download content option.
         */
        public String getOptionDownloadContent() {
            return optionDownloadContent;
        }

        /**
         * @param optionDownloadContent The label for the download content option.
         */
        public void setOptionDownloadContent(String optionDownloadContent) {
            this.optionDownloadContent = optionDownloadContent;
        }

        /**
         * @return The label for the download shapes option.
         */
        public String getOptionDownloadShapes() {
            return optionDownloadShapes;
        }

        /**
         * @param optionDownloadShapes The label for the download shapes option.
         */
        public void setOptionDownloadShapes(String optionDownloadShapes) {
            this.optionDownloadShapes = optionDownloadShapes;
        }

        /**
         * @return The label for the content syntax.
         */
        public String getContentSyntaxLabel() {
            return contentSyntaxLabel;
        }

        /**
         * @param contentSyntaxLabel The label for the content syntax.
         */
        public void setContentSyntaxLabel(String contentSyntaxLabel) {
            this.contentSyntaxLabel = contentSyntaxLabel;
        }

        /**
         * @return The label for the user-provided shapes.
         */
		public String getExternalShapesLabel() {
			return externalShapesLabel;
		}

        /**
         * @param externalShapesLabel The label for the user-provided shapes.
         */
		public void setExternalShapesLabel(String externalShapesLabel) {
			this.externalShapesLabel = externalShapesLabel;
		}

        /**
         * @return The label for the load imports option.
         */
		public String getLoadImportsLabel() {
			return loadImportsLabel;
		}

        /**
         * @param loadImportsLabel The label for the load imports option.
         */
		public void setLoadImportsLabel(String loadImportsLabel) {
			this.loadImportsLabel = loadImportsLabel;
		}

        /**
         * @return The tooltip for the load imports option.
         */
		public String getLoadImportsTooltip() {
			return loadImportsTooltip;
		}

        /**
         * @param loadImportsTooltip The tooltip for the load imports option.
         */
		public void setLoadImportsTooltip(String loadImportsTooltip) {
			this.loadImportsTooltip = loadImportsTooltip;
		}

    }
}
