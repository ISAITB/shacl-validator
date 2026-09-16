/*
 * Copyright (C) 2026 European Union
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

package eu.europa.ec.itb.shacl.rest.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eu.europa.ec.itb.shacl.SparqlQueryConfig;
import eu.europa.ec.itb.validation.commons.FileContent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The input to trigger a new validation via the validator's REST API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "The content and metadata specific to input content that is to be validated.")
public class Input {

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @Schema(description = "The RDF content to validate, provided as a normal string, a URL, or a BASE64-encoded string. Either this must be provided or a SPARQL query (contentQuery). A single value or an array of values may be provided; if more than one value is provided the resulting RDF graphs are merged into one before validation.", oneOf = {String.class, String[].class})
    private List<String> contentToValidate;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @Schema(description = "The mime type of the provided RDF content (e.g. \"application/rdf+xml\", \"application/ld+json\", \"text/turtle\"). If not provided the type is determined from the provided content (if possible). This can be a single value (applied to all contentToValidate items) or an array matching the number of contentToValidate items.", oneOf = {String.class, String[].class})
    private List<String> contentSyntax;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @Schema(description = "The way in which to interpret the contentToValidate. If not provided, the method will be determined from the contentToValidate value. This can be a single value (applied to all contentToValidate items) or an array matching the number of contentToValidate items.", allowableValues = FileContent.EMBEDDING_STRING+","+FileContent.EMBEDDING_URL+","+FileContent.EMBEDDING_BASE_64, oneOf = {String.class, String[].class})
    private List<String> embeddingMethod;
    @Schema(description = "The type of validation to perform (e.g. the profile to apply or the version to validate against). This can be skipped if a single validation type is supported by the validator. Otherwise, if multiple are supported, the service should fail with an error.")
    private String validationType;
    @Schema(description = "A SPARQL CONSTRUCT query that will be executed on the resulting SHACL validation report as a post-processing step. If provided, the result of this query will replace the SHACL validation report in the service's output.")
    private String reportQuery;
    @Schema(description = "The mime type for the validation report syntax. Providing an RDF mime type (\"application/ld+json\", \"application/rdf+xml\", \"text/turtle\", \"application/n-triples\") will produce a SHACL validation report. Specifying \"application/xml\", \"text/xml\" or \"application/json\" will produce the report in the GITB TRL syntax (in XML or JSON). If no syntax is provided \"application/rdf+xml\" is considered as the default, unless a different syntax is configured for the domain in question.")
    private String reportSyntax;
    @Schema(description = "Any shapes to consider that are externally provided (i.e. provided at the time of the call).")
    private List<RuleSet> externalRules;
    @Schema(description = "If owl:Imports should be loaded from the RDF content. This can be skipped if defined in the configuration. If not provided, the decision is determined from the configuration for the domain in question.")
    private Boolean loadImports;
    @Schema(description = "If shape and input models should be merged before validation. This can be skipped if defined in the configuration. If not provided, the decision is determined from the configuration for the domain in question.")
    private Boolean mergeModelsBeforeValidation;
    @Schema(description = "The SPARQL endpoint URI.")
    private String contentQueryEndpoint;
    @Schema(description = "The SPARQL query to execute.")
    private String contentQuery;
    @Schema(description = "Username to access the SPARQL endpoint.")
    private String contentQueryUsername;
    @Schema(description = "Password to access the SPARQL endpoint.")
    private String contentQueryPassword;
    @Schema(description = "Locale (language code) to use for reporting of results. If the provided locale is not supported by the validator the default locale will be used instead (e.g. 'fr', 'fr_FR').")
    private String locale;
    @Schema(description = "In case a GITB TRL report is requested (see reportSyntax), whether to include the validated input in the resulting report's context section. If returning a SHACL validation report this input is ignored.", defaultValue = "false")
    private Boolean addInputToReport;
    @Schema(description = "In case a GITB TRL report is requested (see reportSyntax), whether to include the SHACL shapes in the resulting report's context section. If returning a SHACL validation report this input is ignored.", defaultValue = "false")
    private Boolean addShapesToReport;
    @Schema(description = "In case a GITB TRL report is requested (see reportSyntax), whether to include the SHACL validation report in the resulting GITB TRL report's context section. If returning a SHACL validation report this input is ignored.", defaultValue = "false")
    private Boolean addRdfReportToReport;
    @Schema(description = "In case a GITB TRL report is requested (see reportSyntax), and the SHACL validation report is set to be included in GITB TRL report's context section, this is the mime type to use for the SHACL validation report. If returning a SHACL validation report this input is ignored.")
    private String rdfReportSyntax;
    @Schema(description = "Whether to wrap the data added to the report (see addInputToReport, addShapesToReport and addRdfReportToReport) in CDATA blocks if producing it in the XML GITB TRL format. False results in adding data via XML escaping.", defaultValue = "false")
    private Boolean wrapReportDataInCDATA;

    /**
     * @return The strings representing the content(s) to validate (string as-is, URL or base64 content).
     */
    public List<String> getContentToValidate() { return this.contentToValidate; }

    /**
     * @return The embedding method(s) to consider to determine how the provided content input(s) are to be processed.
     */
    public List<String> getEmbeddingMethod() { return this.embeddingMethod; }

    /**
     * @return The validation type to trigger for this domain.
     */
    public String getValidationType() { return this.validationType; }

    /**
     * @return The syntax (mime type) to use for the produced SHACL validation report.
     */
    public String getReportSyntax() { return this.reportSyntax; }

    /**
     * @return The syntax(es) (mime type) to consider for the provided input(s) to validate.
     */
    public List<String> getContentSyntax() { return this.contentSyntax; }

    /**
     * @return The set of user-provided SHACL shape files with additional business rules.
     */
    public List<RuleSet> getExternalRules(){ return this.externalRules; }

    /**
     * Get a specific user-provided shape file.
     *
     * @param value The index of the provided shape file.
     * @return The shape file.
     */
    public RuleSet getExternalRules(int value) { return this.externalRules.get(value); }

    /**
     * @return True if OWL imports are to be loaded from the content to validate.
     */
    public Boolean isLoadImports(){ return this.loadImports; }

    /**
     * @return True if shape and input models should be merged before validation.
     */
    public Boolean isMergeModelsBeforeValidation(){ return this.mergeModelsBeforeValidation; }

    /**
     * @param contentToValidate The string(s) representing the content(s) to validate (string as-is, URL or base64 content).
     */
    public void setContentToValidate(List<String> contentToValidate) {
        this.contentToValidate = contentToValidate;
    }

    /**
     * @param contentSyntax The syntax(es) (mime type) to consider for the provided input(s) to validate.
     */
    public void setContentSyntax(List<String> contentSyntax) {
        this.contentSyntax = contentSyntax;
    }

    /**
     * @param embeddingMethod The embedding method(s) to consider to determine how the provided content input(s) are to be processed.
     */
    public void setEmbeddingMethod(List<String> embeddingMethod) {
        this.embeddingMethod = embeddingMethod;
    }

    /**
     * @param validationType The validation type to trigger for this domain.
     */
    public void setValidationType(String validationType) {
        this.validationType = validationType;
    }

    /**
     * @param reportSyntax The syntax (mime type) to use for the produced SHACL validation report.
     */
    public void setReportSyntax(String reportSyntax) {
        this.reportSyntax = reportSyntax;
    }

    /**
     * @param externalRules The set of user-provided SHACL shape riles with additional business rules.
     */
    public void setExternalRules(List<RuleSet> externalRules) {
        this.externalRules = externalRules;
    }

    /**
     * @param loadImports True if OWL imports are to be loaded from the content to validate.
     */
    public void setLoadImports(Boolean loadImports) {
        this.loadImports = loadImports;
    }

    /**
     * @param mergeModelsBeforeValidation True if the shape and input models should be merged before validation.
     */
    public void setMergeModelsBeforeValidation(Boolean mergeModelsBeforeValidation) {
        this.mergeModelsBeforeValidation = mergeModelsBeforeValidation;
    }

    /**
     * @return The SPARQL endpoint URL for querying the content to validate.
     */
    public String getContentQueryEndpoint() {
        return contentQueryEndpoint;
    }

    /**
     * @param contentQueryEndpoint The SPARQL endpoint URL for querying the content to validate.
     */
    public void setContentQueryEndpoint(String contentQueryEndpoint) {
        this.contentQueryEndpoint = contentQueryEndpoint;
    }

    /**
     * @return The SPARQL query to use to retrieve the content to validate.
     */
    public String getContentQuery() {
        return contentQuery;
    }

    /**
     * @param contentQuery The SPARQL query to use to retrieve the content to validate.
     */
    public void setContentQuery(String contentQuery) {
        this.contentQuery = contentQuery;
    }

    /**
     * @return The username to use for authentication against the SPARQL endpoint.
     */
    public String getContentQueryUsername() {
        return contentQueryUsername;
    }

    /**
     * @param contentQueryUsername The username to use for authentication against the SPARQL endpoint.
     */
    public void setContentQueryUsername(String contentQueryUsername) {
        this.contentQueryUsername = contentQueryUsername;
    }

    /**
     * @return The password to use for authentication against the SPARQL endpoint.
     */
    public String getContentQueryPassword() {
        return contentQueryPassword;
    }

    /**
     * @param contentQueryPassword The password to use for authentication against the SPARQL endpoint.
     */
    public void setContentQueryPassword(String contentQueryPassword) {
        this.contentQueryPassword = contentQueryPassword;
    }

    /**
     * @return The SPARQL query to use for post-processing of the produced SHACL validation report.
     */
    public String getReportQuery() {
        return reportQuery;
    }

    /**
     * @param reportQuery The SPARQL query to use for post-processing of the produced SHACL validation report.
     */
    public void setReportQuery(String reportQuery) {
        this.reportQuery = reportQuery;
    }

    /**
     * @return Whether to include the validated input in the resulting report's context section (when returning a GITB TRL report).
     */
    public Boolean getAddInputToReport() {
        return addInputToReport;
    }

    /**
     * @param addInputToReport Whether to include the validated input in the resulting report's context section (when returning a GITB TRL report).
     */
    public void setAddInputToReport(Boolean addInputToReport) {
        this.addInputToReport = addInputToReport;
    }

    /**
     * @return Whether to include the SHACL shapes in the resulting report's context section (when returning a GITB TRL report).
     */
    public Boolean getAddShapesToReport() {
        return addShapesToReport;
    }

    /**
     * @param addShapesToReport Whether to include the SHACL shapes in the resulting report's context section (when returning a GITB TRL report).
     */
    public void setAddShapesToReport(Boolean addShapesToReport) {
        this.addShapesToReport = addShapesToReport;
    }

    /**
     * @return Whether to include the SHACL validation report in the resulting GITB TRL report's context section (when returning a GITB TRL report).
     */
    public Boolean getAddRdfReportToReport() {
        return addRdfReportToReport;
    }

    /**
     * @param addRdfReportToReport Whether to include the SHACL validation report in the resulting GITB TRL report's context section (when returning a GITB TRL report).
     */
    public void setAddRdfReportToReport(Boolean addRdfReportToReport) {
        this.addRdfReportToReport = addRdfReportToReport;
    }

    /**
     * @return The mime type to use for the SHACL validation report if the overall report returned is a GITB TRL report, and
     * we have selected to include the SHACL validation report in the GITB TRL report's context section.
     */
    public String getRdfReportSyntax() {
        return rdfReportSyntax;
    }

    /**
     * @param rdfReportSyntax The mime type to use for the SHACL validation report if the overall report returned is a GITB TRL report, and
     * we have selected to include the SHACL validation report in the GITB TRL report's context section.
     */
    public void setRdfReportSyntax(String rdfReportSyntax) {
        this.rdfReportSyntax = rdfReportSyntax;
    }

    /**
     * Parse the SPARQL query configuration from the provided individual parameters.
     *
     * @return The SPARQL query configuration.
     */
    public SparqlQueryConfig parseQueryConfig() {
        SparqlQueryConfig config = null;
        if (contentQuery != null || contentQueryEndpoint != null || contentQueryPassword != null || contentQueryUsername != null) {
            String preferredContentType = (contentSyntax == null || contentSyntax.isEmpty()) ? null : contentSyntax.get(0);
            config = new SparqlQueryConfig(contentQueryEndpoint, contentQuery, contentQueryUsername, contentQueryPassword, preferredContentType);
        }
        return config;
    }

    /**
     * @return The locale string.
     */
    public String getLocale() {
        return locale;
    }

    /**
     * @param locale The locale string to set.
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * @return Whether to wrap the data added to the report (see addInputToReport, addShapesToReport and addRdfReportToReport) in CDATA blocks if producing it in the XML GITB TRL format. False results in adding data via XML escaping.
     */
    public Boolean getWrapReportDataInCDATA() {
        return wrapReportDataInCDATA;
    }

    /**
     * @param wrapReportDataInCDATA Whether to wrap the data added to the report (see addInputToReport, addShapesToReport and addRdfReportToReport) in CDATA blocks if producing it in the XML GITB TRL format. False results in adding data via XML escaping.
     */
    public void setWrapReportDataInCDATA(Boolean wrapReportDataInCDATA) {
        this.wrapReportDataInCDATA = wrapReportDataInCDATA;
    }

}
