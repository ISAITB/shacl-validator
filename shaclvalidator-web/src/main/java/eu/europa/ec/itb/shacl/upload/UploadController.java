package eu.europa.ec.itb.shacl.upload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
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

    public static final String contentType_file     	= "fileType" ;
    public static final String contentType_uri     		= "uriType" ;
    public static final String contentType_string     	= "stringType" ;

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
        attributes.put("contentType", getContentType());
        attributes.put("contentSyntax", getContentSyntax(domainConfig));
        attributes.put("externalShapes", includeExternalShapes(domainConfig));
        attributes.put("config", domainConfig);
        attributes.put("appConfig", appConfig);
        
        return new ModelAndView("uploadForm", attributes);
    }
    
	@PostMapping(value = "/{domain}/upload")
    public ModelAndView handleUpload(@PathVariable("domain") String domain, 
    		@RequestParam("file") MultipartFile file,
    		@RequestParam(value = "uri", defaultValue = "") String uri,  
    		@RequestParam(value = "text-editor", defaultValue = "") String string,     		
    		@RequestParam(value = "contentType", defaultValue = "") String contentType,  
    		@RequestParam(value = "validationType", defaultValue = "") String validationType, 
    		@RequestParam(value = "contentSyntaxType", defaultValue = "") String contentSyntaxType,
    		@RequestParam(value = "contentType-externalShape", required = false) String[] externalContentType,
    		@RequestParam(value = "inputFile-externalShape", required= false) MultipartFile[] externalFiles,
    		@RequestParam(value = "uri-externalShape", required = false) String[] externalUri,
    		@RequestParam(value = "contentSyntaxType-externalShape", required = false) String[] externalFilesSyntaxType,
    		RedirectAttributes redirectAttributes) {
		DomainConfig domainConfig = validateDomain(domain);	
		
		File inputFile = null;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("validationTypes", getValidationTypes(domainConfig));
        attributes.put("contentType", getContentType());
        attributes.put("contentSyntax", getContentSyntax(domainConfig));
        attributes.put("externalShapes", includeExternalShapes(domainConfig));
        attributes.put("config", domainConfig);
        attributes.put("appConfig", appConfig);
		
        try {
        	if(StringUtils.isEmpty(contentSyntaxType) || contentSyntaxType.equals("empty")) {
        		if(!contentType.equals(contentType_string)) {
        			contentSyntaxType = getExtensionContentType((contentType.equals(contentType_file) ? file.getOriginalFilename() : uri));
        		}else {
                    logger.error("Provided content syntax type is not valid");
                    attributes.put("message", "Provided content syntax type is not valid");
        		}
        	}
        	inputFile = getInputFile(contentType, file.getInputStream(), uri, string, contentSyntaxType);
        	
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

        org.apache.jena.rdf.model.Model reportModel = null;
        try {
            if (inputFile != null) {
            	List<FileInfo> extFiles = getExternalShapes(externalContentType, externalFiles, externalUri, externalFilesSyntaxType);

            	SHACLValidator validator = ctx.getBean(SHACLValidator.class, inputFile, validationType, contentSyntaxType, extFiles, domainConfig);

        		reportModel = validator.validateAll(); 
        		
    			TAR TARreport = Utils.getTAR(reportModel, inputFile.toPath(), contentSyntaxType, validator.getAggregatedShapes());
                attributes.put("report", TARreport);
                attributes.put("date", TARreport.getDate().toString());
                
                if(contentType.equals(contentType_file)) {
                	attributes.put("fileName", file.getOriginalFilename());
                }
                if(contentType.equals(contentType_uri)) {
                	attributes.put("fileName", uri);
                }
                attributes.put("inputContentId", inputFile.getName());
            }
        } catch (Exception e) {
            logger.error("An error occurred during the validation [" + e.getMessage() + "]", e);
            attributes.put("message", "An error occurred during the validation [" + e.getMessage() + "]");
        }
        
        // Cache detailed report.
        try {
            String reportString = fileManager.getShaclReport(reportModel, domainConfig.getDefaultReportSyntax());
            File reportFile = fileManager.getStringFile(reportString, domainConfig.getDefaultReportSyntax());
            String reportID =  FilenameUtils.removeExtension(reportFile.getName());
            
            attributes.put("reportID", reportID);            
        } catch (IOException e) {
            logger.error("Error generating detailed report [" + e.getMessage() + "]", e);
            attributes.put("message", "Error generating detailed report [" + e.getMessage() + "]");
        }        
        
        return new ModelAndView("uploadForm", attributes);
    }
	
	private File getInputFile(String contentType, InputStream inputStream, String uri, String string, String contentSyntaxType) throws IOException {
		File inputFile = null;
		
		switch(contentType) {
			case contentType_file:
		    	inputFile = this.fileManager.getInputStreamFile(inputStream, contentSyntaxType);
				break;
			
			case contentType_uri:
				inputFile = this.fileManager.getURLFile(uri);
				break;
				
			case contentType_string:
				inputFile = this.fileManager.getStringFile(string, contentSyntaxType);
				break;
		}

		return inputFile;
	}

	private String getExtensionContentType(String filename) {		
		String contentType = null;
		Lang lang = RDFLanguages.filenameToLang(filename);
		
		if(lang != null) {
			contentType = lang.getContentType().getContentType();
		}
		
		return contentType;
	}
	
	private List<FileInfo> getExternalShapes(String[] externalContentType, MultipartFile[] externalFiles, String[] externalUri, String[] externalFilesSyntaxType) throws IOException {
		List<FileInfo> shaclFiles = new ArrayList<>();

		if(externalContentType != null) {
			for(int i=0; i<externalContentType.length; i++) {
				File inputFile = null;
				
				String contentSyntaxType = "";
				if(externalFilesSyntaxType.length > i) {
					contentSyntaxType = externalFilesSyntaxType[i];
				}
	        	
				switch(externalContentType[i]) {
					case contentType_file:
						if(!externalFiles[i].isEmpty()) {
							if(StringUtils.isEmpty(contentSyntaxType) || contentSyntaxType.equals("empty")) {
				        		contentSyntaxType = getExtensionContentType(externalFiles[i].getOriginalFilename());
				        	}
							
				        	inputFile = this.fileManager.getInputStreamFile(externalFiles[i].getInputStream(), contentSyntaxType);				
						}
						break;
					case contentType_uri:					
						if(externalUri.length>i && !externalUri[i].isEmpty()) {
							if(StringUtils.isEmpty(contentSyntaxType) || contentSyntaxType.equals("empty")) {
				        		contentSyntaxType = getExtensionContentType(externalUri[i]);
				        	}
							inputFile = this.fileManager.getURLFile(externalUri[i]);
						}
						break;
				}
	        	
	        	if(inputFile != null) {
	        		FileInfo fi = new FileInfo(inputFile, contentSyntaxType);
	        		shaclFiles.add(fi);
	        	}
			}
		}
		
		if(shaclFiles.isEmpty()) {
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
        return types.stream().sorted(Comparator.comparing(UploadTypes::getKey)).collect(Collectors.toList());
    }
    
    public List<UploadTypes> getContentType(){
        List<UploadTypes> types = new ArrayList<>();

		types.add(new UploadTypes(contentType_file, "File"));
		types.add(new UploadTypes(contentType_uri, "URI"));
		types.add(new UploadTypes(contentType_string, "Direct input"));
		
		return types;        
    }
    
    private List<UploadTypes> getContentSyntax(DomainConfig config) {
    	List<String> contentSyntax = config.getWebContentSyntax();    
    	if(contentSyntax.isEmpty()) {
    		contentSyntax = new ArrayList<>(appConfig.getContentSyntax());
    	}
        List<UploadTypes> types = new ArrayList<>();
    	
        for(String cs : contentSyntax) {
        	Lang lang = RDFLanguages.contentTypeToLang(cs);
			types.add(new UploadTypes(lang.getLabel(), lang.getContentType().getContentType()));
        }
    	
    	return types.stream().sorted(Comparator.comparing(UploadTypes::getKey)).collect(Collectors.toList());
    }

}
