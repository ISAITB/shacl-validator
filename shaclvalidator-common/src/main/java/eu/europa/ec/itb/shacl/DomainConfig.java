/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.shacl;

import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.ErrorResponseTypeEnum;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The configuration for a specific validation domain.
 */
public class DomainConfig extends WebDomainConfig {

    public static final String FILE_NAME_SHAPES = "shapesFile";

    private String defaultReportSyntax;
    private Map<String, Boolean> defaultLoadImportsType;
    private Map<String, Boolean> defaultMergeModelsType;
    private Map<String, ExternalArtifactSupport> userInputForLoadImportsType;
    private Map<String, ExternalArtifactSupport> userInputForMergeModelsType;
    private List<String> webContentSyntax;
    private boolean supportsQueries;
    private String queryEndpoint;
    private ExternalArtifactSupport queryAuthentication;
    private String queryUsername;
    private String queryPassword;
    private String queryContentType;
    private boolean returnMessagesForAllLocales;
    private Map<String, ErrorResponseTypeEnum> importedShapeErrorResponse;
    private Set<String> urisToIgnoreForImportErrors;
    private Set<String> urisToSkipWhenImporting;
    private Map<String, Path> owlImportMappings;
    private Map<String, Boolean> preloadImports;
    private Map<String, Boolean> preloadShapeGraph;
    private Map<String, Boolean> hideDownloadShapes;

    /**
     * Check to see if the shapes report can be cached.
     * <p>
     * We check this because a SHACL shape model can be quite large and take time to serialise.
     * We can cache this report for a given validation type if no user-provided shapes are
     * supported and the type does not define remotely loaded shapes.
     *
     * @param validationType The validation type to check for.
     * @return The check result.
     */
    public boolean canCacheShapes(String validationType) {
        TypedValidationArtifactInfo info = getArtifactInfo().get(validationType);
        return info != null
                && info.getOverallExternalArtifactSupport() == ExternalArtifactSupport.NONE
                && !info.hasRemoteArtifacts();
    }

    /**
     * Check to see whether any validation types are set for OWL import preloading.
     *
     * @return The check result.
     */
    public boolean isPreloadingImportsForAnyType() {
        return preloadImports != null && preloadImports.values().stream().anyMatch(preload -> preload);
    }

    /**
     * Check to see whether any validation types are set for SHACL shape graph preloading.
     *
     * @return The check result.
     */
    public boolean isPreloadingShapeGraphsForAnyType() {
        return preloadShapeGraph != null && preloadShapeGraph.values().stream().anyMatch(preload -> preload);
    }

    /**
     * Check to see whether OWL imports should be preloaded for the provided (full) validation type.
     *
     * @param validationType The validation type.
     * @return Whether imports should be preloaded.
     */
    public boolean preloadImportsForType(String validationType) {
        if (preloadImports != null) {
            return preloadImports.getOrDefault(validationType, false);
        }
        return false;
    }

    /**
     * Check to see whether the SHACL shape graph should be preloaded for the provided (full) validation type.
     *
     * @param validationType The validation type.
     * @return Whether the graph should be preloaded.
     */
    public boolean preloadShapeGraphForType(String validationType) {
        if (preloadShapeGraph != null) {
            return preloadShapeGraph.getOrDefault(validationType, false);
        }
        return false;
    }

    /**
     * @param preloadImports Set the configuration of validation types to whether OWL imports are preloaded.
     */
    public void setPreloadImports(Map<String, Boolean> preloadImports) {
        this.preloadImports = preloadImports;
    }

    /**
     * Check to see whether the download shapes button should be hidden for the provided (full) validation type.
     *
     * @param validationType The validation type.
     * @return Whether the download shapes button should be hidden.
     */
    public boolean hideDownloadShapesForType(String validationType) {
        if (hideDownloadShapes != null) {
            return hideDownloadShapes.getOrDefault(validationType, false);
        }
        return false;
    }

    /**
     * @param hideDownloadShapes Set the configuration of validation types to whether the download shapes UI button is displayed.
     */
    public void setHideDownloadShapes(Map<String, Boolean> hideDownloadShapes) {
        this.hideDownloadShapes = hideDownloadShapes;
    }

    /**
     * @param preloadShapeGraph Set the configuration of validation types to whether SHACL shape graphs are preloaded.
     */
    public void setPreloadShapeGraph(Map<String, Boolean> preloadShapeGraph) {
        this.preloadShapeGraph = preloadShapeGraph;
    }

    /**
     * @return The mapping of URIs to local files used in owl:imports.
     */
    public Map<String, Path> getOwlImportMappings() {
        return owlImportMappings;
    }

    /**
     * @param owlImportMappings The mapping of URIs to local files used in owl:imports.
     */
    public void setOwlImportMappings(Map<String, Path> owlImportMappings) {
        this.owlImportMappings = owlImportMappings;
    }

    /**
     * @return The URIs to ignore for import errors.
     */
    public Set<String> getUrisToIgnoreForImportErrors() {
        return urisToIgnoreForImportErrors;
    }

    /**
     * @param urisToIgnoreForImportErrors The URIs to ignore for import errors.
     */
    public void setUrisToIgnoreForImportErrors(Set<String> urisToIgnoreForImportErrors) {
        this.urisToIgnoreForImportErrors = urisToIgnoreForImportErrors;
    }

    /**
     * @return The URIs that should be skipped when found in owl:imports.
     */
    public Set<String> getUrisToSkipWhenImporting() {
        return urisToSkipWhenImporting;
    }

    /**
     * @param urisToSkipWhenImporting The URIs that should be skipped when found in owl:imports.
     */
    public void setUrisToSkipWhenImporting(Set<String> urisToSkipWhenImporting) {
        this.urisToSkipWhenImporting = urisToSkipWhenImporting;
    }

    /**
     * Check how to react to a failure when loading shapes imported via owl:imports.
     *
     * @param validationType The validation type to check for.
     * @return The reaction type.
     */
    public ErrorResponseTypeEnum getResponseForImportedShapeFailure(String validationType) {
        if (validationType == null) {
            validationType = getDefaultType();
        }
        if (validationType == null) {
            return ErrorResponseTypeEnum.LOG;
        } else {
            return importedShapeErrorResponse.computeIfAbsent(validationType, key -> ErrorResponseTypeEnum.LOG);
        }
    }

    /**
     * @param importedShapeErrorResponse The configuration per (full) validation type of how to respond to owl:import errors.
     */
    public void setImportedShapeErrorResponse(Map<String, ErrorResponseTypeEnum> importedShapeErrorResponse) {
        this.importedShapeErrorResponse = importedShapeErrorResponse;
    }

    /**
     * @return Whether messages for all defined locales should be included in the SHACL validation report.
     */
    public boolean isReturnMessagesForAllLocales() {
        return returnMessagesForAllLocales;
    }

    /**
     * @param returnMessagesForAllLocales Whether messages for all defined locales should be included in the SHACL validation report.
     */
    public void setReturnMessagesForAllLocales(boolean returnMessagesForAllLocales) {
        this.returnMessagesForAllLocales = returnMessagesForAllLocales;
    }

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
     * @return True if users are allowed to specify whether shape and input models are merged before validation.
     */
    public boolean supportsUserProvidedMergeModelsBeforeValidation() {
        for (ExternalArtifactSupport supportType: getUserInputForMergeModelsType().values()) {
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
         * @return The mapping of validation type to whether the input graph should be merged with the shape graph before validation.
     */
    public Map<String, Boolean> getDefaultMergeModelsType() {
        return defaultMergeModelsType;
    }

    /**
     * @param mergeModelsType The mapping of validation type to whether the input graph should be merged with the shape graph before validation.
     */
    public void setDefaultMergeModelsType(Map<String, Boolean> mergeModelsType) {
        this.defaultMergeModelsType = mergeModelsType;
    }

    /**
     * @return The mapping from validation type to the support level for users providing the flag on whether the SHACL
     * shape graph should be merged with the input before validation.
     */
    public Map<String, ExternalArtifactSupport> getUserInputForMergeModelsType() {
        return userInputForMergeModelsType;
    }

    /**
     * @param userInputForMergeModelsType The mapping from validation type to the support level for users providing the
     *                                    flag on whether the SHACL shape graph should be merged with the input before validation.
     */
    public void setUserInputForMergeModelsType(Map<String, ExternalArtifactSupport> userInputForMergeModelsType) {
        this.userInputForMergeModelsType = userInputForMergeModelsType;
    }

    /**
     * @return The mapping from validation type to the support level for users providing the flag on whether OWL
     * imports should be loaded from the input.
     */
	public Map<String, ExternalArtifactSupport> getUserInputForLoadImportsType() {
		return userInputForLoadImportsType;
	}

    /**
     * @param userInputForLoadImportsType The mapping from validation type to the support level for users providing the
     *                                    flag on whether OWL imports should be loaded from the input.
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
