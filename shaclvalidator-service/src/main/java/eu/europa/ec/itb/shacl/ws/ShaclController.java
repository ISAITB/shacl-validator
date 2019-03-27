package eu.europa.ec.itb.shacl.ws;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.ValidatorChannel;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import eu.europa.ec.itb.shacl.ws.InputData.RuleSet;

/**
 * Simple REST controller to allow an easy way of validating files with the correspondence shapes.
 * 
 * Created by mfontsan on 25/03/2019
 *
 */
@RestController
public class ShaclController {

    private static final Logger logger = LoggerFactory.getLogger(ShaclController.class);
    
    private File inputFile;
    private String reportSyntax = "application/rdf+xml";

    @Autowired
    ApplicationContext ctx;

    @Autowired
    ApplicationConfig config;
    
    @Autowired
    DomainConfigCache domainConfigs;

    DomainConfig domainConfig;

    public ShaclController(DomainConfig domainConfig) {
        this.domainConfig = domainConfig;
    }
    
    /**
     * POST service to receive a single RDF instance to validate
     * 
     * @param domain The domain where the SHACL validator is executed. 
     * @param input JSON with the configuration of the validation.
     * @return The result of the SHACL validator.
     */
    @RequestMapping(value = "/{domain}/validate", method = RequestMethod.POST)
    public ResponseEntity<String> validate(@PathVariable("domain") String domain, @RequestBody String input) { 
    	String shaclResult = null;
    	
    	validateDomain(domain);
        
        //Process the input request body
    	Gson gson = new Gson();
    	InputData in = gson.fromJson(input, InputData.class);
    	
    	//Execute one single validation	    
		Model shaclReport = executeValidation(in);
		
		//Process the result according to content-type
	    shaclResult = getShaclReport(shaclReport, this.reportSyntax);
	    
	    HttpHeaders responseHeaders = new HttpHeaders();
	    responseHeaders.setContentType(MediaType.parseMediaType(this.reportSyntax));
	    
	    //Remove temporary files
	    removeContentToValidate();
		
		return new ResponseEntity<String>(shaclResult, responseHeaders, HttpStatus.CREATED);
    }

    
    /**
     * POST service to receive multiple RDF instances to validate
     * 
     * @param domain The domain where the SHACL validator is executed. 
     * @param input JSON with the configuration of the validation.
     * @return The result of the SHACL validator.
     */
    @RequestMapping(value = "/{domain}/validateMultiple", method = RequestMethod.POST)
    public ResponseEntity<String> validateMultiple(@PathVariable("domain") String domain, @RequestBody String input) { 
    	throw new ValidatorException(ValidatorException.message_support);
    }
    
    /**
     * Validates that the domain exists.
     * @param domain 
     */
    private void validateDomain(String domain) {    	
        DomainConfig config = domainConfigs.getConfigForDomain(domain);
        
        if (config == null || !config.getChannels().contains(ValidatorChannel.REST_API)) {
            logger.error("The following domain does not exist: " + domain);
			throw new NotFoundException();
        }
        
        MDC.put("domain", domain);    	
    }
    
    /**
     * Executes the validation
     * @param inputData Configuration of the current RDF
     * @return Model SHACL report
     */
    private Model executeValidation(InputData inputData) {
    	Model report = null;
    	
    	//Start validation of the input file
		inputFile = getContentToValidate(inputData);	
    	try {	
			SHACLValidator validator = ctx.getBean(SHACLValidator.class, inputFile, inputData.getValidationType(), inputData.getContentSyntax(), domainConfig);	    
			
			report = validator.validateAll();   
    	}catch(Exception e){
    	    removeContentToValidate();
    	    
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
    private void removeContentToValidate() {
    	if (inputFile != null && inputFile.exists() && inputFile.isFile()) {
            FileUtils.deleteQuietly(inputFile);
        }
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
				if(!config.getAcceptedInputExtensions().contains(extension.toUpperCase())) {
				    logger.error(ValidatorException.message_parameters, extension);			    
					throw new ValidatorException(ValidatorException.message_parameters);   					
				} 
				
				extension = "." + extension;
			}
			
			Path tmpPath = Paths.get(config.getTmpFolder(), UUID.randomUUID().toString() + extension);
			tmpPath.toFile().getParentFile().mkdirs();
			
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
     * Validates that the JSON send via parameters has all necessary data.
     * @param input JSON send via REST API
     * @return File to validate
     */
    private File getContentToValidate(InputData input) {
    	String embeddingMethod = input.getEmbeddingMethod();
    	String reportSyntax = input.getReportSyntax();
    	String contentSyntax = input.getContentSyntax();
    	String contentToValidate = input.getContentToValidate();
    	List<RuleSet> externalRules = input.getExternalRules();
    	
    	File contentFile = null;
    	
    	//EmbeddingMethod validation
    	if(embeddingMethod!=null) {
	    	if(config.getAcceptedEmbeddingMethod().contains(embeddingMethod.toUpperCase())) {
	    		switch(embeddingMethod) {
	    			case InputData.embedding_URL:
	    				contentFile = getURLFile(contentToValidate);
	    				break;
	    			case InputData.embedding_BASE64:
	    			    logger.error("Feature not supported: ", embeddingMethod);
	    				throw new ValidatorException(ValidatorException.message_support);
	    			default:
	    			    logger.error(ValidatorException.message_parameters, embeddingMethod);
	    			    
	    				throw new ValidatorException(ValidatorException.message_parameters);
	    		}
	    	}else {		    
			    logger.error(ValidatorException.message_parameters, embeddingMethod);
			    
	    		throw new ValidatorException(ValidatorException.message_parameters);
	    	}
    	}else {
			contentFile = getURLFile(contentToValidate);
    	}
    	
    	//ReportSyntax validation
    	if(reportSyntax!=null) {
    		Lang lang = RDFLanguages.contentTypeToLang(reportSyntax.toLowerCase());
        	if(lang!=null) {
        		this.reportSyntax = reportSyntax;
        	}else {
        		removeContentToValidate();
        		
			    logger.error(ValidatorException.message_parameters, reportSyntax);			    
				throw new ValidatorException(ValidatorException.message_parameters);        		
        	}
    	}
    	
    	//ContentSyntax validation
    	if(contentSyntax!=null && !config.getAcceptedContentMimeTypes().contains(contentSyntax.toLowerCase())) {
    		removeContentToValidate();
    		
		    logger.error(ValidatorException.message_parameters, embeddingMethod);			    
			throw new ValidatorException(ValidatorException.message_parameters);        		
    	}
    	
    	//ExternalRules validation
    	if(externalRules!=null) {
		    logger.error("Feature not supported.");
			throw new ValidatorException(ValidatorException.message_support);
    	}
    	
    	return contentFile;
    }
}
