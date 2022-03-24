package eu.europa.ec.itb.shacl.rest.model;

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

    @Schema(description = "The RDF content to validate, provided as a normal string, a URL, or a BASE64-encoded string. Either this must be provided or a SPARQL query (contentQuery).")
    private String contentToValidate;
    @Schema(description = "The mime type of the provided RDF content (e.g. \"application/rdf+xml\", \"application/ld+json\", \"text/turtle\"). If not provided the type is determined from the provided content (if possible).")
    private String contentSyntax;
    @Schema(description = "The way in which to interpret the contentToValidate. If not provided, the method will be determined from the contentToValidate value.", allowableValues = FileContent.EMBEDDING_STRING+","+FileContent.EMBEDDING_URL+","+FileContent.EMBEDDING_BASE_64)
    private String embeddingMethod;
    @Schema(description = "The type of validation to perform (e.g. the profile to apply or the version to validate against). This can be skipped if a single validation type is supported by the validator. Otherwise, if multiple are supported, the service should fail with an error.")
    private String validationType;
    @Schema(description = "A SPARQL CONSTRUCT query that will be executed on the resulting SHACL validation report as a post-processing step. If provided, the result of this query will replace the SHACL validation report in the service's output.")
    private String reportQuery;
    @Schema(description = "The mime type for the validation report syntax (e.g. \"application/ld+json\", \"application/rdf+xml\", \"text/turtle\", \"application/n-triples\"). If none is provided \"application/rdf+xml\" is considered as the default, unless a different syntax is configured for the domain in question.")
    private String reportSyntax;
    @Schema(description = "Any shapes to consider that are externally provided (i.e. provided at the time of the call).")
    private List<RuleSet> externalRules;
    @Schema(description = "If owl:Imports should be loaded from the RDF content. This can be skipped if defined in the configuration. If not provided, the decision is determined from the configuration for the domain in question.")
    private Boolean loadImports;
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

    /**
     * @return The string representing the content to validate (string as-is, URL or base64 content).
     */
    public String getContentToValidate() { return this.contentToValidate; }

    /**
     * @return The embedding method to consider to determine how the provided content input is to be processed.
     */
    public String getEmbeddingMethod() { return this.embeddingMethod; }

    /**
     * @return The validation type to trigger for this domain.
     */
    public String getValidationType() { return this.validationType; }

    /**
     * @return The syntax (mime type) to use for the produced SHACL validation report.
     */
    public String getReportSyntax() { return this.reportSyntax; }

    /**
     * @return The syntax (mime type) to consider for the provided input to validate.
     */
    public String getContentSyntax() { return this.contentSyntax; }

    /**
     * @return The set of user-provided SHACL shape riles with additional business rules.
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
     * @param contentToValidate The string representing the content to validate (string as-is, URL or base64 content).
     */
    public void setContentToValidate(String contentToValidate) {
        this.contentToValidate = contentToValidate;
    }

    /**
     * @param contentSyntax The syntax (mime type) to consider for the provided input to validate.
     */
    public void setContentSyntax(String contentSyntax) {
        this.contentSyntax = contentSyntax;
    }

    /**
     * @param embeddingMethod  The embedding method to consider to determine how the provided content input is to be processed.
     */
    public void setEmbeddingMethod(String embeddingMethod) {
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
     * Parse the SPARQL query configuration from the provided individual parameters.
     *
     * @return The SPARQL query configuration.
     */
    public SparqlQueryConfig parseQueryConfig() {
        SparqlQueryConfig config = null;
        if (contentQuery != null || contentQueryEndpoint != null || contentQueryPassword != null || contentQueryUsername != null) {
            config = new SparqlQueryConfig(contentQueryEndpoint, contentQuery, contentQueryUsername, contentQueryPassword, contentSyntax);
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
}
