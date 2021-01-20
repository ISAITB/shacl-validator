package eu.europa.ec.itb.shacl;

import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.LabelConfig;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;

import java.util.List;
import java.util.Map;

/**
 * Created by simatosc on 21/03/2016.
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

    public ExternalArtifactSupport getQueryAuthentication() {
        return queryAuthentication;
    }

    public void setQueryAuthentication(ExternalArtifactSupport queryAuthentication) {
        this.queryAuthentication = queryAuthentication;
    }

    public boolean supportsExternalArtifacts() {
        for (TypedValidationArtifactInfo info: getArtifactInfo().values()) {
            if (info.getOverallExternalArtifactSupport() != ExternalArtifactSupport.NONE) {
                return true;
            }
        }
        return false;
    }

    public boolean supportsUserProvidedLoadImports() {
        for (ExternalArtifactSupport supportType: getUserInputForLoadImportsType().values()) {
            if (supportType != ExternalArtifactSupport.NONE) {
                return true;
            }
        }
        return false;
    }

    public ValidationArtifactInfo getShapeInfo(String validationType) {
        return getArtifactInfo().get(validationType).get();
    }

    public boolean isSupportsQueries() {
        return supportsQueries;
    }

    public boolean isQueryAuthenticationMandatory() {
        return supportsQueries && queryAuthentication == ExternalArtifactSupport.REQUIRED;
    }

    public boolean supportsQueryCredentials() {
        return supportsQueries && queryAuthentication != ExternalArtifactSupport.NONE;
    }

    public void setSupportsQueries(boolean supportsQueries) {
        this.supportsQueries = supportsQueries;
    }

    @Override
    protected Label newLabelConfig() {
        return new Label();
    }

    public String getDefaultReportSyntax() {
        return defaultReportSyntax;
    }

    public void setDefaultReportSyntax(String defaultReportSyntax) {
        this.defaultReportSyntax = defaultReportSyntax;
    }

	public boolean isReportsOrdered() {
        return reportsOrdered;
    }

    public void setReportsOrdered(boolean reportsOrdered) {
        this.reportsOrdered = reportsOrdered;
    }
	
    public Map<String, Boolean> getDefaultLoadImportsType() {
		return defaultLoadImportsType;
	}

	public void setDefaultLoadImportsType(Map<String, Boolean> loadImportsType) {
		this.defaultLoadImportsType = loadImportsType;
	}

    public boolean isMergeModelsBeforeValidation() {
		return mergeModelsBeforeValidation;
	}

	public void setMergeModelsBeforeValidation(boolean mergeModelsBeforeValidation) {
		this.mergeModelsBeforeValidation = mergeModelsBeforeValidation;
	}

	public Map<String, ExternalArtifactSupport> getUserInputForLoadImportsType() {
		return userInputForLoadImportsType;
	}

	public void setUserInputForLoadImportsType(Map<String, ExternalArtifactSupport> userInputForLoadImportsType) {
		this.userInputForLoadImportsType = userInputForLoadImportsType;
	}

	public List<String> getWebContentSyntax() {
		return webContentSyntax;
	}

	public void setWebContentSyntax(List<String> webContentSyntax) {
		this.webContentSyntax = webContentSyntax;
	}

	public String getQueryEndpoint() {
		return queryEndpoint;
	}

	public void setQueryEndpoint(String queryEndpoint) {
		this.queryEndpoint = queryEndpoint;
	}

	public String getQueryUsername() {
		return queryUsername;
	}

	public void setQueryUsername(String queryUsername) {
		this.queryUsername = queryUsername;
	}

	public String getQueryPassword() {
		return queryPassword;
	}

	public void setQueryPassword(String queryPassword) {
		this.queryPassword = queryPassword;
	}

	public String getQueryContentType() {
		return queryContentType;
	}

	public void setQueryContentType(String queryContentType) {
		this.queryContentType = queryContentType;
	}

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

        public String getOptionContentQuery() {
            return optionContentQuery;
        }

        public void setOptionContentQuery(String optionContentQuery) {
            this.optionContentQuery = optionContentQuery;
        }

        public String getQueryEndpointInputPlaceholder() {
            return queryEndpointInputPlaceholder;
        }

        public void setQueryEndpointInputPlaceholder(String queryEndpointInputPlaceholder) {
            this.queryEndpointInputPlaceholder = queryEndpointInputPlaceholder;
        }

        public String getQueryUsernameInputPlaceholder() {
            return queryUsernameInputPlaceholder;
        }

        public void setQueryUsernameInputPlaceholder(String queryUsernameInputPlaceholder) {
            this.queryUsernameInputPlaceholder = queryUsernameInputPlaceholder;
        }

        public String getQueryPasswordInputPlaceholder() {
            return queryPasswordInputPlaceholder;
        }

        public void setQueryPasswordInputPlaceholder(String queryPasswordInputPlaceholder) {
            this.queryPasswordInputPlaceholder = queryPasswordInputPlaceholder;
        }

        public String getQueryAuthenticateLabel() {
            return queryAuthenticateLabel;
        }

        public void setQueryAuthenticateLabel(String queryAuthenticateLabel) {
            this.queryAuthenticateLabel = queryAuthenticateLabel;
        }

        public String getReportItemFocusNode() {
            return reportItemFocusNode;
        }

        public void setReportItemFocusNode(String reportItemFocusNode) {
            this.reportItemFocusNode = reportItemFocusNode;
        }

        public String getReportItemResultPath() {
            return reportItemResultPath;
        }

        public void setReportItemResultPath(String reportItemResultPath) {
            this.reportItemResultPath = reportItemResultPath;
        }

        public String getReportItemShape() {
            return reportItemShape;
        }

        public void setReportItemShape(String reportItemShape) {
            this.reportItemShape = reportItemShape;
        }

        public String getReportItemValue() {
            return reportItemValue;
        }

        public void setReportItemValue(String reportItemValue) {
            this.reportItemValue = reportItemValue;
        }

        public String getIncludeExternalShapes() {
            return includeExternalShapes;
        }

        public void setIncludeExternalShapes(String includeExternalShapes) {
            this.includeExternalShapes = includeExternalShapes;
        }

        public String getSaveDownload() {
            return saveDownload;
        }

        public void setSaveDownload(String saveDownload) {
            this.saveDownload = saveDownload;
        }

        public String getSaveAs() {
            return saveAs;
        }

        public void setSaveAs(String saveAs) {
            this.saveAs = saveAs;
        }

        public String getResultLocationLabel() {
            return resultLocationLabel;
        }

        public void setResultLocationLabel(String resultLocationLabel) {
            this.resultLocationLabel = resultLocationLabel;
        }

        public String getContentSyntaxTooltip() {
            return contentSyntaxTooltip;
        }

        public void setContentSyntaxTooltip(String contentSyntaxTooltip) {
            this.contentSyntaxTooltip = contentSyntaxTooltip;
        }

        public String getExternalRulesTooltip() {
            return externalRulesTooltip;
        }

        public void setExternalRulesTooltip(String externalRulesTooltip) {
            this.externalRulesTooltip = externalRulesTooltip;
        }

        public String getOptionDownloadReport() {
            return optionDownloadReport;
        }

        public void setOptionDownloadReport(String optionDownloadReport) {
            this.optionDownloadReport = optionDownloadReport;
        }

        public String getOptionDownloadContent() {
            return optionDownloadContent;
        }

        public void setOptionDownloadContent(String optionDownloadContent) {
            this.optionDownloadContent = optionDownloadContent;
        }

        public String getOptionDownloadShapes() {
            return optionDownloadShapes;
        }

        public void setOptionDownloadShapes(String optionDownloadShapes) {
            this.optionDownloadShapes = optionDownloadShapes;
        }

        public String getContentSyntaxLabel() {
            return contentSyntaxLabel;
        }

        public void setContentSyntaxLabel(String contentSyntaxLabel) {
            this.contentSyntaxLabel = contentSyntaxLabel;
        }

		public String getExternalShapesLabel() {
			return externalShapesLabel;
		}

		public void setExternalShapesLabel(String externalShapesLabel) {
			this.externalShapesLabel = externalShapesLabel;
		}

		public String getLoadImportsLabel() {
			return loadImportsLabel;
		}

		public void setLoadImportsLabel(String loadImportsLabel) {
			this.loadImportsLabel = loadImportsLabel;
		}

		public String getLoadImportsTooltip() {
			return loadImportsTooltip;
		}

		public void setLoadImportsTooltip(String loadImportsTooltip) {
			this.loadImportsTooltip = loadImportsTooltip;
		}


    }
}
