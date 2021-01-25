package eu.europa.ec.itb.shacl.rest;

import com.gitb.core.ValueEmbeddingEnumeration;
import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.InputHelper;
import eu.europa.ec.itb.shacl.rest.model.ApiInfo;
import eu.europa.ec.itb.shacl.rest.model.Input;
import eu.europa.ec.itb.shacl.rest.model.Output;
import eu.europa.ec.itb.shacl.rest.model.RuleSet;
import eu.europa.ec.itb.shacl.util.Utils;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import eu.europa.ec.itb.validation.commons.FileContent;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import io.swagger.annotations.*;
import org.apache.commons.io.FileUtils;
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

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple REST controller to allow an easy way of validating files with the correspondence shapes.
 * 
 * Created by mfontsan on 25/03/2019
 *
 */
@Api(value="/{domain}/api", description = "Operations for the validation of RDF content based on SHACL shapes.")
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

	@ApiOperation(value = "Get API information (for a given domain).", response = ApiInfo.class, notes="Retrieve the supported validation " +
			"types that can be requested when calling this API's validation operations.")
	@ApiResponses({
			@ApiResponse(code = 200, message = "Success", response = ApiInfo.class),
			@ApiResponse(code = 500, message = "Error (If a problem occurred with processing the request)", response = String.class),
			@ApiResponse(code = 404, message = "Not found (for an invalid domain value)", response = String.class)
	})
	@RequestMapping(value = "/{domain}/api/info", method = RequestMethod.GET, consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiInfo info(
			@ApiParam(required = true, name = "domain", value = "A fixed value corresponding to the specific validation domain.")
			@PathVariable("domain") String domain
	) {
		DomainConfig domainConfig = validateDomain(domain);
		return ApiInfo.fromDomainConfig(domainConfig);
	}

	/** 
	 * GET service to receive all domains and its validation types.
	 * @return The data as List.
	 */
	@ApiOperation(value = "Get API information (all supported domains and validation types).", response = ApiInfo[].class, notes="Retrieve the supported domains and validation types configured in this validator. "
			+ "These are the domain and validation types that can be used as parameters with the API's other operations.")
	@ApiResponses({
			@ApiResponse(code = 200, message = "Success", response = ApiInfo[].class),
			@ApiResponse(code = 500, message = "Error (If a problem occurred with processing the request)", response = String.class)
	})
	@RequestMapping(value = "/api/info", method = RequestMethod.GET, consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
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
	 * POST service to receive a single RDF instance to validate
     * 
     * @param domain The domain where the SHACL validator is executed. 
	 * @param in The input for the validation.
	 * @param request HttpServletRequest
     * @return The result of the SHACL validator.
	 */
	@ApiOperation(value = "Validate one RDF instance.", response = String.class, notes="Validate a single RDF instance. " +
			"The content can be provided either within the request as a BASE64 encoded string or remotely as a URL. The RDF syntax for the input can be " +
			"determined in the request as can the syntax to produce the resulting SHACL validation report.")
	@ApiResponses({
			@ApiResponse(code = 200, message = "Success (for successful validation)", response = String.class),
			@ApiResponse(code = 500, message = "Error (If a problem occurred with processing the request)", response = String.class),
			@ApiResponse(code = 404, message = "Not found (for an invalid domain value)", response = String.class)
	})
	@RequestMapping(value = "/{domain}/api/validate", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE, "application/ld+json"})
	public ResponseEntity<String> validate(
			@ApiParam(required = true, name = "domain", value = "A fixed value corresponding to the specific validation domain.")
			@PathVariable("domain") String domain,
			@ApiParam(required = true, name = "input", value = "The input for the validation (content and metadata for one RDF instance).")
			@RequestBody Input in,
			HttpServletRequest request
	) {
		DomainConfig domainConfig = validateDomain(domain);
		String reportSyntax = getValidationReportSyntax(in.getReportSyntax(), getFirstSupportedAcceptHeader(request), domainConfig.getDefaultReportSyntax());

		//Start validation process of the Input
		String shaclResult = executeValidationProcess(in, domainConfig, reportSyntax);

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.parseMediaType(reportSyntax));
		return new ResponseEntity<>(shaclResult, responseHeaders, HttpStatus.OK);
	}

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

	private boolean validReportSyntax(String reportSyntax) {
		Lang lang = RDFLanguages.contentTypeToLang(reportSyntax.toLowerCase());
		return lang != null;
	}
	
	/**
	 * Returns the report syntax of the content to validate
	 * @param inputReportSyntax Report syntax from the Input data
	 * @param acceptHeader Report syntax from the Header
	 * @param defaultReportSyntax Default report syntax
	 * @return Report syntax as string
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
				throw new ValidatorException(String.format("The requested report syntax [%s] is not supported.", reportSyntax));
			}
		}
		return reportSyntax;
	}
	
	/**
	 * Execute the process to validate the content
	 * @param in The input for the validation (content and metadata for one or more RDF instances).
	 * @param domainConfig The domain where the SHACL validator is executed. 
	 * @param reportSyntax Report syntax of the report.
	 * @return Report of the validation as String.
	 */
	private String executeValidationProcess(Input in, DomainConfig domainConfig, String reportSyntax) {
		String validationResult;
		//Start validation of the input file
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
			SHACLValidator validator = ctx.getBean(SHACLValidator.class, inputFile, validationType, contentSyntax, externalShapes, loadImports, domainConfig);
			Model validationReport = validator.validateAll();
			if (in.getReportQuery() != null && !in.getReportQuery().isBlank()) {
				// Run post-processing query on report and return based on content-type
				Query query = QueryFactory.create(in.getReportQuery());
				QueryExecution queryExecution = QueryExecutionFactory.create(query, validationReport);
				validationResult = Utils.serializeRdfModel(queryExecution.execConstruct(), reportSyntax);
			} else {
				// Return the validation report according to content-type
				validationResult = Utils.serializeRdfModel(validationReport, reportSyntax);
			}
		} catch (ValidatorException | NotFoundException e) {
			throw e;
		} catch (Exception e) {
			throw new ValidatorException(e);
		} finally {
			FileUtils.deleteQuietly(parentFolder);
		}
		return validationResult;
	}

	private List<FileInfo> getExternalShapes(DomainConfig domainConfig, String validationType, List<RuleSet> externalRules, File parentFolder) {
		List<FileContent> shapeContents = null;
		if (externalRules != null) {
			shapeContents = externalRules.stream().map(RuleSet::toFileContent).collect(Collectors.toList());
		}
		return inputHelper.validateExternalArtifacts(domainConfig, shapeContents, validationType, null, parentFolder);
	}

	/**
     * POST service to receive multiple RDF instances to validate
     * 
     * @param domain The domain where the SHACL validator is executed. 
	 * @param inputs The input for the validation (content and metadata for one or more RDF instances).
     * @return The result of the SHACL validator.
	 */
    @ApiOperation(value = "Validate multiple RDF instances.", response = Output[].class, notes="Validate multiple RDF instances. " +
			"The content for each instance can be provided either within the request as a BASE64 encoded string or remotely as a URL. " +
			"The RDF syntax for each input can be determined in the request as can the syntax to produce each resulting SHACL validation report.")
	@ApiResponses({
			@ApiResponse(code = 200, message = "Success (for successful validation)", response = Output[].class),
			@ApiResponse(code = 500, message = "Error (If a problem occurred with processing the request)", response = String.class),
			@ApiResponse(code = 404, message = "Not found (for an invalid domain value)", response = String.class)
	})
    @RequestMapping(value = "/{domain}/api/validateMultiple", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE, "application/ld+json"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Output[] validateMultiple(
			@ApiParam(required = true, name = "domain", value = "A fixed value corresponding to the specific validation domain.")
    		@PathVariable("domain") String domain,
			@ApiParam(required = true, name = "input", value = "The input for the validation (content and metadata for one or more RDF instances).")
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
			String shaclResult = executeValidationProcess(input, domainConfig, reportSyntax);
			output.setReport(Base64.getEncoder().encodeToString(shaclResult.getBytes()));
			output.setReportSyntax(reportSyntax);

			outputs[i] = output;

			i++;
		}
		return outputs;
    }
    
    /**
     * Validates that the domain exists.
     * @param domain The domain where the SHACL validator is executed as String.
     * @return DomainConfig
     */
    private DomainConfig validateDomain(String domain) {
		DomainConfig config = domainConfigs.getConfigForDomainName(domain);
        if (config == null || !config.isDefined() || !config.getChannels().contains(ValidatorChannel.REST_API)) {
			throw new NotFoundException(domain);
        }
        MDC.put("domain", domain);
        return config;
    }

	@ApiOperation(hidden = true, value="")
	@RequestMapping(value = "/{domain}/api", method = RequestMethod.GET, produces = "application/ld+json")
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

	@ApiOperation(hidden = true, value="")
	@RequestMapping(value = "/{domain}/api/contexts/{contextName}", method = RequestMethod.GET, produces = "application/ld+json")
	public String hydraContexts(@PathVariable String domain, @PathVariable String contextName) throws IOException {
		DomainConfig domainConfig = validateDomain(domain);
		return FileUtils.readFileToString(new File(fileManager.getHydraDocsFolder(domainConfig.getDomainName()), "EntryPoint.jsonld"), Charset.defaultCharset());
	}

	@ApiOperation(hidden = true, value="")
	@RequestMapping(value = "/{domain}/api/vocab", method = RequestMethod.GET, produces = "application/ld+json")
	public String hydraVocab(@PathVariable String domain) throws IOException {
		DomainConfig domainConfig = validateDomain(domain);
		return FileUtils.readFileToString(new File(fileManager.getHydraDocsFolder(domainConfig.getDomainName()), "vocab.jsonld"), Charset.defaultCharset());
	}

}
