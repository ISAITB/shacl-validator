package eu.europa.ec.itb.shacl.upload;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.shacl.*;
import eu.europa.ec.itb.shacl.util.Utils;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.report.ReportGeneratorBean;
import eu.europa.ec.itb.validation.commons.web.KeyWithLabel;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static eu.europa.ec.itb.validation.commons.web.Constants.IS_MINIMAL;

@Controller
public class UploadController {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

    private static final String CONTENT_TYPE__FILE = "fileType" ;
    private static final String CONTENT_TYPE__URI = "uriType" ;
    private static final String CONTENT_TYPE__EDITOR = "stringType" ;
	private static final String CONTENT_TYPE__QUERY = "queryType" ;
	static final String DOWNLOAD_TYPE__REPORT = "reportType";
	static final String DOWNLOAD_TYPE__SHAPES = "shapesType";
	static final String DOWNLOAD_TYPE__CONTENT = "contentType";
	static final String FILE_NAME__INPUT = "inputFile";
	static final String FILE_NAME__REPORT = "reportFile";
	static final String FILE_NAME__SHAPES = "shapesFile";
    
    @Autowired
	private FileManager fileManager = null;

    @Autowired
	private DomainConfigCache domainConfigs = null;

    @Autowired
	private ApplicationConfig appConfig = null;

    @Autowired
	private ApplicationContext ctx = null;

    @Autowired
    private ReportGeneratorBean reportGenerator = null;
    
    @Autowired
    private InputHelper inputHelper = null;

    @GetMapping(value = "/{domain}/upload")
    public ModelAndView upload(@PathVariable("domain") String domain, Model model, HttpServletRequest request) {
		setMinimalUIFlag(request, false);
    	DomainConfig domainConfig;
		try {
			domainConfig = validateDomain(domain);
		} catch (Exception e) {
			throw new NotFoundException();
		}
		
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("contentType", getContentType(domainConfig));
        attributes.put("contentSyntax", getContentSyntax(domainConfig));
        attributes.put("externalArtifactInfo", domainConfig.getExternalArtifactInfoMap());
		attributes.put("loadImportsInfo", domainConfig.getUserInputForLoadImportsType());
        attributes.put("minimalUI", false);
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
    		@RequestParam(value = "contentType-external_default", required = false) String[] externalContentType,
    		@RequestParam(value = "inputFile-external_default", required= false) MultipartFile[] externalFiles,
    		@RequestParam(value = "uri-external_default", required = false) String[] externalUri,
    		@RequestParam(value = "contentSyntaxType-external_default", required = false) String[] externalFilesSyntaxType,
            @RequestParam(value = "loadImportsCheck", required = false, defaultValue = "false") Boolean loadImportsValue,
			@RequestParam(value = "contentQuery", defaultValue = "") String contentQuery,
			@RequestParam(value = "contentQueryEndpoint", defaultValue = "") String contentQueryEndpoint,
			@RequestParam(value = "contentQueryAuthenticate", defaultValue = "false") Boolean contentQueryAuthenticate,
			@RequestParam(value = "contentQueryUsername", defaultValue = "") String contentQueryUsername,
			@RequestParam(value = "contentQueryPassword", defaultValue = "") String contentQueryPassword,
			HttpServletRequest request) {
		setMinimalUIFlag(request, false);
		DomainConfig domainConfig;
		try {
			domainConfig = validateDomain(domain);
		} catch (Exception e) {
			throw new NotFoundException();
		}
		// Temporary folder for the request.
		File parentFolder = fileManager.createTemporaryFolderPath();

		File inputFile;
		List<FileInfo> userProvidedShapes = null;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("contentType", getContentType(domainConfig));
        attributes.put("contentSyntax", getContentSyntax(domainConfig));
		attributes.put("externalArtifactInfo", domainConfig.getExternalArtifactInfoMap());
		attributes.put("loadImportsInfo", domainConfig.getUserInputForLoadImportsType());
        attributes.put("minimalUI", false);
        attributes.put("downloadType", getDownloadType(domainConfig));
        attributes.put("config", domainConfig);
        attributes.put("appConfig", appConfig);
		if (StringUtils.isNotBlank(validationType)) {
			attributes.put("validationTypeLabel", domainConfig.getTypeLabel().get(validationType));
		}
		boolean forceCleanup = false;
        try {
        	if (CONTENT_TYPE__QUERY.equals(contentType)) {
				contentSyntaxType = domainConfig.getQueryContentType();
			} else {
				if (contentSyntaxType.isEmpty() || contentSyntaxType.equals("empty")) {
					if (!contentType.equals(CONTENT_TYPE__EDITOR)) {
						contentSyntaxType = getExtensionContentType((contentType.equals(CONTENT_TYPE__FILE) ? file.getOriginalFilename() : uri));
					} else {
						logger.error("Provided content syntax type is not valid");
						attributes.put("message", "Provided content syntax type is not valid");
					}
				}
			}
        	if (!contentQuery.isEmpty() || !contentQueryEndpoint.isEmpty() || !contentQueryUsername.isEmpty() || !contentQueryPassword.isEmpty()) {
        		// Load input using SPARQL query.
				if (!domainConfig.isQueryAuthenticationMandatory() && !contentQueryAuthenticate) {
					// Empty the optional credentials
					contentQueryUsername = null;
					contentQueryPassword = null;
				}
				SparqlQueryConfig queryConfig = new SparqlQueryConfig(contentQueryEndpoint, contentQuery, contentQueryUsername, contentQueryPassword, contentSyntaxType);
				queryConfig = inputHelper.validateSparqlConfiguration(domainConfig, queryConfig);
				inputFile = fileManager.getContentFromSparqlEndpoint(queryConfig, parentFolder, FILE_NAME__INPUT).toFile();
			} else {
        		// Load input using file, URI or editor.
				inputFile = getInputFile(contentType, file.getInputStream(), uri, string, contentSyntaxType, parentFolder);
			}
			if (validationType.isBlank()) {
				validationType = null;
			}
			if (domainConfig.hasMultipleValidationTypes() && (validationType == null || !domainConfig.getType().contains(validationType))) {
				// A validation type is required.
				attributes.put("message", "Provided validation type is not valid");
			} else {
				if (inputFile != null) {
					if (hasExternalShapes(domainConfig, validationType)) {
						userProvidedShapes = getExternalShapes(externalContentType, externalFiles, externalUri, externalFilesSyntaxType, parentFolder);
					} else {
						userProvidedShapes = Collections.emptyList();
					}
	
					if(domainConfig.getUserInputForLoadImportsType().get(validationType) == ExternalArtifactSupport.NONE) {
						loadImportsValue = null;
					}
					loadImportsValue = inputHelper.validateLoadInputs(domainConfig, loadImportsValue, validationType);
					SHACLValidator validator = ctx.getBean(SHACLValidator.class, inputFile, validationType, contentSyntaxType, userProvidedShapes, loadImportsValue, domainConfig);
					org.apache.jena.rdf.model.Model reportModel = validator.validateAll();
					TAR tarReport = Utils.getTAR(reportModel, domainConfig);
					if (tarReport.getReports().getInfoOrWarningOrError().size() <= domainConfig.getMaximumReportsForDetailedOutput()) {
						File pdfReport = new File(parentFolder, FILE_NAME__REPORT +".pdf");
						reportGenerator.writeReport(domainConfig, tarReport, pdfReport);
					}
					String fileName;
					if (CONTENT_TYPE__FILE.equals(contentType)) {
						fileName = file.getOriginalFilename();
					} else if (CONTENT_TYPE__URI.equals(contentType)) {
						fileName = uri;
					} else {
						// Query or editor.
						fileName = "-";
					}
					String extension = fileManager.getFileExtension(domainConfig.getDefaultReportSyntax());
					try (FileWriter out = new FileWriter(fileManager.createFile(parentFolder, extension, FILE_NAME__REPORT).toFile())) {
						fileManager.writeRdfModel(out, reportModel, domainConfig.getDefaultReportSyntax());
					}
					try (FileWriter out = new FileWriter(fileManager.createFile(parentFolder, extension, FILE_NAME__SHAPES).toFile())) {
						fileManager.writeRdfModel(out, validator.getAggregatedShapes(), domainConfig.getDefaultReportSyntax());
					}
					// All ok - add attributes for the UI.
					attributes.put("reportID", parentFolder.getName());
					attributes.put("fileName", fileName);
					attributes.put("report", tarReport);
					attributes.put("date", tarReport.getDate().toString());
				}
			}
		} catch (ValidatorException e) {
			logger.error(e.getMessage(), e);
			attributes.put("message", e.getMessage());
			forceCleanup = true;
		} catch (Exception e) {
            logger.error("An error occurred during the validation [" + e.getMessage() + "]", e);
            if (e.getMessage() != null) {
				attributes.put("message", "An error occurred during the validation [" + e.getMessage() + "]");
			} else {
				attributes.put("message", "An error occurred during the validation");
			}
			forceCleanup = true;
        } finally {
        	/*
        	In the web UI case the cleanup cannot fully remove the temp folder as we need to keep the reports.
        	 */
        	if (forceCleanup) {
				FileUtils.deleteQuietly(parentFolder);
			} else {
				fileManager.removeContentToValidate(null, userProvidedShapes);
			}
		}
        return new ModelAndView("uploadForm", attributes);
    }
	
    @GetMapping(value = "/{domain}/uploadm")
    public ModelAndView uploadm(@PathVariable("domain") String domain, Model model, HttpServletRequest request) {
		setMinimalUIFlag(request, true);
    	DomainConfig domainConfig;
		try {
			domainConfig = validateDomain(domain);
		} catch (Exception e) {
			throw new NotFoundException();
		}
		if(!domainConfig.isSupportMinimalUserInterface()) {
			logger.error("Minimal user interface is not supported in this domain [" + domain + "].");
			throw new NotFoundException();
		}

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("contentType", getContentType(domainConfig));
        attributes.put("contentSyntax", getContentSyntax(domainConfig));
		attributes.put("externalArtifactInfo", domainConfig.getExternalArtifactInfoMap());
		attributes.put("loadImportsInfo", domainConfig.getUserInputForLoadImportsType());
        attributes.put("minimalUI", true);
        attributes.put("config", domainConfig);
        attributes.put("appConfig", appConfig);

        return new ModelAndView("uploadForm", attributes);
    }
    
	@PostMapping(value = "/{domain}/uploadm")
    public ModelAndView handleUploadM(@PathVariable("domain") String domain, 
    		@RequestParam("file") MultipartFile file,
    		@RequestParam(value = "uri", defaultValue = "") String uri,  
    		@RequestParam(value = "text-editor", defaultValue = "") String string,     		
    		@RequestParam(value = "contentType", defaultValue = "") String contentType,  
    		@RequestParam(value = "validationType", defaultValue = "") String validationType, 
    		@RequestParam(value = "contentSyntaxType", defaultValue = "") String contentSyntaxType,
    		@RequestParam(value = "contentType-external_default", required = false) String[] externalContentType,
    		@RequestParam(value = "inputFile-external_default", required= false) MultipartFile[] externalFiles,
    		@RequestParam(value = "uri-external_default", required = false) String[] externalUri,
    		@RequestParam(value = "contentSyntaxType-external_default", required = false) String[] externalFilesSyntaxType,
            @RequestParam(value = "loadImportsCheck", required = false, defaultValue = "false") Boolean loadImportsValue,
			@RequestParam(value = "contentQuery", defaultValue = "") String contentQuery,
			@RequestParam(value = "contentQueryEndpoint", defaultValue = "") String contentQueryEndpoint,
			@RequestParam(value = "contentQueryAuthenticate", defaultValue = "false") Boolean contentQueryAuthenticate,
			@RequestParam(value = "contentQueryUsername", defaultValue = "") String contentQueryUsername,
			@RequestParam(value = "contentQueryPassword", defaultValue = "") String contentQueryPassword,
			HttpServletRequest request) {
		setMinimalUIFlag(request, true);
		ModelAndView mv = handleUpload(domain, file, uri, string, contentType, validationType, contentSyntaxType, externalContentType, externalFiles, externalUri, externalFilesSyntaxType, loadImportsValue, contentQuery, contentQueryEndpoint, contentQueryAuthenticate, contentQueryUsername, contentQueryPassword, request);
		
		Map<String, Object> attributes = mv.getModel();
        attributes.put("minimalUI", true);

        return new ModelAndView("uploadForm", attributes);	
	}

	private void setMinimalUIFlag(HttpServletRequest request, boolean isMinimal) {
		if (request.getAttribute(IS_MINIMAL) == null) {
			request.setAttribute(IS_MINIMAL, isMinimal);
		}
	}

	private boolean hasExternalShapes(DomainConfig domainConfig, String validationType) {
    	if (validationType == null) {
			validationType = domainConfig.getType().get(0);
		}
    	return domainConfig.getShapeInfo(validationType).getExternalArtifactSupport() != ExternalArtifactSupport.NONE;
	}

	private File getInputFile(String contentType, InputStream inputStream, String uri, String string, String contentSyntaxType, File tmpFolder) throws IOException {
		File inputFile = null;
		switch(contentType) {
			case CONTENT_TYPE__FILE:
		    	inputFile = this.fileManager.getFileFromInputStream(tmpFolder, inputStream, contentSyntaxType, FILE_NAME__INPUT);
				break;
			
			case CONTENT_TYPE__URI:
				inputFile = this.fileManager.getFileFromURL(tmpFolder, uri, fileManager.getFileExtension(contentSyntaxType), FILE_NAME__INPUT);
				break;
				
			case CONTENT_TYPE__EDITOR:
				inputFile = this.fileManager.getFileFromString(tmpFolder, string, contentSyntaxType, FILE_NAME__INPUT);
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
	
	private List<FileInfo> getExternalShapes(String[] externalContentType, MultipartFile[] externalFiles, String[] externalUri, String[] externalFilesSyntaxType, File parentFolder) throws IOException {
		List<FileInfo> shaclFiles = new ArrayList<>();

		if(externalContentType != null) {
			for(int i=0; i<externalContentType.length; i++) {
				File inputFile = null;
				String contentSyntaxType = "";
				if(externalFilesSyntaxType.length > i) {
					contentSyntaxType = externalFilesSyntaxType[i];
				}
	        	
				switch(externalContentType[i]) {
					case CONTENT_TYPE__FILE:
						if (!externalFiles[i].isEmpty()) {
							if(StringUtils.isEmpty(contentSyntaxType) || contentSyntaxType.equals("empty")) {
								contentSyntaxType = getExtensionContentType(externalFiles[i].getOriginalFilename());
							}
							inputFile = this.fileManager.getFileFromInputStream(parentFolder, externalFiles[i].getInputStream(), contentSyntaxType, null);
						}
						break;
					case CONTENT_TYPE__URI:
						if(externalUri.length>i && !externalUri[i].isEmpty()) {
							if(StringUtils.isEmpty(contentSyntaxType) || contentSyntaxType.equals("empty")) {
				        		contentSyntaxType = getExtensionContentType(externalUri[i]);
				        	}
							inputFile = this.fileManager.getFileFromURL(parentFolder, externalUri[i]);
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

    private List<KeyWithLabel> getContentType(DomainConfig config){
        List<KeyWithLabel> types = new ArrayList<>();

		types.add(new KeyWithLabel(CONTENT_TYPE__FILE, config.getLabel().getOptionContentFile()));
		types.add(new KeyWithLabel(CONTENT_TYPE__URI, config.getLabel().getOptionContentURI()));
		types.add(new KeyWithLabel(CONTENT_TYPE__EDITOR, config.getLabel().getOptionContentDirectInput()));
		
		return types;        
    }

	private List<KeyWithLabel> getDownloadType(DomainConfig config){
        List<KeyWithLabel> types = new ArrayList<>();
		types.add(new KeyWithLabel(DOWNLOAD_TYPE__REPORT, config.getLabel().getOptionDownloadReport()));
		types.add(new KeyWithLabel(DOWNLOAD_TYPE__SHAPES, config.getLabel().getOptionDownloadShapes()));
		types.add(new KeyWithLabel(DOWNLOAD_TYPE__CONTENT, config.getLabel().getOptionDownloadContent()));
		return types;
    }
    
    private List<KeyWithLabel> getContentSyntax(DomainConfig config) {
    	List<String> contentSyntax = config.getWebContentSyntax();    
    	if(contentSyntax.isEmpty()) {
    		contentSyntax = new ArrayList<>(appConfig.getContentSyntax());
    	}
        List<KeyWithLabel> types = new ArrayList<>();
    	
        for(String cs : contentSyntax) {
        	Lang lang = RDFLanguages.contentTypeToLang(cs);
			types.add(new KeyWithLabel(lang.getLabel(), lang.getContentType().getContentType()));
        }
    	
    	return types.stream().sorted(Comparator.comparing(KeyWithLabel::getKey)).collect(Collectors.toList());
    }

}
