package eu.europa.ec.itb.shacl.rest;

import com.gitb.core.ValueEmbeddingEnumeration;
import eu.europa.ec.itb.shacl.*;
import eu.europa.ec.itb.shacl.rest.model.ApiInfo;
import eu.europa.ec.itb.shacl.rest.model.Input;
import eu.europa.ec.itb.shacl.rest.model.Output;
import eu.europa.ec.itb.shacl.rest.model.RuleSet;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import eu.europa.ec.itb.validation.commons.*;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

import static eu.europa.ec.itb.validation.commons.web.Constants.MDC_DOMAIN;

/**
 * REST controller to allow triggering the validator via its REST API.
 */
@Tag(name = "/{domain}/api", description = "Operations for the validation of RDF content based on SHACL shapes.")
@RestController
public class ShaclController {

    @Autowired
    ApplicationContext ctx;
    @Autowired
    ApplicationConfig config;
    @Autowired
    DomainConfigCache domainConfigs;
    @Autowired
    FileManager fileManager;
    @Autowired
    InputHelper inputHelper;

    @Value("${validator.acceptedHeaderAcceptTypes}")
    Set<String> acceptedHeaderAcceptTypes;
    @Value("${validator.hydraServer}")
    String hydraServer;
    @Value("${validator.hydraRootPath}")
    private String hydraRootPath;

    /**
     * Get all domains configured in this validator and their supported validation types.
     *
     * @return A list of domains coupled with their validation types.
     */
    @Operation(summary = "Get API information (all supported domains and validation types).", description="Retrieve the supported domains " +
            "and validation types configured in this validator. These are the domain and validation types that can be used as parameters " +
            "with the API's other operations.")
    @ApiResponse(responseCode = "200", description = "Success", content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = ApiInfo.class))) })
    @ApiResponse(responseCode = "500", description = "Error (If a problem occurred with processing the request)", content = @Content)
    @GetMapping(value = "/api/info", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiInfo[] infoAll() {
        List<DomainConfig> listDomainsConfig = domainConfigs.getAllDomainConfigurations();
        ApiInfo[] listApiInfo = new ApiInfo[listDomainsConfig.size()];

        int i=0;
        for(DomainConfig domainConfig : listDomainsConfig) {
            listApiInfo[i] = ApiInfo.fromDomainConfig(domainConfig);

            i++;
        }

        return listApiInfo;
    }

    /**
     * Get the validation types supported by the current domain.
     *
     * @param domain The domain.
     * @return The list of validation types.
     */
    @Operation(summary = "Get API information (for a given domain).", description = "Retrieve the supported validation types " +
            "that can be requested when calling this API's validation operations.")
    @ApiResponse(responseCode = "200", description = "Success", content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiInfo.class)) })
    @ApiResponse(responseCode = "500", description = "Error (If a problem occurred with processing the request)", content = @Content)
    @ApiResponse(responseCode = "404", description = "Not found (for an invalid domain value)", content = @Content)
    @GetMapping(value = "/{domain}/api/info", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiInfo info(
            @Parameter(required = true, name = "domain", description = "A fixed value corresponding to the specific validation domain.")
            @PathVariable("domain") String domain
    ) {
        DomainConfig domainConfig = validateDomain(domain);
        return ApiInfo.fromDomainConfig(domainConfig);
    }

    /**
     * Service to trigger one validation for the provided input and settings.
     *
     * @param domain The relevant domain for the SHACL validation.
     * @param in The input for the validation.
     * @param request The HTTP request.
     * @return The result of the SHACL validator.
     */
    @Operation(summary = "Validate one RDF instance.", description="Validate a single RDF instance. The content can be provided " +
            "either within the request as a BASE64 encoded string or remotely as a URL. The RDF syntax for the input can be " +
            "determined in the request as can the syntax to produce the resulting SHACL validation report.")
    @ApiResponse(responseCode = "200", description = "Success (for successful validation)", content = @Content)
    @ApiResponse(responseCode = "500", description = "Error (If a problem occurred with processing the request)", content = @Content)
    @ApiResponse(responseCode = "404", description = "Not found (for an invalid domain value)", content = @Content)
    @PostMapping(value = "/{domain}/api/validate", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/ld+json"})
    public ResponseEntity<StreamingResponseBody> validate(
            @Parameter(required = true, name = "domain", description = "A fixed value corresponding to the specific validation domain.")
            @PathVariable("domain") String domain,
            @Parameter(required = true, name = "input", description = "The input for the validation (content and metadata for one RDF instance).")
            @RequestBody Input in,
            HttpServletRequest request
    ) {
        DomainConfig domainConfig = validateDomain(domain);
        String reportSyntax = getValidationReportSyntax(in.getReportSyntax(), getFirstSupportedAcceptHeader(request), domainConfig.getDefaultReportSyntax());
        /*
         * Important: We call executeValidationProcess here and not in the return statement because the StreamingResponseBody
         * uses a separate thread. Doing so would break the ThreadLocal used in the statistics reporting.
         */
        Model report = executeValidationProcess(in, domainConfig);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(reportSyntax))
                .body(outputStream -> fileManager.writeRdfModel(outputStream, report, reportSyntax));
    }

    /**
     * Scan the HTTP request and return the first ACCEPT header that is accepted as a syntax for the produced
     * validation report.
     *
     * @param request The HTTP request.
     * @return The value (or null if not found).
     */
    private String getFirstSupportedAcceptHeader(HttpServletRequest request) {
        Enumeration<String> headerValues = request.getHeaders(HttpHeaders.ACCEPT);
        while (headerValues.hasMoreElements()) {
            String acceptHeader = headerValues.nextElement();
            for (String acceptableValue: acceptedHeaderAcceptTypes) {
                if (acceptHeader.contains(acceptableValue)) {
                    return acceptableValue;
                }
            }
        }
        return null;
    }

    /**
     * Validate the provided value as a validation report syntax.
     *
     * @param reportSyntax The value to check.
     * @return True if valid.
     */
    private boolean validReportSyntax(String reportSyntax) {
        Lang lang = RDFLanguages.contentTypeToLang(reportSyntax.toLowerCase());
        return lang != null;
    }

    /**
     * Returns the report syntax of the content to validate.
     *
     * @param inputReportSyntax Report syntax from the Input data.
     * @param acceptHeader Report syntax from the Header.
     * @param defaultReportSyntax Default report syntax.
     * @return Report syntax as string.
     */
    private String getValidationReportSyntax(String inputReportSyntax, String acceptHeader, String defaultReportSyntax) {
        // Consider first the report syntax requested as part of the input properties.
        String reportSyntax = inputReportSyntax;
        if (reportSyntax == null) {
            // If unspecified consider the first acceptable syntax from the Accept header.
            reportSyntax = acceptHeader;
        }
        if (reportSyntax == null) {
            // No syntax specified by client - use the configuration default.
            reportSyntax = defaultReportSyntax;
        } else {
            if (!validReportSyntax(reportSyntax)) {
                // The requested syntax is invalid.
                throw new ValidatorException("validator.label.exception.reportSyntaxNotSupported", reportSyntax);
            }
        }
        return reportSyntax;
    }

    /**
     * Execute the process to validate the content.
     *
     * @param in The input for the validation (content and metadata for one or more RDF instances).
     * @param domainConfig The domain where the SHACL validator is executed.
     * @return The report's model.
     */
    private Model executeValidationProcess(Input in, DomainConfig domainConfig) {
        // Start validation of the input file
        File parentFolder = fileManager.createTemporaryFolderPath();
        File inputFile;
        String contentSyntax = in.getContentSyntax();
        try {
            // Prepare input
            String validationType = inputHelper.validateValidationType(domainConfig, in.getValidationType());
            List<FileInfo> externalShapes = getExternalShapes(domainConfig, validationType, in.getExternalRules(), parentFolder);
            ValueEmbeddingEnumeration embeddingMethod = inputHelper.getEmbeddingMethod(in.getEmbeddingMethod());
            var queryConfig = in.parseQueryConfig();
            if (queryConfig == null) {
                inputFile = inputHelper.validateContentToValidate(in.getContentToValidate(), embeddingMethod, parentFolder);
            } else {
                queryConfig = inputHelper.validateSparqlConfiguration(domainConfig, queryConfig);
                inputFile = fileManager.getContentFromSparqlEndpoint(queryConfig, parentFolder).toFile();
                contentSyntax = queryConfig.getPreferredContentType();
            }
            Boolean loadImports = inputHelper.validateLoadInputs(domainConfig, in.isLoadImports(), validationType);
            // Execute validation
            SHACLValidator validator = ctx.getBean(SHACLValidator.class, inputFile, validationType, contentSyntax, externalShapes, loadImports, domainConfig, new LocalisationHelper(domainConfig, Utils.getSupportedLocale(LocaleUtils.toLocale(in.getLocale()), domainConfig)));
            ModelPair models = validator.validateAll();
            if (in.getReportQuery() != null && !in.getReportQuery().isBlank()) {
                // Run post-processing query on report and return based on content-type
                Query query = QueryFactory.create(in.getReportQuery());
                try (QueryExecution queryExecution = QueryExecutionFactory.create(query, models.getReportModel())) {
                    return queryExecution.execConstruct();
                }
            } else {
                // Return the validation report according to content-type
                return models.getReportModel();
            }
        } catch (ValidatorException | NotFoundException e) {
            // Localisation of the ValidatorException takes place in the ErrorHandler.
            throw e;
        } catch (Exception e) {
            // Localisation of the ValidatorException takes place in the ErrorHandler.
            throw new ValidatorException(e);
        } finally {
            FileUtils.deleteQuietly(parentFolder);
        }
    }

    /**
     * Validate, store and return the user-provided SHACL shape resources.
     *
     * @param domainConfig The domain configuration.
     * @param validationType The requested validation type.
     * @param externalRules The user-provided shape files.
     * @param parentFolder The temp folder to use for this validation run.
     * @return The list of recorded SHACL shape files to be used.
     */
    private List<FileInfo> getExternalShapes(DomainConfig domainConfig, String validationType, List<RuleSet> externalRules, File parentFolder) {
        List<FileContent> shapeContents = null;
        if (externalRules != null) {
            shapeContents = externalRules.stream().map(RuleSet::toFileContent).collect(Collectors.toList());
        }
        return inputHelper.validateExternalArtifacts(domainConfig, shapeContents, validationType, null, parentFolder);
    }

    /**
     * Validate multiple RDF inputs considering their settings and producing separate SHACL validation reports.
     *
     * @param domain The domain where the SHACL validator is executed. 
     * @param inputs The input for the validation (content and metadata for one or more RDF instances).
     * @param request The HTTP request.
     * @return The validation result.
     */
    @Operation(summary = "Validate multiple RDF instances.", description="Validate multiple RDF instances. " +
            "The content for each instance can be provided either within the request as a BASE64 encoded string or remotely as a URL. " +
            "The RDF syntax for each input can be determined in the request as can the syntax to produce each resulting SHACL validation report.")
    @ApiResponse(responseCode = "200", description = "Success (for successful validation)", content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Output.class))) })
    @ApiResponse(responseCode = "500", description = "Error (If a problem occurred with processing the request)", content = @Content)
    @ApiResponse(responseCode = "404", description = "Not found (for an invalid domain value)", content = @Content)
    @PostMapping(value = "/{domain}/api/validateMultiple", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/ld+json"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Output[] validateMultiple(
            @Parameter(required = true, name = "domain", description = "A fixed value corresponding to the specific validation domain.")
            @PathVariable("domain") String domain,
            @Parameter(required = true, name = "input", description = "The input for the validation (content and metadata for one or more RDF instances).")
            @RequestBody Input[] inputs,
            HttpServletRequest request
    ) {
        DomainConfig domainConfig = validateDomain(domain);
        String acceptHeader = getFirstSupportedAcceptHeader(request);

        Output[] outputs = new Output[inputs.length];
        int i = 0;
        for(Input input: inputs) {
            Output output = new Output();
            String reportSyntax = getValidationReportSyntax(input.getReportSyntax(), acceptHeader, domainConfig.getDefaultReportSyntax());

            //Start validation process of the Input
            Model report = executeValidationProcess(input, domainConfig);
            output.setReport(Base64.getEncoder().encodeToString(fileManager.writeRdfModelToString(report, reportSyntax).getBytes()));
            output.setReportSyntax(reportSyntax);

            outputs[i] = output;

            i++;
        }
        return outputs;
    }

    /**
     * Validates that the domain exists.
     *
     * @param domain The domain identifier provided in the call..
     * @return The matched domain configuration.
     * @throws NotFoundException If the domain does not exist or it does not support a REST API.
     */
    private DomainConfig validateDomain(String domain) {
        DomainConfig domainConfig = domainConfigs.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.isDefined() || !domainConfig.getChannels().contains(ValidatorChannel.REST_API)) {
            throw new NotFoundException(domain);
        }
        MDC.put(MDC_DOMAIN, domain);
        return domainConfig;
    }

    /**
     * Get the Hydra documentation for the service.
     *
     * @param domain The domain configuration in question.
     * @return The documentation.
     * @throws IOException If an error occurs reading the documentation.
     */
    @Operation(hidden = true)
    @GetMapping(value = "/{domain}/api", produces = "application/ld+json")
    public ResponseEntity<String> hydraApi(@PathVariable String domain) throws IOException {
        DomainConfig domainConfig = validateDomain(domain);
        String content = FileUtils.readFileToString(new File(fileManager.getHydraDocsFolder(domainConfig.getDomainName()), "api.jsonld"), Charset.defaultCharset());
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setAccessControlExposeHeaders(Collections.singletonList("Link"));
        // Construct URL for vocabulary
        StringBuilder path = new StringBuilder();
        path.append(hydraServer);
        path.append(hydraRootPath);
        if (path.charAt(path.length()-1) != '/') {
            path.append('/');
        }
        path.append(domainConfig.getDomainName());
        responseHeaders.set("Link", "<"+path.toString()+"/api/vocab>; rel=\"http://www.w3.org/ns/hydra/core#apiDocumentation\"");
        return new ResponseEntity<>(content, responseHeaders, HttpStatus.OK);
    }

    /**
     * Return the Hydra contexts for the API.
     *
     * @param domain The domain configuration.
     * @param contextName The name of the context.
     * @return The documentation.
     * @throws IOException If an error occurs reading the documentation.
     */
    @Operation(hidden = true)
    @GetMapping(value = "/{domain}/api/contexts/{contextName}", produces = "application/ld+json")
    public String hydraContexts(@PathVariable String domain, @PathVariable String contextName) throws IOException {
        DomainConfig domainConfig = validateDomain(domain);
        return FileUtils.readFileToString(new File(fileManager.getHydraDocsFolder(domainConfig.getDomainName()), "EntryPoint.jsonld"), Charset.defaultCharset());
    }

    /**
     * Return the Hydra vocabulary.
     *
     * @param domain The domain configuration.
     * @return The documentation.
     * @throws IOException If an error occurs reading the documentation.
     */
    @Operation(hidden = true)
    @GetMapping(value = "/{domain}/api/vocab", produces = "application/ld+json")
    public String hydraVocab(@PathVariable String domain) throws IOException {
        DomainConfig domainConfig = validateDomain(domain);
        return FileUtils.readFileToString(new File(fileManager.getHydraDocsFolder(domainConfig.getDomainName()), "vocab.jsonld"), Charset.defaultCharset());
    }

}
