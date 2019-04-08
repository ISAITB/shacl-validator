package eu.europa.ec.itb.shacl.rest;

import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.ValidatorChannel;
import eu.europa.ec.itb.shacl.rest.errors.NotFoundException;
import eu.europa.ec.itb.shacl.rest.errors.ValidatorException;
import eu.europa.ec.itb.shacl.rest.model.ApiInfo;
import eu.europa.ec.itb.shacl.rest.model.Input;
import eu.europa.ec.itb.shacl.rest.model.Input.RuleSet;
import eu.europa.ec.itb.shacl.rest.model.Output;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import io.swagger.annotations.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

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
	@RequestMapping(value = "/{domain}/api/validate", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> validate(
			@ApiParam(required = true, name = "domain", value = "A fixed value corresponding to the specific validation domain.")
			@PathVariable("domain") String domain,
			@ApiParam(required = true, name = "input", value = "The input for the validation (content and metadata for one RDF instance).")
			@RequestBody Input in
	) {
		String shaclResult;

		DomainConfig domainConfig = validateDomain(domain);

		//Start validation of the input file
		File inputFile = null;
		try {
			inputFile = getContentToValidate(in);
			//Execute one single validation
			Model shaclReport = executeValidation(inputFile, in, domainConfig);

			String reportSyntax = domainConfig.getDefaultReportSyntax();
			//ReportSyntax validation
			if (in.getReportSyntax() != null) {
				if (validReportSyntax(in.getReportSyntax())) {
					reportSyntax = in.getReportSyntax();
				} else {
					logger.error(ValidatorException.message_parameters, reportSyntax);
					throw new ValidatorException(ValidatorException.message_parameters);
				}
			}

			//Process the result according to content-type
			shaclResult = getShaclReport(shaclReport, reportSyntax);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.setContentType(MediaType.parseMediaType(reportSyntax));
			return new ResponseEntity<>(shaclResult, responseHeaders, HttpStatus.OK);
		} catch (ValidatorException | NotFoundException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error occurred during processing", e);
			throw new ValidatorException(ValidatorException.message_default);
		} finally {
			//Remove temporary files
			removeContentToValidate(inputFile);
		}
	}

    @Autowired
    DomainConfigCache domainConfigs;


	private boolean validReportSyntax(String reportSyntax) {
		Lang lang = RDFLanguages.contentTypeToLang(reportSyntax.toLowerCase());
		return lang != null;
	}

    
    /**
     * POST service to receive multiple RDF instances to validate
     * 
     * @param domain The domain where the SHACL validator is executed. 
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
    @RequestMapping(value = "/{domain}/api/validateMultiple", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Output[] validateMultiple(
			@ApiParam(required = true, name = "domain", value = "A fixed value corresponding to the specific validation domain.")
    		@PathVariable("domain") String domain,
			@ApiParam(required = true, name = "input", value = "The input for the validation (content and metadata for one or more RDF instances).")
			@RequestBody Input[] inputs
	) {
    	throw new ValidatorException(ValidatorException.message_support);
    }
    
    /**
     * Validates that the domain exists.
     * @param domain 
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
     * @param input Configuration of the current RDF
     * @param domainConfig 
     * @return Model SHACL report
     */
    private Model executeValidation(File inputFile, Input input, DomainConfig domainConfig) {
    	Model report = null;
    	
    	try {	
			SHACLValidator validator = ctx.getBean(SHACLValidator.class, inputFile, input.getValidationType(), input.getContentSyntax(), domainConfig);
			
			report = validator.validateAll();   
    	}catch(Exception e){
            logger.error("Error during the validation: " + e);
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
     * Remove the validated file
     */
    private void removeContentToValidate(File inputFile) {
    	if (inputFile != null && inputFile.exists() && inputFile.isFile()) {
            FileUtils.deleteQuietly(inputFile);
        }
    }
    
    /**
     * Get the content from URL or BASE64
     * @param convert URL or BASE64 as String
     * @param convertSyntax Parameter convertSyntax
     * @return File
     */
    private File getContentFile(String convert, String convertSyntax) {
    	File outputFile = null;
    	
    	try {
    		outputFile = getURLFile(convert);
    	}catch(Exception e) {
    		logger.info("Content is not a URL, treating as BASE64.");
    		outputFile = getBase64File(convert, convertSyntax);
    	}
    	
    	return outputFile;
    }
    
    /**
     * From a URL, it gets the File
     * @param URLConvert URL as String
     * @return File
     */
    private File getURLFile(String URLConvert) {
		try {			
			URL url = new URL(URLConvert);
			String extension = FilenameUtils.getExtension(url.getFile());
			
			if(extension!=null) {				
				extension = "." + extension;
			}
			
			Path tmpPath = getTmpPath(extension);
			
			try(InputStream in = new URL(URLConvert).openStream()){
				Files.copy(in, tmpPath, StandardCopyOption.REPLACE_EXISTING);
			}
			
			return tmpPath.toFile();
		} catch (IOException e) {
			logger.error("Error when transforming the URL into File: " + URLConvert, e);
			throw new ValidatorException(ValidatorException.message_contentToValidate);
		}
    }
    
    /**
     * From Base64 string to File
     * @param base64Convert Base64 as String
     * @return File
     */
    private File getBase64File(String base64Convert, String contentSyntax) {
		if (contentSyntax == null) {
			logger.error(ValidatorException.message_parameters, "");
			throw new ValidatorException(ValidatorException.message_parameters);
		}

		Path tmpPath = getTmpPath("");
    	try {
            // Construct the string from its BASE64 encoded bytes.
        	byte[] decodedBytes = Base64.getDecoder().decode(base64Convert);
			FileUtils.writeByteArrayToFile(tmpPath.toFile(), decodedBytes);
		} catch (IOException e) {
			logger.error("Error when transforming the Base64 into File.", e);
			throw new ValidatorException(ValidatorException.message_contentToValidate);
		}
    	
    	return tmpPath.toFile();
    }
    
    /**
     * Get a temporary path and create the directory
     * @param extension Extension of the file (optional)
     * @return Path
     */
    private Path getTmpPath(String extension) {
		Path tmpPath = Paths.get(config.getTmpFolder(), UUID.randomUUID().toString() + extension);
		tmpPath.toFile().getParentFile().mkdirs();
		
		return tmpPath;
    }

    /**
     * Validates that the JSON send via parameters has all necessary data.
     * @param input JSON send via REST API
     * @return File to validate
     */
    private File getContentToValidate(Input input) {
    	String embeddingMethod = input.getEmbeddingMethod();
    	String contentToValidate = input.getContentToValidate();
    	List<RuleSet> externalRules = input.getExternalRules();
    	String contentSyntax = input.getContentSyntax();
    	
    	File contentFile = null;
    	
    	//EmbeddingMethod validation
    	if(embeddingMethod!=null) {
    		switch(embeddingMethod) {
    			case Input.embedding_URL:
    				contentFile = getURLFile(contentToValidate);
    				break;
    			case Input.embedding_BASE64:
    			    contentFile = getBase64File(contentToValidate, contentSyntax);
    			    break;
    			default:
    			    logger.error(ValidatorException.message_parameters, embeddingMethod);    			    
    				throw new ValidatorException(ValidatorException.message_parameters);
    		}
    	}else {
			contentFile = getContentFile(contentToValidate, contentSyntax);
    	}
    	
    	//ExternalRules validation
    	if(externalRules!=null) {
		    logger.error("Feature not supported.");
			throw new ValidatorException(ValidatorException.message_support);
    	}
    	
    	return contentFile;
    }
}
