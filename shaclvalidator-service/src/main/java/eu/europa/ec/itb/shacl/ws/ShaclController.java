package eu.europa.ec.itb.shacl.ws;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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
    @RequestMapping(value = "/{domain}/shaclvalidator", method = RequestMethod.GET)
    public String shaclValidator(@PathVariable("domain") String domain, @RequestBody String input) { 
    	String shaclResult = null;
    	
        DomainConfig config = domainConfigs.getConfigForDomain(domain);
        if (config == null || !config.getChannels().contains(ValidatorChannel.FORM)) {
            logger.error("The following domain does not exist: " + domain);
            throw new RuntimeException();
        }
        MDC.put("domain", domain);
        
    	Gson gson = new Gson();
    	InputData inputObj = gson.fromJson(input, InputData.class);
    	
    	InputContent in = inputObj.getInput(0);
    	
		if(getContentToValidate(in)) {
			File fileInput = getURLFile(in.getContentToValidate());
			SHACLValidator validator = ctx.getBean(SHACLValidator.class, fileInput, in.getValidationType(), domainConfig);
	        Model shaclReport = validator.validateAll();
	        
	        String shaclReportstr = ModelPrinter.get().print(shaclReport);
	        
	        shaclResult = String.format("Sent message [%s]", shaclReportstr);
		}
		
		return shaclResult;
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
						
			InputStream in = new URL(URLConvert).openStream();
			Files.copy(in, Paths.get(config.getResourceRoot(), domainConfig.getDomain(), f.getName()), StandardCopyOption.REPLACE_EXISTING);
			
			in.close();
			
			return Paths.get(config.getResourceRoot(), domainConfig.getDomain(), f.getName()).toFile();
		} catch (IOException e) {
			logger.error("Error when transforming the URL into File: " + URLConvert);
			e.printStackTrace();
			
			return null;
		}
    }

    /**
     * Validates that the JSON send via parameters has all necessary data.
     * @param input
     * @return
     */
    private boolean getContentToValidate(InputContent input) {
    	boolean contentCorrect = false;
    	String embeddingMethod = input.getEmbeddingMethod();
    	String reportSyntax = input.getReportSyntax();
    	
    	if(config.getAcceptedEmbeddingMethod().contains(embeddingMethod) && config.getAcceptedMimeTypes().contains(reportSyntax)) {
    		contentCorrect = true;
    	}
    	
    	return contentCorrect;
    }
}
