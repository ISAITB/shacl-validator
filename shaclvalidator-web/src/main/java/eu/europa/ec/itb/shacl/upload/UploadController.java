package eu.europa.ec.itb.shacl.upload;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gitb.tr.TAR;

import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.ValidatorChannel;
import eu.europa.ec.itb.shacl.upload.errors.NotFoundException;
import eu.europa.ec.itb.shacl.util.Utils;
import eu.europa.ec.itb.shacl.validation.FileInfo;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;


@Controller
public class UploadController {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

    @Autowired
    FileManager fileManager;

    @Autowired
    BeanFactory beans;

    @Autowired
    DomainConfigCache domainConfigs;

    @Autowired
    ApplicationConfig appConfig;

    @Autowired
    ApplicationContext ctx;
    
    @GetMapping(value = "/{domain}/upload")
    public ModelAndView upload(@PathVariable("domain") String domain, Model model) {
		DomainConfig domainConfig = validateDomain(domain);	
		
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("validationTypes", getValidationTypes(domainConfig));
        attributes.put("contentSyntax", getContentSyntax());
        attributes.put("externalShapes", domainConfig.hasMultipleValidationTypes() ? includeExternalShapes(domainConfig) : includeExternalShape(domainConfig));
        attributes.put("config", domainConfig);
        attributes.put("appConfig", appConfig);
        
        return new ModelAndView("uploadForm", attributes);
    }
    
	@PostMapping(value = "/{domain}/upload")
    public ModelAndView handleUpload(@PathVariable("domain") String domain, @RequestParam("file") MultipartFile file, 
    		@RequestParam(value = "validationType", defaultValue = "") String validationType, 
    		@RequestParam(value = "contentSyntaxType", defaultValue = "") String contentSyntaxType,
    		@RequestParam(value = "inputFile-externalShape", required= false) MultipartFile[] externalFiles,
    		@RequestParam(value = "contentSyntaxType-externalShape", required = false) String[] externalFilesSyntaxType,
    		RedirectAttributes redirectAttributes) {
		DomainConfig domainConfig = validateDomain(domain);	
		
		File inputFile = null;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("validationTypes", getValidationTypes(domainConfig));
        attributes.put("contentSyntax", getContentSyntax());
        attributes.put("externalShapes", domainConfig.hasMultipleValidationTypes() ? includeExternalShapes(domainConfig) : includeExternalShape(domainConfig));
        attributes.put("config", domainConfig);
        attributes.put("appConfig", appConfig);
		
        try {
        	inputFile = this.fileManager.getInputStreamFile(file.getInputStream(), contentSyntaxType);
        	
        } catch (IOException e) {
            logger.error("Error while reading uploaded file [" + e.getMessage() + "]", e);
            attributes.put("message", "Error in upload [" + e.getMessage() + "]");
        }
        if (StringUtils.isBlank(validationType)) {
            validationType = null;
        }
        if (domainConfig.hasMultipleValidationTypes() && (validationType == null || !domainConfig.getType().contains(validationType))) {
            // A invoice type is required.
            attributes.put("message", "Provided validation type is not valid");
        }

        try {
            if (inputFile != null) {
            	List<FileInfo> extFiles = getExternalShapes(externalFiles, externalFilesSyntaxType);
        		SHACLValidator validator = ctx.getBean(SHACLValidator.class, inputFile, validationType, contentSyntaxType, extFiles, domainConfig);
        			
        		org.apache.jena.rdf.model.Model reportModel = validator.validateAll(); 
        		
    			TAR TARreport = Utils.getTAR(reportModel, inputFile.toPath(), contentSyntaxType, validator.getAggregatedShapes());
                attributes.put("report", TARreport);
                attributes.put("date", TARreport.getDate().toString());
                attributes.put("fileName", file.getOriginalFilename());
                
                // Cache detailed report.
                try {
                    String reportString = fileManager.getShaclReport(reportModel, domainConfig.getDefaultReportSyntax());
                    File reportFile = fileManager.getStringFile(reportString, domainConfig.getDefaultReportSyntax());
                    attributes.put("reportID", reportFile.getName());
                    
                    System.out.println("REPORT ID: " + reportFile.getName());
                    
                } catch (IOException e) {
                    logger.error("Error generating detailed report [" + e.getMessage() + "]", e);
                    attributes.put("message", "Error generating detailed report [" + e.getMessage() + "]");
                }
            }
        } catch (Exception e) {
            logger.error("An error occurred during the validation [" + e.getMessage() + "]", e);
            attributes.put("message", "An error occurred during the validation [" + e.getMessage() + "]");
        }        
        
        return new ModelAndView("uploadForm", attributes);
    }
	
	private List<FileInfo> getExternalShapes(MultipartFile[] externalFiles, String[] externalFilesSyntaxType) throws IOException {
		List<FileInfo> shaclFiles = new ArrayList<>();

		if(externalFiles != null && externalFiles.length>0) {
			for(int i=0; i<externalFiles.length; i++) {
	        	File inputFile = this.fileManager.getInputStreamFile(externalFiles[i].getInputStream(), externalFilesSyntaxType[i]);
	        	FileInfo fi = new FileInfo(inputFile, externalFilesSyntaxType[i]);
	        	
	        	shaclFiles.add(fi);
			}			
		}else {
			shaclFiles = Collections.emptyList();
		}
		
		return shaclFiles;
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
    
    public Boolean includeExternalShape(DomainConfig config) {
    	Map<String, Boolean> externalShapes = config.getExternalShapes();
    	
    	return externalShapes.get(config.getType().get(0));
    }
    
    public List<UploadTypes> includeExternalShapes(DomainConfig config){
        List<UploadTypes> types = new ArrayList<>();
    	Map<String, Boolean> externalShapes = config.getExternalShapes();
    	
    	for(Map.Entry<String, Boolean> entry : externalShapes.entrySet()) {
    		types.add(new UploadTypes(entry.getKey(), entry.getValue().toString()));
    	}
    	
    	return types;
    }

    public List<UploadTypes> getValidationTypes(DomainConfig config) {
        List<UploadTypes> types = new ArrayList<>();
        if (config.hasMultipleValidationTypes()) {
            for (String type: config.getType()) {
                types.add(new UploadTypes(type, config.getTypeLabel().get(type)));
            }
        }
        return types;
    }
    
    private List<UploadTypes> getContentSyntax() {
        List<UploadTypes> types = new ArrayList<>();
    	Collection<Lang> registeredLang = RDFLanguages.getRegisteredLanguages();
    	Iterator<Lang> itLang = registeredLang.iterator();
    	List<String> langTypes = new ArrayList<>();
    	
    	while(itLang.hasNext()){
    		Lang lang = itLang.next();
    		
    		if(!langTypes.contains(lang.getLabel())) {
    			types.add(new UploadTypes(lang.getLabel(), lang.getContentType().getContentType()));
    			langTypes.add(lang.getLabel());
    		}
    	}
    	
    	return types.stream().sorted(Comparator.comparing(UploadTypes::getLabel)).collect(Collectors.toList());
    }

}
