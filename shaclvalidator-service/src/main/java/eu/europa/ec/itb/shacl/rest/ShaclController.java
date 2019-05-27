package eu.europa.ec.itb.shacl.rest;

import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.ValidatorChannel;
import eu.europa.ec.itb.shacl.ValidatorContent;
import eu.europa.ec.itb.shacl.rest.errors.NotFoundException;
import eu.europa.ec.itb.shacl.rest.errors.ValidatorException;
import eu.europa.ec.itb.shacl.rest.model.ApiInfo;
import eu.europa.ec.itb.shacl.rest.model.Input;
import eu.europa.ec.itb.shacl.rest.model.Output;
import eu.europa.ec.itb.shacl.rest.model.RuleSet;
import eu.europa.ec.itb.shacl.validation.FileContent;
import eu.europa.ec.itb.shacl.validation.FileInfo;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import io.swagger.annotations.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ShaclController.class);
    
    @Autowired
    ApplicationContext ctx;
    @Autowired
    ApplicationConfig config;
	@Autowired
	DomainConfigCache domainConfigs;
    @Autowired
	FileManager fileManager;
    @Autowired
	ValidatorContent validatorContent;
    @Value("${validator.acceptedHeaderAcceptTypes}")
	Set<String> acceptedHeaderAcceptTypes;
    @Value("${validator.hydraServer}")
	String hydraServer;
	@Value("${validator.hydraRootPath}")
	private String hydraRootPath;

	@ApiOperation(value = "Get API information.", response = ApiInfo.class, notes="Retrieve the supported validation " +
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
		String reportSyntax = getValidateReportSyntax(in.getReportSyntax(), getFirstSupportedAcceptHeader(request), domainConfig.getDefaultReportSyntax());

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
	 * @param inputRS Report syntax from the Input data
	 * @param acceptHeader Report syntax from the Header
	 * @param defaultRS Default report syntax
	 * @return Report syntax as string
	 */
	private String getValidateReportSyntax(String inputRS, String acceptHeader, String defaultRS) {
		// Consider first the report syntax requested as part of the input properties.
		String reportSyntax = inputRS;
		if (reportSyntax == null) {
			// If unspecified consider the first acceptable syntax from the Accept header.
			reportSyntax = acceptHeader;
		}
		if (reportSyntax == null) {
			// No syntax specified by client - use the configuration default.
			reportSyntax = defaultRS;
		} else {
			if (!validReportSyntax(reportSyntax)) {
				// The requested syntax is invalid.
				logger.error(ValidatorException.message_parameters, reportSyntax);
				throw new ValidatorException(ValidatorException.message_parameters);
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
		String shaclResult;
		List<FileInfo> remoteShaclFiles = new ArrayList<>();
		File inputFile = null;
		
		//Start validation of the input file
		try {
			inputFile = getContentToValidate(in, domainConfig);
			remoteShaclFiles = getExternalShapes(in.getExternalRules());
			//Execute one single validation
			Model shaclReport = executeValidation(inputFile, in, domainConfig, remoteShaclFiles);

			//Process the result according to content-type
			shaclResult = getShaclReport(shaclReport, reportSyntax);
		} catch (ValidatorException | NotFoundException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error occurred during processing", e);
			throw new ValidatorException(ValidatorException.message_default);
		} finally {
			// Remove temporary files
			fileManager.removeContentToValidate(inputFile, remoteShaclFiles);
		}
		
		return shaclResult;
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
			String reportSyntax = getValidateReportSyntax(input.getReportSyntax(), acceptHeader, domainConfig.getDefaultReportSyntax());

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
            logger.error("The following domain does not exist: " + domain);
			throw new NotFoundException();
        }
        MDC.put("domain", domain);
        return config;
    }
    
    /**
     * Executes the validation
     * @param inputFile The input RDF (or other) content to validate.
     * @param input Configuration of the current RDF.
     * @param domainConfig The domain where the SHACL validator is executed.
     * @param remoteShaclFiles Any shapes to consider that are externally provided.
     * @return Model SHACL report
     */
    private Model executeValidation(File inputFile, Input input, DomainConfig domainConfig, List<FileInfo> remoteShaclFiles) {
    	Model report = null;
    	
    	try {	
			SHACLValidator validator = ctx.getBean(SHACLValidator.class, inputFile, input.getValidationType(), input.getContentSyntax(), remoteShaclFiles, domainConfig);
			
			report = validator.validateAll();   
    	}catch(Exception e){
            logger.error("Error during the validation", e);
			throw new ValidatorException(ValidatorException.message_default);
    	}
		
		return report;
    }
    
    /**
     * Get report in the provided content type
     * @param shaclReport Model with the report
     * @param reportSyntax Content type
     * @return String
     */
    private String getShaclReport(Model shaclReport, String reportSyntax) {
		StringWriter writer = new StringWriter();		
		Lang lang = RDFLanguages.contentTypeToLang(reportSyntax);
		
		if(lang == null) {
			shaclReport.write(writer, null);
		}else {
			try {
				shaclReport.write(writer, lang.getName());
			}catch(Exception e) {
				shaclReport.write(writer, null);
			}
		}
		
		return writer.toString();
    }

    /**
     * Validates that the JSON send via parameters has all necessary data.
     * @param input JSON send via REST API
     * @return File to validate
     */
    private File getContentToValidate(Input input, DomainConfig domainConfig) {
    	String embeddingMethod = input.getEmbeddingMethod();
    	String contentToValidate = input.getContentToValidate();
    	List<RuleSet> externalRules = input.getExternalRules();
    	String contentSyntax = input.getContentSyntax();
    	String validationType = input.getValidationType();

    	//ValidationType validation
    	validationType = validatorContent.validateValidationType(validationType, domainConfig);
    	
    	//ExternalRules validation
    	Boolean hasExternalShapes = domainConfig.getExternalShapes().get(validationType);
    	if (externalRules!=null) {
    		if (!externalRules.isEmpty() && !hasExternalShapes) {
				logger.error("Loading external shape files is not supported in this domain.");
				throw new ValidatorException("Loading external shape files is not supported in this domain.");
			} else {
				for (RuleSet ruleSet: externalRules) {
					if (FileContent.embedding_BASE64.equals(ruleSet.getEmbeddingMethod()) && StringUtils.isEmpty(ruleSet.getRuleSyntax())) {
						logger.error(ValidatorException.message_syntaxRequired, "");
						throw new ValidatorException(ValidatorException.message_syntaxRequired);
					}
				}
			}
		}
    	
    	//EmbeddingMethod validation and returns the content    	
    	return validatorContent.getContentToValidate(embeddingMethod, contentToValidate, contentSyntax);
    }
    
    private List<FileInfo> getExternalShapes(List<RuleSet> externalRules) {
		List<FileInfo> shaclFiles;
		if (externalRules != null) {
			shaclFiles = fileManager.getRemoteExternalShapes(externalRules.stream().map(RuleSet::toFileContent).collect(Collectors.toList()));
		} else {
			shaclFiles = Collections.emptyList();
		}
    	return shaclFiles;
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
