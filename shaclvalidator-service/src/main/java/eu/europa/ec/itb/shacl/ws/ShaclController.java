package eu.europa.ec.itb.shacl.ws;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFWriter;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.topbraid.shacl.util.ModelPrinter;

import com.google.gson.Gson;

import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.ValidatorChannel;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import eu.europa.ec.itb.shacl.ws.InputData.InputContent;

/**
 * Simple REST controller to allow an easy way of validating TTL files with the correspondence shapes.
 * 
 * Created by mfontsan on 25/03/2019
 *
 */
@RestController
public class ShaclController {

    private static final Logger logger = LoggerFactory.getLogger(ShaclController.class);

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
     * GET service to receive input for the test bed.
     * 
     * @param domain The domain where the SHACL validator is executed. 
     * @param input JSON with the configuration of the validation.
     * @return The result of the SHACL validator.
     */
    @RequestMapping(value = "/{domain}/shaclvalidator", method = RequestMethod.POST)
    public ResponseEntity<String> shaclValidator(@PathVariable("domain") String domain, @RequestHeader("Content-Type") String contentType, @RequestBody String input) { 
    	String shaclResult = null;
    	
        DomainConfig config = domainConfigs.getConfigForDomain(domain);
        if (config == null || !config.getChannels().contains(ValidatorChannel.REST_API)) {
            logger.error("The following domain does not exist: " + domain);
			throw new NotFoundException();
        }
        MDC.put("domain", domain);
        
        //Process the input request body
    	Gson gson = new Gson();
    	InputData inputObj = gson.fromJson(input, InputData.class);    	
    	InputContent in = inputObj.getInput(0);
    	
    	//Start validation of the input file
		File fileInput = getContentToValidate(in);		
		SHACLValidator validator = ctx.getBean(SHACLValidator.class, fileInput, in.getValidationType(), in.getContentSyntax(), domainConfig);	    
		Model shaclReport = validator.validateAll();
		
		//Process the result according to content-type
	    shaclResult = getShaclReport(shaclReport, in.getReportSyntax());
	    
	    HttpHeaders responseHeaders = new HttpHeaders();
	    responseHeaders.setContentType(MediaType.parseMediaType(contentType));
	    
	    //Remove temporary files
	    removeContentToValidate(fileInput);
		
		return new ResponseEntity<String>(shaclResult, responseHeaders, HttpStatus.CREATED);
    }
    
    private String getShaclReport(Model shaclReport, String reportSyntax) {
		StringWriter writer = new StringWriter();
		RDFWriter w;
		Lang lang = RDFLanguages.shortnameToLang(reportSyntax);
		
		if(lang == null) {
			w = shaclReport.getWriter(null);
		}else {
			try {
				w = shaclReport.getWriter(lang.getName());
			}catch(Exception e) {
				w = shaclReport.getWriter(null);
			}
		}
		
		w.write(shaclReport, writer, null);
		return writer.toString();
    }
    
    private void removeContentToValidate(File fileToValidate) {
    	if (fileToValidate.exists() && fileToValidate.isFile()) {
            FileUtils.deleteQuietly(fileToValidate);
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
			File f = new File(url.getPath());
			String path = config.getTmpFolder() + domainConfig.getDomain() + UUID.randomUUID().toString() + f.getName();
						
			InputStream in = new URL(URLConvert).openStream();
			Files.copy(in, Paths.get(path), StandardCopyOption.REPLACE_EXISTING);
			
			in.close();
			
			return Paths.get(path).toFile();
		} catch (IOException e) {
			logger.error("Error when transforming the URL into File: " + URLConvert);
			throw new NotFoundException();
		}
    }

    /**
     * Validates that the JSON send via parameters has all necessary data.
     * @param input
     * @return File to validate
     */
    private File getContentToValidate(InputContent input) {
    	String embeddingMethod = input.getEmbeddingMethod();
    	String reportSyntax = input.getReportSyntax();
    	String contentToValidate = input.getContentToValidate();
    	
    	File contentFile = null;
    	
    	if(config.getAcceptedEmbeddingMethod().contains(embeddingMethod) && config.getAcceptedMimeTypes().contains(reportSyntax)) {
    		switch(embeddingMethod) {
    			case InputData.embeddingMethod_URL:
    				contentFile = getURLFile(contentToValidate);
    				break;
    			default:
    			    removeContentToValidate(contentFile);
    				throw new NotFoundException();
    		}
    	}else {
    		throw new NotFoundException();
    	}
    	
    	return contentFile;
    }
}
