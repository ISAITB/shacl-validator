package eu.europa.ec.itb.shacl.upload;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.ValidatorChannel;
import eu.europa.ec.itb.shacl.errors.ValidatorException;
import eu.europa.ec.itb.shacl.upload.errors.NotFoundException;
import eu.europa.ec.itb.shacl.util.Utils;
import eu.europa.ec.itb.shacl.validation.FileInfo;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;


@Controller
public class UploadController {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

    public static final String contentType_file     	= "fileType" ;
    public static final String contentType_uri     		= "uriType" ;
    public static final String contentType_string     	= "stringType" ;
    
    public static final String downloadType_report		= "reportType";
    public static final String downloadType_shapes		= "shapesType";
    public static final String downloadType_content		= "contentType";
    
    public static final String fileName_input			= "inputFile";
    public static final String fileName_report			= "reportFile";
    public static final String fileName_shapes			= "shapesFile";
    
    @Autowired
    FileManager fileManager;

    @Autowired
    DomainConfigCache domainConfigs;

    @Autowired
    ApplicationConfig appConfig;

    @Autowired
    ApplicationContext ctx;

    @Autowired
    ReportGeneratorBean reportGenerator;
    
    @GetMapping(value = "/{domain}/upload")
    public ModelAndView upload(@PathVariable("domain") String domain, Model model) {
    	DomainConfig domainConfig;
		try {
			domainConfig = validateDomain(domain);
		} catch (Exception e) {
			throw new NotFoundException();
		}
		
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("validationTypes", getValidationTypes(domainConfig));
        attributes.put("contentType", getContentType(domainConfig));
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
    		@RequestParam(value = "addExternalRules", required = false) Boolean addExternalRules,
    		@RequestParam(value = "contentSyntaxType-externalShape", required = false) String[] externalFilesSyntaxType) {
		DomainConfig domainConfig;
		try {
			domainConfig = validateDomain(domain);
		} catch (Exception e) {
			throw new NotFoundException();
		}
		String folderName = UUID.randomUUID().toString();
		String tmpFolder = this.appConfig.getTmpFolder() + "/web/" + folderName;
		
		File inputFile;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("validationTypes", getValidationTypes(domainConfig));
        attributes.put("contentType", getContentType(domainConfig));
        attributes.put("contentSyntax", getContentSyntax(domainConfig));
        attributes.put("externalShapes", includeExternalShapes(domainConfig));
        attributes.put("downloadType", getDownloadType(domainConfig));
        attributes.put("config", domainConfig);
        attributes.put("appConfig", appConfig);

        org.apache.jena.rdf.model.Model reportModel;
		org.apache.jena.rdf.model.Model agrregatedShapes;
        try {
			if(StringUtils.isEmpty(contentSyntaxType) || contentSyntaxType.equals("empty")) {
				if(!contentType.equals(contentType_string)) {
					contentSyntaxType = getExtensionContentType((contentType.equals(contentType_file) ? file.getOriginalFilename() : uri));
				}else {
					logger.error("Provided content syntax type is not valid");
					attributes.put("message", "Provided content syntax type is not valid");
				}
			}
			inputFile = getInputFile(contentType, file.getInputStream(), uri, string, contentSyntaxType, tmpFolder);
			if (StringUtils.isBlank(validationType)) {
				validationType = null;
			}
			if (domainConfig.hasMultipleValidationTypes() && (validationType == null || !domainConfig.getType().contains(validationType))) {
				// A validation type is required.
				attributes.put("message", "Provided validation type is not valid");
			} else {
				if (inputFile != null) {

					List<FileInfo> extFiles;
					if (addExternalRules != null && addExternalRules && hasExternalShapes(domainConfig, validationType)) {
						extFiles = getExternalShapes(externalContentType, externalFiles, externalUri, externalFilesSyntaxType);
					} else {
						extFiles = Collections.emptyList();
					}

					SHACLValidator validator = ctx.getBean(SHACLValidator.class, inputFile, validationType, contentSyntaxType, extFiles, domainConfig);

					reportModel = validator.validateAll();
					agrregatedShapes =  validator.getAggregatedShapes();

					TAR TARreport = Utils.getTAR(reportModel, inputFile.toPath(), agrregatedShapes, domainConfig.isReportsOrdered());
					attributes.put("report", TARreport);
					attributes.put("date", TARreport.getDate().toString());

					File pdfReport = new File(tmpFolder, fileName_report+"PDF.pdf");
					reportGenerator.writeReport(domainConfig, TARreport, pdfReport);

					if(contentType.equals(contentType_file)) {
						attributes.put("fileName", file.getOriginalFilename());
					} else if(contentType.equals(contentType_uri)) {
						attributes.put("fileName", uri);
					} else {
						attributes.put("fileName", "-");
					}
					String reportString = fileManager.getShaclReport(reportModel, domainConfig.getDefaultReportSyntax());
					fileManager.getStringFile(tmpFolder, reportString, domainConfig.getDefaultReportSyntax(), fileName_report);

					String shapesString = fileManager.getShaclReport(agrregatedShapes, domainConfig.getDefaultReportSyntax());
					fileManager.getStringFile(tmpFolder, shapesString, domainConfig.getDefaultReportSyntax(), fileName_shapes);

					attributes.put("reportID", folderName);
				}
			}
		} catch (ValidatorException e) {
			logger.error(e.getMessage(), e);
			attributes.put("message", e.getMessage());
		} catch (Exception e) {
            logger.error("An error occurred during the validation [" + e.getMessage() + "]", e);
            attributes.put("message", "An error occurred during the validation [" + e.getMessage() + "]");
        }
        return new ModelAndView("uploadForm", attributes);
    }

	private boolean hasExternalShapes(DomainConfig domainConfig, String validationType) {
    	if (validationType == null) {
			validationType = domainConfig.getType().get(0);
		}
    	return domainConfig.getExternalShapes().getOrDefault(validationType, Boolean.FALSE);
	}

	private File getInputFile(String contentType, InputStream inputStream, String uri, String string, String contentSyntaxType, String tmpFolder) throws IOException {
		File inputFile = null;
		
		switch(contentType) {
			case contentType_file:
		    	inputFile = this.fileManager.getInputStreamFile(tmpFolder, inputStream, contentSyntaxType, fileName_input);
				break;
			
			case contentType_uri:
				inputFile = this.fileManager.getURLFile(tmpFolder, uri, fileName_input);
				break;
				
			case contentType_string:
				inputFile = this.fileManager.getStringFile(tmpFolder, string, contentSyntaxType, fileName_input);
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
    private DomainConfig validateDomain(String domain) throws Exception {
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
    
    public List<UploadTypes> getContentType(DomainConfig config){
        List<UploadTypes> types = new ArrayList<>();

		types.add(new UploadTypes(contentType_file, config.getLabel().getOptionContentFile()));
		types.add(new UploadTypes(contentType_uri, config.getLabel().getOptionContentURI()));
		types.add(new UploadTypes(contentType_string, config.getLabel().getOptionContentDirectInput()));
		
		return types;        
    }
    
    public List<UploadTypes> getDownloadType(DomainConfig config){
        List<UploadTypes> types = new ArrayList<>();
		types.add(new UploadTypes(downloadType_report, config.getLabel().getOptionDownloadReport()));
		types.add(new UploadTypes(downloadType_shapes, config.getLabel().getOptionDownloadShapes()));
		types.add(new UploadTypes(downloadType_content, config.getLabel().getOptionDownloadContent()));
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