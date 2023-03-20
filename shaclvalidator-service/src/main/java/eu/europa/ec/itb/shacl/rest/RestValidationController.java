package eu.europa.ec.itb.shacl.rest;

import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.TAR;
import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.InputHelper;
import eu.europa.ec.itb.shacl.ModelPair;
import eu.europa.ec.itb.shacl.rest.model.Input;
import eu.europa.ec.itb.shacl.rest.model.Output;
import eu.europa.ec.itb.shacl.rest.model.RuleSet;
import eu.europa.ec.itb.shacl.util.ShaclValidatorUtils;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.ReportSpecs;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import eu.europa.ec.itb.validation.commons.FileContent;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import eu.europa.ec.itb.validation.commons.web.rest.BaseRestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.riot.RDFLanguages;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller to allow triggering the validator via its REST API.
 */
@Tag(name = "/{domain}/api", description = "Operations for the validation of RDF content based on SHACL shapes.")
@RestController
public class RestValidationController extends BaseRestController<DomainConfig, ApplicationConfig, FileManager, InputHelper> {

    @Autowired
    ApplicationContext ctx;
    @Autowired
    FileManager fileManager;

    @Value("${validator.acceptedHeaderAcceptTypes}")
    Set<String> acceptedHeaderAcceptTypes;
    @Value("${validator.hydraServer}")
    String hydraServer;
    @Value("${validator.hydraRootPath}")
    private String hydraRootPath;

    @Override
    protected Set<String> getSupportedReportTypes() {
        return acceptedHeaderAcceptTypes;
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
    @ApiResponse(responseCode = "200", description = "Success (for successful validation)", content = { @Content(mediaType = "application/ld+json"), @Content(mediaType = "application/rdf+xml"), @Content(mediaType = "text/turtle"), @Content(mediaType = "application/n-triples"), @Content(mediaType = MediaType.APPLICATION_XML_VALUE), @Content(mediaType = MediaType.APPLICATION_JSON_VALUE) })
    @ApiResponse(responseCode = "500", description = "Error (If a problem occurred with processing the request)", content = @Content)
    @ApiResponse(responseCode = "404", description = "Not found (for an invalid domain value)", content = @Content)
    @PostMapping(value = "/{domain}/api/validate", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/ld+json"}, produces = { "application/ld+json", "application/rdf+xml", "text/turtle", "application/n-triples", MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<StreamingResponseBody> validate(
            @Parameter(required = true, name = "domain", description = "A fixed value corresponding to the specific validation domain.")
            @PathVariable("domain") String domain,
            @Parameter(required = true, name = "input", description = "The input for the validation (content and metadata for one RDF instance).")
            @RequestBody Input in,
            HttpServletRequest request
    ) {
        DomainConfig domainConfig = validateDomain(domain);
        var reportSyntax = getValidationReportSyntax(in.getReportSyntax(), getAcceptHeader(request, domainConfig.getDefaultReportSyntax()));
        /*
         * Important: We call executeValidationProcess here and not in the return statement because the StreamingResponseBody
         * uses a separate thread. Doing so would break the ThreadLocal used in the statistics reporting.
         */
        var result = executeValidationProcess(in, domainConfig);
        return ResponseEntity.ok()
                .contentType(reportSyntax)
                .body(outputStream -> {
                    if (MediaType.APPLICATION_XML.equals(reportSyntax) || MediaType.TEXT_XML.equals(reportSyntax)) {
                        // GITB TRL report (XML format).
                        var wrapReportDataInCDATA = Objects.requireNonNullElse(in.getWrapReportDataInCDATA(), false);
                        fileManager.saveReport(createTAR(in, result, domainConfig, result.getValidationType()), outputStream, domainConfig, wrapReportDataInCDATA);
                    } else if (MediaType.APPLICATION_JSON.equals(reportSyntax)) {
                        // GITB TRL report (JSON format).
                        writeReportAsJson(outputStream, createTAR(in, result, domainConfig, result.getValidationType()), domainConfig);
                    } else {
                        // SHACL validation report.
                        fileManager.writeRdfModel(outputStream, result.getReport(), reportSyntax.toString());
                    }
                });
    }

    /**
     * Check to see if the original input received is needed in the resulting report.
     *
     * @param in The inputs.
     * @return The check result.
     */
    private boolean isOriginalInputNeeded(Input in) {
        return Objects.requireNonNullElse(in.getAddInputToReport(), false);
    }

    /**
     * Create a GITB TRL report (TAR) based on the received input and the result of the validation.
     *
     * @param in The inputs.
     * @param result The validation result.
     * @param domainConfig The domain configuration.
     * @param validationType The applied validation type.
     * @return The report.
     */
    private TAR createTAR(Input in, ValidationResources result, DomainConfig domainConfig, String validationType) {
        var reportSpecs = ReportSpecs.builder(result.getInput(), result.getReport(),
                new LocalisationHelper(Utils.getSupportedLocale(LocaleUtils.toLocale(in.getLocale()), domainConfig)),
                domainConfig, validationType);
        if (Objects.requireNonNullElse(in.getAddRdfReportToReport(), false)) {
            reportSpecs = reportSpecs.withReportContentToInclude(ShaclValidatorUtils.getRdfReportToIncludeInTAR(
                    result.getReport(),
                    StringUtils.defaultIfEmpty(in.getRdfReportSyntax(), domainConfig.getDefaultReportSyntax()),
                    in.getReportQuery(), fileManager));
        }
        if (isOriginalInputNeeded(in) && result.getInputContent().isPresent()) {
            reportSpecs = reportSpecs.withInputContentToInclude(result.getInputContent().get());
        }
        if (Objects.requireNonNullElse(in.getAddShapesToReport(), false)) {
            reportSpecs = reportSpecs.withShapesToInclude(result.getShapes());
        }
        return ShaclValidatorUtils.getTAR(reportSpecs.build()).getDetailedReport();
    }

    /**
     * Validate the provided value as a validation report syntax.
     *
     * @param reportSyntax The value to check.
     * @return True if valid.
     */
    private MediaType validReportSyntax(String reportSyntax) {
        var mediaType = MediaType.parseMediaType(reportSyntax);
        if (!MediaType.APPLICATION_XML.equals(mediaType) && !MediaType.TEXT_XML.equals(mediaType) && !MediaType.APPLICATION_JSON.equals(mediaType)) {
            if (RDFLanguages.contentTypeToLang(reportSyntax.toLowerCase()) == null) {
                return null;
            }
        }
        return mediaType;
    }

    /**
     * Returns the report syntax of the content to validate.
     *
     * @param inputReportSyntax Report syntax from the Input data.
     * @param acceptHeader Report syntax from the Header.
     * @return Report syntax as a media type instance.
     */
    private MediaType getValidationReportSyntax(String inputReportSyntax, String acceptHeader) {
        // Consider first the report syntax requested as part of the input properties.
        if (inputReportSyntax == null) {
            // If unspecified consider the first acceptable syntax from the Accept header.
            inputReportSyntax = acceptHeader;
        }
        var reportSyntax = validReportSyntax(inputReportSyntax);
        if (reportSyntax == null) {
            // The requested syntax is invalid.
            throw new ValidatorException("validator.label.exception.reportSyntaxNotSupported", inputReportSyntax);
        }
        return reportSyntax;
    }

    /**
     * Execute the process to validate the content.
     *
     * @param in The input for the validation of one RDF document.
     * @param domainConfig The domain where the SHACL validator is executed.
     * @return The report's models (input and report).
     */
    private ValidationResources executeValidationProcess(Input in, DomainConfig domainConfig) {
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
                    models = new ModelPair(models.getInputModel(), queryExecution.execConstruct());
                }
            }
            return new ValidationResources(
                    isOriginalInputNeeded(in)?Files.readString(inputFile.toPath()):null,
                    models.getInputModel(),
                    models.getReportModel(),
                    validator.getAggregatedShapes(),
                    validator.getValidationType());
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
        String acceptHeader = getAcceptHeader(request, domainConfig.getDefaultReportSyntax());
        Output[] outputs = new Output[inputs.length];
        int i = 0;
        for (Input input: inputs) {
            Output output = new Output();
            var reportSyntax = getValidationReportSyntax(input.getReportSyntax(), acceptHeader);
            if (MediaType.APPLICATION_JSON.equals(reportSyntax)) {
                // We don't support a JSON GITB TRL when validating multiple inputs.
                reportSyntax = MediaType.APPLICATION_XML;
            }
            // Start validation process of the Input
            var result = executeValidationProcess(input, domainConfig);
            if (MediaType.APPLICATION_XML.equals(reportSyntax) || MediaType.TEXT_XML.equals(reportSyntax)) {
                // GITB TRL report (XML format).
                try (var bos = new ByteArrayOutputStream()) {
                    var wrapReportDataInCDATA = Objects.requireNonNullElse(input.getWrapReportDataInCDATA(), false);
                    fileManager.saveReport(createTAR(input, result, domainConfig, result.getValidationType()), bos, domainConfig, wrapReportDataInCDATA);
                    output.setReport(Base64.getEncoder().encodeToString(bos.toByteArray()));
                } catch (IOException e) {
                    throw new ValidatorException(e);
                }
            } else {
                output.setReport(Base64.getEncoder().encodeToString(fileManager.writeRdfModelToString(result.getReport(), reportSyntax.toString()).getBytes()));
            }
            output.setReportSyntax(reportSyntax.toString());
            outputs[i] = output;
            i++;
        }
        return outputs;
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
        responseHeaders.set("Link", "<"+ path +"/api/vocab>; rel=\"http://www.w3.org/ns/hydra/core#apiDocumentation\"");
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
