package eu.europa.ec.itb.shacl.upload;

import eu.europa.ec.itb.shacl.*;
import eu.europa.ec.itb.shacl.util.ShaclValidatorUtils;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.ReportSpecs;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import eu.europa.ec.itb.validation.commons.*;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfig;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.KeyWithLabel;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import eu.europa.ec.itb.validation.commons.web.locale.CustomLocaleResolver;
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
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static eu.europa.ec.itb.validation.commons.web.Constants.*;

/**
 * Controller to manage the validator's web user interface.
 */
@Controller
public class UploadController {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

    private static final String CONTENT_TYPE_FILE = "fileType" ;
    private static final String CONTENT_TYPE_URI = "uriType" ;
    private static final String CONTENT_TYPE_EDITOR = "stringType" ;
    private static final String CONTENT_TYPE_QUERY = "queryType" ;
    private static final String PARAM_CONTENT_SYNTAX = "contentSyntax";
    private static final String PARAM_LOAD_IMPORTS_INFO = "loadImportsInfo";
    private static final String PARAM_CONTENT_TYPE = "contentType";
    private static final String PARAM_REPORT_ID = "reportID";
    private static final String EMPTY = "empty";
    static final String DOWNLOAD_TYPE_REPORT = "reportType";
    static final String DOWNLOAD_TYPE_SHAPES = "shapesType";
    static final String DOWNLOAD_TYPE_CONTENT = "contentType";
    static final String FILE_NAME_INPUT = "inputFile";
    static final String FILE_NAME_REPORT = "reportFile";
    static final String FILE_NAME_PDF_REPORT_DETAILED = "pdfReportFileDetailed";
    static final String FILE_NAME_CSV_REPORT_DETAILED = "csvReportFileDetailed";
    static final String FILE_NAME_PDF_REPORT_AGGREGATED = "pdfReportFileAggregated";
    static final String FILE_NAME_CSV_REPORT_AGGREGATED = "csvReportFileAggregated";
    static final String FILE_NAME_SHAPES = "shapesFile";
    static final String FILE_NAME_TAR = "tarFile";
    static final String FILE_NAME_TAR_AGGREGATE = "tarAggregateFile";

    @Autowired
    private FileManager fileManager = null;

    @Autowired
    private DomainConfigCache domainConfigs = null;

    @Autowired
    private ApplicationConfig appConfig = null;

    @Autowired
    private ApplicationContext ctx = null;

    @Autowired
    private InputHelper inputHelper = null;

    @Autowired
    private CustomLocaleResolver localeResolver;

    /**
     * Prepare the upload page.
     *
     * @param domain The domain name.
     * @param model The UI model.
     * @param request The received request.
     * @param response The produced response.
     * @return The model and view information.
     */
    @GetMapping(value = "/{domain}/upload")
    public ModelAndView upload(@PathVariable("domain") String domain, Model model, HttpServletRequest request, HttpServletResponse response) {
        setMinimalUIFlag(request, false);
        DomainConfig domainConfig;
        try {
            domainConfig = validateDomain(request, domain);
        } catch (Exception e) {
            throw new NotFoundException();
        }

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(PARAM_CONTENT_SYNTAX, getContentSyntax(domainConfig));
        attributes.put(PARAM_EXTERNAL_ARTIFACT_INFO, domainConfig.getExternalArtifactInfoMap());
        attributes.put(PARAM_LOAD_IMPORTS_INFO, domainConfig.getUserInputForLoadImportsType());
        attributes.put(PARAM_MINIMAL_UI, false);
        attributes.put(PARAM_DOMAIN_CONFIG, domainConfig);
        attributes.put(PARAM_APP_CONFIG, appConfig);
        var localisationHelper = new LocalisationHelper(domainConfig, localeResolver.resolveLocale(request, response, domainConfig, appConfig));
        attributes.put(PARAM_LOCALISER, localisationHelper);
        attributes.put(PARAM_CONTENT_TYPE, getContentType(localisationHelper));
        attributes.put(PARAM_HTML_BANNER_EXISTS, localisationHelper.propertyExists("validator.bannerHtml"));

        return new ModelAndView(VIEW_UPLOAD_FORM, attributes);
    }

    /**
     * Handle the upload form's submission.
     *
     * @param domain The domain name.
     * @param file The input file (if provided via file upload).
     * @param uri The input URI (if provided via remote URI).
     * @param string The input content (if provided via editor).
     * @param contentType The type of the provided content.
     * @param validationType The validation type.
     * @param contentSyntaxType The syntax for the content.
     * @param externalContentType The content type for user-provided shapes (those provided as URIs).
     * @param externalFiles The user-provided shapes (those provided as files).
     * @param externalUri The user-provided shapes (those provided as URIs).
     * @param externalFilesSyntaxType The content type for user-provided shapes (those provided as files).
     * @param loadImportsValue The load OWL imports from input flag.
     * @param contentQuery The SPARQL query to load the input with.
     * @param contentQueryEndpoint The SPARQL endpoint URL to query for the content.
     * @param contentQueryAuthenticate Whether or not authentication is needed for the SPARQL endpoint.
     * @param contentQueryUsername The username to use for authentication with the SPARQL endpoint.
     * @param contentQueryPassword The password to use for authentication with the SPARQL endpoint.
     * @param request The received request.
     * @param response The produced response.
     * @return The model and view information.
     */
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
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        setMinimalUIFlag(request, false);
        DomainConfig domainConfig;
        try {
            domainConfig = validateDomain(request, domain);
        } catch (Exception e) {
            throw new NotFoundException();
        }
        // Temporary folder for the request.
        File parentFolder = fileManager.createTemporaryFolderPath();
        var localisationHelper = new LocalisationHelper(domainConfig, localeResolver.resolveLocale(request, response, domainConfig, appConfig));
        File inputFile;
        List<FileInfo> userProvidedShapes = null;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(PARAM_CONTENT_SYNTAX, getContentSyntax(domainConfig));
        attributes.put(PARAM_EXTERNAL_ARTIFACT_INFO, domainConfig.getExternalArtifactInfoMap());
        attributes.put(PARAM_LOAD_IMPORTS_INFO, domainConfig.getUserInputForLoadImportsType());
        attributes.put(PARAM_MINIMAL_UI, false);
        attributes.put(PARAM_DOMAIN_CONFIG, domainConfig);
        attributes.put(PARAM_APP_CONFIG, appConfig);
        attributes.put(PARAM_LOCALISER, localisationHelper);
        attributes.put(PARAM_CONTENT_TYPE, getContentType(localisationHelper));
        attributes.put(PARAM_HTML_BANNER_EXISTS, localisationHelper.propertyExists("validator.bannerHtml"));

        if (StringUtils.isNotBlank(validationType)) {
            attributes.put(PARAM_VALIDATION_TYPE_LABEL, domainConfig.getCompleteTypeOptionLabel(validationType, localisationHelper));
        }
        boolean forceCleanup = false;
        try {
            if (CONTENT_TYPE_QUERY.equals(contentType)) {
                contentSyntaxType = domainConfig.getQueryContentType();
            } else {
                if (contentSyntaxType.isEmpty() || contentSyntaxType.equals(EMPTY)) {
                    if (!contentType.equals(CONTENT_TYPE_EDITOR)) {
                        contentSyntaxType = getExtensionContentType((contentType.equals(CONTENT_TYPE_FILE) ? file.getOriginalFilename() : uri));
                    } else {
                        logger.error("Provided content syntax type is not valid");
                        attributes.put(PARAM_MESSAGE, "Provided content syntax type is not valid");
                    }
                }
            }
            if (!contentQuery.isEmpty() || !contentQueryEndpoint.isEmpty() || !contentQueryUsername.isEmpty() || !contentQueryPassword.isEmpty()) {
                // Load input using SPARQL query.
                if (!domainConfig.isQueryAuthenticationMandatory() && Boolean.FALSE.equals(contentQueryAuthenticate)) {
                    // Empty the optional credentials
                    contentQueryUsername = null;
                    contentQueryPassword = null;
                }
                SparqlQueryConfig queryConfig = new SparqlQueryConfig(contentQueryEndpoint, contentQuery, contentQueryUsername, contentQueryPassword, contentSyntaxType);
                queryConfig = inputHelper.validateSparqlConfiguration(domainConfig, queryConfig);
                inputFile = fileManager.getContentFromSparqlEndpoint(queryConfig, parentFolder, FILE_NAME_INPUT).toFile();
            } else {
                // Load input using file, URI or editor.
                inputFile = getInputFile(contentType, file.getInputStream(), uri, string, contentSyntaxType, parentFolder);
            }
            if (validationType.isBlank()) {
                validationType = null;
            }
            if (domainConfig.hasMultipleValidationTypes() && (validationType == null || !domainConfig.getType().contains(validationType))) {
                // A validation type is required.
                attributes.put(PARAM_MESSAGE, "Provided validation type is not valid");
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
                    SHACLValidator validator = ctx.getBean(SHACLValidator.class, inputFile, validationType, contentSyntaxType, userProvidedShapes, loadImportsValue, domainConfig, localisationHelper);
                    ModelPair models = validator.validateAll();
                    ReportPair tarReport = ShaclValidatorUtils.getTAR(ReportSpecs
                            .builder(models.getInputModel(), models.getReportModel(), localisationHelper, domainConfig)
                            .produceAggregateReport()
                            .build()
                    );
                    fileManager.saveReport(tarReport.getDetailedReport(), fileManager.createFile(parentFolder, ".xml", FILE_NAME_TAR).toFile(), domainConfig);
                    fileManager.saveReport(tarReport.getAggregateReport(), fileManager.createFile(parentFolder, ".xml", FILE_NAME_TAR_AGGREGATE).toFile(), domainConfig);
                    String fileName;
                    if (CONTENT_TYPE_FILE.equals(contentType)) {
                        fileName = file.getOriginalFilename();
                    } else if (CONTENT_TYPE_URI.equals(contentType)) {
                        fileName = uri;
                    } else {
                        // Query or editor.
                        fileName = "-";
                    }
                    String extension = fileManager.getFileExtension(domainConfig.getDefaultReportSyntax());
                    try (FileWriter out = new FileWriter(fileManager.createFile(parentFolder, extension, FILE_NAME_REPORT).toFile())) {
                        fileManager.writeRdfModel(out, models.getReportModel(), domainConfig.getDefaultReportSyntax());
                    }
                    try (FileWriter out = new FileWriter(fileManager.createFile(parentFolder, extension, FILE_NAME_SHAPES).toFile())) {
                        fileManager.writeRdfModel(out, validator.getAggregatedShapes(), domainConfig.getDefaultReportSyntax());
                    }
                    // All ok - add attributes for the UI.
                    attributes.put(PARAM_REPORT_ID, parentFolder.getName());
                    attributes.put(PARAM_FILE_NAME, fileName);
                    attributes.put(PARAM_REPORT, tarReport.getDetailedReport());
                    attributes.put(PARAM_AGGREGATE_REPORT, tarReport.getAggregateReport());
                    attributes.put(PARAM_SHOW_AGGREGATE_REPORT, Utils.aggregateDiffers(tarReport.getDetailedReport(), tarReport.getAggregateReport()));
                    attributes.put(PARAM_DATE, tarReport.getDetailedReport().getDate().toString());
                }
            }
        } catch (ValidatorException e) {
            logger.error(e.getMessageForLog(), e);
            attributes.put(PARAM_MESSAGE, e.getMessageForDisplay(localisationHelper));
            forceCleanup = true;
        } catch (Exception e) {
            logger.error("An error occurred during the validation [" + e.getMessage() + "]", e);
            if (e.getMessage() != null) {
                attributes.put(PARAM_MESSAGE, localisationHelper.localise("validator.label.exception.unexpectedErrorDuringValidationWithParams", e.getMessage()));
            } else {
                attributes.put(PARAM_MESSAGE, localisationHelper.localise("validator.label.exception.unexpectedErrorDuringValidation"));
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
        return new ModelAndView(VIEW_UPLOAD_FORM, attributes);
    }

    /**
     * Prepare the upload page (minimal UI version).
     *
     * @param domain The domain name.
     * @param model The UI model.
     * @param request The received request.
     * @param response The produced response.
     * @return The model and view information.
     */
    @GetMapping(value = "/{domain}/uploadm")
    public ModelAndView uploadm(@PathVariable("domain") String domain, Model model, HttpServletRequest request, HttpServletResponse response) {
        setMinimalUIFlag(request, true);
        DomainConfig domainConfig;
        try {
            domainConfig = validateDomain(request, domain);
        } catch (Exception e) {
            throw new NotFoundException();
        }
        if(!domainConfig.isSupportMinimalUserInterface()) {
            logger.error("Minimal user interface is not supported in this domain [{}].", domain);
            throw new NotFoundException();
        }

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(PARAM_CONTENT_SYNTAX, getContentSyntax(domainConfig));
        attributes.put(PARAM_EXTERNAL_ARTIFACT_INFO, domainConfig.getExternalArtifactInfoMap());
        attributes.put(PARAM_LOAD_IMPORTS_INFO, domainConfig.getUserInputForLoadImportsType());
        attributes.put(PARAM_MINIMAL_UI, true);
        attributes.put(PARAM_DOMAIN_CONFIG, domainConfig);
        attributes.put(PARAM_APP_CONFIG, appConfig);
        var localisationHelper = new LocalisationHelper(domainConfig, localeResolver.resolveLocale(request, response, domainConfig, appConfig));
        attributes.put(PARAM_LOCALISER, localisationHelper);
        attributes.put(PARAM_CONTENT_TYPE, getContentType(localisationHelper));
        attributes.put(PARAM_HTML_BANNER_EXISTS, localisationHelper.propertyExists("validator.bannerHtml"));

        return new ModelAndView(VIEW_UPLOAD_FORM, attributes);
    }

    /**
     * Handle the upload form's submission.
     *
     * @param domain The domain name.
     * @param file The input file (if provided via file upload).
     * @param uri The input URI (if provided via remote URI).
     * @param string The input content (if provided via editor).
     * @param contentType The type of the provided content.
     * @param validationType The validation type.
     * @param contentSyntaxType The syntax for the content.
     * @param externalContentType The content type for user-provided shapes (those provided as URIs).
     * @param externalFiles The user-provided shapes (those provided as files).
     * @param externalUri The user-provided shapes (those provided as URIs).
     * @param externalFilesSyntaxType The content type for user-provided shapes (those provided as files).
     * @param loadImportsValue The load OWL imports from input flag.
     * @param contentQuery The SPARQL query to load the input with.
     * @param contentQueryEndpoint The SPARQL endpoint URL to query for the content.
     * @param contentQueryAuthenticate Whether or not authentication is needed for the SPARQL endpoint.
     * @param contentQueryUsername The username to use for authentication with the SPARQL endpoint.
     * @param contentQueryPassword The password to use for authentication with the SPARQL endpoint.
     * @param request The received request.
     * @param response The produced response.
     * @return The model and view information.
     */
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
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        setMinimalUIFlag(request, true);
        ModelAndView mv = handleUpload(domain, file, uri, string, contentType, validationType, contentSyntaxType, externalContentType, externalFiles, externalUri, externalFilesSyntaxType, loadImportsValue, contentQuery, contentQueryEndpoint, contentQueryAuthenticate, contentQueryUsername, contentQueryPassword, request, response);

        Map<String, Object> attributes = mv.getModel();
        attributes.put(PARAM_MINIMAL_UI, true);

        return new ModelAndView(VIEW_UPLOAD_FORM, attributes);
    }

    /**
     * Record whether the current request is through a minimal UI.
     *
     * @param request The current request.
     * @param isMinimal True in case of the minimal UI being used.
     */
    private void setMinimalUIFlag(HttpServletRequest request, boolean isMinimal) {
        if (request.getAttribute(IS_MINIMAL) == null) {
            request.setAttribute(IS_MINIMAL, isMinimal);
        }
    }

    /**
     * Check if the given domain and validation type support user-provided SHACL shapes.
     *
     * @param domainConfig The domain configuration.
     * @param validationType The validation type.
     * @return True if user-provided shapes are supported.
     */
    private boolean hasExternalShapes(DomainConfig domainConfig, String validationType) {
        if (validationType == null) {
            validationType = domainConfig.getType().get(0);
        }
        return domainConfig.getShapeInfo(validationType).getExternalArtifactSupport() != ExternalArtifactSupport.NONE;
    }

    /**
     * Get the content to validate as a file stored on the validator's temp file system.
     *
     * @param contentType The way to interpret the content.
     * @param inputStream Input as a stream.
     * @param uri Input as a URI.
     * @param string Input as directly provided content.
     * @param contentSyntaxType The input's content type (mime type).
     * @param tmpFolder The temp folder to use for storing the input.
     * @return The input to use for the validation.
     * @throws IOException If an IO error occurs.
     */
    private File getInputFile(String contentType, InputStream inputStream, String uri, String string, String contentSyntaxType, File tmpFolder) throws IOException {
        File inputFile;
        switch (contentType) {
            case CONTENT_TYPE_FILE:
                inputFile = this.fileManager.getFileFromInputStream(tmpFolder, inputStream, contentSyntaxType, FILE_NAME_INPUT);
                break;
            case CONTENT_TYPE_URI:
                inputFile = this.fileManager.getFileFromURL(tmpFolder, uri, fileManager.getFileExtension(contentSyntaxType), FILE_NAME_INPUT);
                break;
            case CONTENT_TYPE_EDITOR:
                inputFile = this.fileManager.getFileFromString(tmpFolder, string, contentSyntaxType, FILE_NAME_INPUT);
                break;
            default: throw new IllegalArgumentException("Unknown content type ["+contentType+"]");
        }
        return inputFile;
    }

    /**
     * Determine the RDF content type (mime type) from the provided file name.
     *
     * @param filename The file name to check.
     * @return The extracted mime type.
     */
    private String getExtensionContentType(String filename) {
        String contentType = null;
        Lang lang = RDFLanguages.filenameToLang(filename);

        if(lang != null) {
            contentType = lang.getContentType().getContentTypeStr();
        }

        return contentType;
    }

    /**
     * Store and return the list of user-provided SHACL shape files to consider.
     *
     * @param externalContentType Per shape file, the way in which it should be extracted.
     * @param externalFiles The shapes as uploaded files.
     * @param externalUri The shapes as URIs.
     * @param externalFilesSyntaxType The syntax (mime type) of each shape file.
     * @param parentFolder The temp folder to use for storing the shape files.
     * @return The list of stored shape files to be used.
     * @throws IOException If an IO error occurs.
     */
    private List<FileInfo> getExternalShapes(String[] externalContentType, MultipartFile[] externalFiles, String[] externalUri, String[] externalFilesSyntaxType, File parentFolder) throws IOException {
        List<FileInfo> shapeFiles = new ArrayList<>();
        if (externalContentType != null) {
            for (int i=0; i<externalContentType.length; i++) {
                File inputFile = null;
                String contentSyntaxType = "";
                if (externalFilesSyntaxType.length > i) {
                    contentSyntaxType = externalFilesSyntaxType[i];
                }
                if (CONTENT_TYPE_FILE.equals(externalContentType[i])) {
                    if (!externalFiles[i].isEmpty()) {
                        if (StringUtils.isEmpty(contentSyntaxType) || contentSyntaxType.equals(EMPTY)) {
                            contentSyntaxType = getExtensionContentType(externalFiles[i].getOriginalFilename());
                        }
                        inputFile = this.fileManager.getFileFromInputStream(parentFolder, externalFiles[i].getInputStream(), contentSyntaxType, null);
                    }
                } else if (CONTENT_TYPE_URI.equals(externalContentType[i]) && externalUri.length > i && !externalUri[i].isEmpty()) {
                    if (StringUtils.isEmpty(contentSyntaxType) || contentSyntaxType.equals(EMPTY)) {
                        contentSyntaxType = getExtensionContentType(externalUri[i]);
                    }
                    inputFile = this.fileManager.getFileFromURL(parentFolder, externalUri[i]);
                }
                if (inputFile != null) {
                    FileInfo fi = new FileInfo(inputFile, contentSyntaxType);
                    shapeFiles.add(fi);
                }
            }
        }
        return shapeFiles;
    }

    /**
     * Validates that the domain exists and supports REST calls and return it if so.
     *
     *
     * @param request The HTTP request.
     * @param domain The domain to check.
     * @return The retrieved domain configuration.
     * @throws NotFoundException If the domain or REST API are unsupported.
     */
    private DomainConfig validateDomain(HttpServletRequest request, String domain) {
        DomainConfig config = domainConfigs.getConfigForDomainName(domain);
        if (config == null || !config.isDefined() || !config.getChannels().contains(ValidatorChannel.REST_API)) {
            logger.error("The following domain does not exist: {}", domain);
            throw new NotFoundException();
        }
        request.setAttribute(WebDomainConfig.DOMAIN_CONFIG_REQUEST_ATTRIBUTE, config);
        MDC.put(MDC_DOMAIN, domain);
        return config;
    }

    /**
     * The list of content provision options as key and label pairs to be displayed on the UI.
     *
     * @param localiser The localisation helper.
     * @return The list of options.
     */
    private List<KeyWithLabel> getContentType(LocalisationHelper localiser){
        return List.of(
                new KeyWithLabel(CONTENT_TYPE_FILE, localiser.localise("validator.label.optionContentFile")),
                new KeyWithLabel(CONTENT_TYPE_URI, localiser.localise("validator.label.optionContentURI")),
                new KeyWithLabel(CONTENT_TYPE_EDITOR, localiser.localise("validator.label.optionContentDirectInput"))
        );
    }

    /**
     * The list of syntax types for the input of user-provided shapes as key and label pairs to be used
     * on the UI.
     *
     * @param config The domain configuration.
     * @return The list of options.
     */
    private List<KeyWithLabel> getContentSyntax(DomainConfig config) {
        List<String> contentSyntax = config.getWebContentSyntax();
        if (contentSyntax.isEmpty()) {
            contentSyntax = new ArrayList<>(appConfig.getContentSyntax());
        }
        List<KeyWithLabel> types = new ArrayList<>();
        for (String cs : contentSyntax) {
            Lang lang = RDFLanguages.contentTypeToLang(cs);
            types.add(new KeyWithLabel(lang.getLabel(), lang.getContentType().getContentTypeStr()));
        }
        return types.stream().sorted(Comparator.comparing(KeyWithLabel::getKey)).collect(Collectors.toList());
    }

}
