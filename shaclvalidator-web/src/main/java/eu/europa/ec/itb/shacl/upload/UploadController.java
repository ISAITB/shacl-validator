package eu.europa.ec.itb.shacl.upload;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.shacl.*;
import eu.europa.ec.itb.shacl.util.ShaclValidatorUtils;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.ReportSpecs;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.ReportPair;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.ErrorResponseTypeEnum;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.BaseUploadController;
import eu.europa.ec.itb.validation.commons.web.CSPNonceFilter;
import eu.europa.ec.itb.validation.commons.web.Constants;
import eu.europa.ec.itb.validation.commons.web.KeyWithLabel;
import eu.europa.ec.itb.validation.commons.web.locale.CustomLocaleResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpClient;
import java.util.*;
import java.util.stream.Collectors;

import static eu.europa.ec.itb.validation.commons.web.Constants.*;

/**
 * Controller to manage the validator's web user interface.
 */
@Controller
@RestController
public class UploadController extends BaseUploadController<DomainConfig, DomainConfigCache> {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

    private static final String CONTENT_TYPE_QUERY = "queryType" ;
    private static final String PARAM_CONTENT_SYNTAX = "contentSyntax";
    private static final String PARAM_LOAD_IMPORTS_INFO = "loadImportsInfo";
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
    private FileManager fileManager;
    @Autowired
    private ApplicationConfig appConfig;
    @Autowired
    private ApplicationContext ctx;
    @Autowired
    private InputHelper inputHelper;
    @Autowired
    private CustomLocaleResolver localeResolver;

    /**
     * Prevent parameter values that contain commas to be interpreted as separate values.
     *
     * @param binder The data binder registry.
     */
    @SuppressWarnings("DataFlowIssue")
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Explicitly pass null to override the default (",").
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor(null));
    }

    /**
     * Prepare the upload page.
     *
     * @param domain The domain name.
     * @param request The received request.
     * @param response The produced response.
     * @return The model and view information.
     */
    @GetMapping(value = "/{domain}/upload")
    public ModelAndView upload(@PathVariable("domain") String domain, HttpServletRequest request, HttpServletResponse response) {
        var domainConfig = getDomainConfig(request);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(PARAM_CONTENT_SYNTAX, getContentSyntax(domainConfig));
        attributes.put(PARAM_EXTERNAL_ARTIFACT_INFO, domainConfig.getExternalArtifactInfoMap());
        attributes.put(PARAM_LOAD_IMPORTS_INFO, domainConfig.getUserInputForLoadImportsType());
        attributes.put(PARAM_MINIMAL_UI, isMinimalUI(request));
        attributes.put(PARAM_DOMAIN_CONFIG, domainConfig);
        attributes.put(PARAM_APP_CONFIG, appConfig);
        var localisationHelper = new LocalisationHelper(domainConfig, localeResolver.resolveLocale(request, response, domainConfig, appConfig));
        attributes.put(PARAM_LOCALISER, localisationHelper);
        attributes.put(PARAM_HTML_BANNER_EXISTS, localisationHelper.propertyExists("validator.bannerHtml"));
        attributes.put(PARAM_JAVASCRIPT_EXTENSION_EXISTS, localisationHelper.propertyExists("validator.javascriptExtension"));
        attributes.put(PARAM_NONCE, request.getAttribute(CSPNonceFilter.CSP_NONCE_ATTRIBUTE));
        attributes.put(PARAM_LABEL_CONFIG, getDynamicLabelConfiguration(localisationHelper, domainConfig, Collections.emptyList(),
                List.of(
                        Pair.of("validator.label.includeExternalShapes", "externalIncludeText"),
                        Pair.of("validator.label.externalRulesTooltip", "externalIncludeTooltip"),
                        Pair.of("validator.label.externalShapesLabel", "external."+ TypedValidationArtifactInfo.DEFAULT_TYPE+".label"),
                        Pair.of("validator.label.externalShapesPlaceholder", "external."+ TypedValidationArtifactInfo.DEFAULT_TYPE+".placeholder")
                )
        ));
        return new ModelAndView(VIEW_UPLOAD_FORM, attributes);
    }

    /**
     * Prepare the upload page (minimal UI version).
     *
     * @param domain The domain name.
     * @param request The received request.
     * @param response The produced response.
     * @return The model and view information.
     */
    @GetMapping(value = "/{domain}/uploadm")
    public ModelAndView uploadMinimal(@PathVariable("domain") String domain, HttpServletRequest request, HttpServletResponse response) {
        return upload(domain, request, response);
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
     * @param contentQueryAuthenticate Whether authentication is needed for the SPARQL endpoint.
     * @param contentQueryUsername The username to use for authentication with the SPARQL endpoint.
     * @param contentQueryPassword The password to use for authentication with the SPARQL endpoint.
     * @param request The received request.
     * @param response The produced response.
     * @return The model and view information.
     */
    @PostMapping(value = "/{domain}/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public UploadResult handleUpload(@PathVariable("domain") String domain,
                                 @RequestParam(value = "file", required = false) MultipartFile file,
                                 @RequestParam(value = "uri", defaultValue = "") String uri,
                                 @RequestParam(value = "text", defaultValue = "") String string,
                                 @RequestParam(value = "contentType", defaultValue = "") String contentType,
                                 @RequestParam(value = "validationType", defaultValue = "") String validationType,
                                 @RequestParam(value = "contentSyntaxType", defaultValue = "") String contentSyntaxType,
                                 @RequestParam(value = "contentType-external_default", required = false, defaultValue = "") String[] externalContentType,
                                 @RequestParam(value = "inputFile-external_default", required= false) MultipartFile[] externalFiles,
                                 @RequestParam(value = "uri-external_default", required = false, defaultValue = "") String[] externalUri,
                                 @RequestParam(value = "text-external_default", required = false, defaultValue = "") String[] externalString,
                                 @RequestParam(value = "contentSyntaxType-external_default", required = false, defaultValue = "") String[] externalFilesSyntaxType,
                                 @RequestParam(value = "loadImportsCheck", required = false, defaultValue = "false") Boolean loadImportsValue,
                                 @RequestParam(value = "contentQuery", defaultValue = "") String contentQuery,
                                 @RequestParam(value = "contentQueryEndpoint", defaultValue = "") String contentQueryEndpoint,
                                 @RequestParam(value = "contentQueryAuthenticate", defaultValue = "false") Boolean contentQueryAuthenticate,
                                 @RequestParam(value = "contentQueryUsername", defaultValue = "") String contentQueryUsername,
                                 @RequestParam(value = "contentQueryPassword", defaultValue = "") String contentQueryPassword,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        var domainConfig = getDomainConfig(request);
        contentType = checkInputType(contentType, file, uri, string, contentQuery);
        // Temporary folder for the request.
        File parentFolder = fileManager.createTemporaryFolderPath();
        var localisationHelper = new LocalisationHelper(domainConfig, localeResolver.resolveLocale(request, response, domainConfig, appConfig));

        var result = new UploadResult();
        result.setContentSyntax(getContentSyntax(domainConfig));
        File inputFile = null;
        List<FileInfo> userProvidedShapes = null;
        boolean forceCleanup = false;
        try {
            // Check validation type.
            if (validationType.isBlank()) {
                validationType = null;
            }
            if (domainConfig.hasMultipleValidationTypes() && (validationType == null || !domainConfig.getType().contains(validationType))) {
                // A validation type is required.
                result.setMessage(localisationHelper.localise("validator.label.exception.providedValidationTypeInvalid"));
            } else {
                // Check provided content type.
                if (CONTENT_TYPE_QUERY.equals(contentType)) {
                    // Load input using SPARQL query.
                    contentSyntaxType = domainConfig.getQueryContentType();
                    if (!domainConfig.isQueryAuthenticationMandatory() && Boolean.FALSE.equals(contentQueryAuthenticate)) {
                        // Empty the optional credentials
                        contentQueryUsername = null;
                        contentQueryPassword = null;
                    }
                    SparqlQueryConfig queryConfig = new SparqlQueryConfig(contentQueryEndpoint, contentQuery, contentQueryUsername, contentQueryPassword, contentSyntaxType);
                    queryConfig = inputHelper.validateSparqlConfiguration(domainConfig, queryConfig);
                    inputFile = fileManager.getContentFromSparqlEndpoint(queryConfig, parentFolder, FILE_NAME_INPUT).toFile();
                } else {
                    var noContentSyntaxProvided = contentSyntaxType.isEmpty() || contentSyntaxType.equals(EMPTY);
                    switch (contentType) {
                        case CONTENT_TYPE_STRING:
                            if (noContentSyntaxProvided) {
                                logger.error("Provided content syntax type is not valid");
                                result.setMessage(localisationHelper.localise("validator.label.exception.providedContentSyntaxInvalid"));
                            } else {
                                inputFile = this.fileManager.getFileFromString(parentFolder, string, contentSyntaxType, FILE_NAME_INPUT);
                            }
                            break;
                        case CONTENT_TYPE_FILE:
                            Objects.requireNonNull(file, "The input file must be provided");
                            if (noContentSyntaxProvided) {
                                contentSyntaxType = getExtensionContentTypeForFileName(file.getOriginalFilename());
                            }
                            try (var stream = file.getInputStream()) {
                                inputFile = fileManager.getFileFromInputStream(parentFolder, stream, contentSyntaxType, FILE_NAME_INPUT);
                            }
                            break;
                        case CONTENT_TYPE_URI:
                            if (noContentSyntaxProvided) {
                                contentSyntaxType = getExtensionContentTypeForURL(uri);
                            }
                            FileInfo uriResult = this.fileManager.getFileFromURL(parentFolder, uri, fileManager.getFileExtension(contentSyntaxType), FILE_NAME_INPUT, null, null, null, getAcceptedContentTypes(contentSyntaxType), domainConfig.getHttpVersion());
                            inputFile = uriResult.getFile();
                            if (noContentSyntaxProvided) {
                                // Only override the content syntax if one has not been explicitly provided as part of the input.
                                contentSyntaxType = ShaclValidatorUtils.contentSyntaxToUse(contentSyntaxType, uriResult.getType());
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown content type [" + contentType + "]");
                    }
                }
            }
            if (inputFile != null) {
                // Proceed with the validation.
                if (hasExternalShapes(domainConfig, validationType)) {
                    userProvidedShapes = getExternalShapes(externalContentType, externalFiles, externalUri, externalString, externalFilesSyntaxType, parentFolder, domainConfig.getHttpVersion());
                } else {
                    userProvidedShapes = Collections.emptyList();
                }
                if (domainConfig.getUserInputForLoadImportsType().get(validationType) == ExternalArtifactSupport.NONE) {
                    loadImportsValue = null;
                }
                loadImportsValue = inputHelper.validateLoadInputs(domainConfig, loadImportsValue, validationType);
                SHACLValidator validator = ctx.getBean(SHACLValidator.class, inputFile, validationType, contentSyntaxType, userProvidedShapes, loadImportsValue, domainConfig, localisationHelper);
                ModelPair models = validator.validateAll();
                ReportPair tarReport = ShaclValidatorUtils.getTAR(ReportSpecs
                        .builder(models.getInputModel(), models.getReportModel(), localisationHelper, domainConfig, validator.getValidationType())
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
                // Report if needed an error on owl:imports having failed.
                if (validator.hasErrorsDuringOwlImports()) {
                    // We always add the error messages as they will be included as (hidden) additional input.
                    result.setAdditionalErrorMessages(validator.getErrorsWhileLoadingOwlImports());
                    if (validator.hasErrorsWhileLoadingOwlImportsToReport() && result.getMessage() == null && domainConfig.getResponseForImportedShapeFailure(validationType) == ErrorResponseTypeEnum.WARN) {
                        result.setMessage(localisationHelper.localise("validator.label.exception.failureToLoadRemoteArtefacts"));
                        result.setMessageIsError(false);
                    }
                }
                // All ok - collect data for the UI.
                result.populateCommon(localisationHelper, validationType, domainConfig, parentFolder.getName(),
                        fileName, tarReport.getDetailedReport(), tarReport.getAggregateReport(),
                        prepareTranslations(localisationHelper, tarReport.getDetailedReport(), domainConfig));
            }
        } catch (ExtendedValidatorException e) {
            logger.error(e.getMessageForLog(), e);
            result.setMessage(e.getMessageForDisplay(localisationHelper));
            result.setAdditionalErrorMessages(e.getAdditionalInformation());
            forceCleanup = true;
        } catch (ValidatorException e) {
            logger.error(e.getMessageForLog(), e);
            result.setMessage(e.getMessageForDisplay(localisationHelper));
            forceCleanup = true;
        } catch (Exception e) {
            logger.error("An error occurred during the validation [" + e.getMessage() + "]", e);
            if (e.getMessage() != null) {
                result.setMessage(localisationHelper.localise("validator.label.exception.unexpectedErrorDuringValidationWithParams", e.getMessage()));
            } else {
                result.setMessage(localisationHelper.localise("validator.label.exception.unexpectedErrorDuringValidation"));
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
        return result;
    }

    /**
     * Handle the upload form's submission when the user interface is minimal.
     *
     * @see UploadController#handleUpload(String, MultipartFile, String, String, String, String, String, String[], MultipartFile[], String[], String[], String[], Boolean, String, String, Boolean, String, String, HttpServletRequest, HttpServletResponse)
     */
    @PostMapping(value = "/{domain}/uploadm", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public UploadResult handleUploadMinimal(@PathVariable("domain") String domain,
                                        @RequestParam(value = "file", required = false) MultipartFile file,
                                        @RequestParam(value = "uri", defaultValue = "") String uri,
                                        @RequestParam(value = "text", defaultValue = "") String string,
                                        @RequestParam(value = "contentType", defaultValue = "") String contentType,
                                        @RequestParam(value = "validationType", defaultValue = "") String validationType,
                                        @RequestParam(value = "contentSyntaxType", defaultValue = "") String contentSyntaxType,
                                        @RequestParam(value = "contentType-external_default", required = false, defaultValue = "") String[] externalContentType,
                                        @RequestParam(value = "inputFile-external_default", required= false) MultipartFile[] externalFiles,
                                        @RequestParam(value = "uri-external_default", required = false, defaultValue = "") String[] externalUri,
                                        @RequestParam(value = "text-external_default", required = false, defaultValue = "") String[] externalString,
                                        @RequestParam(value = "contentSyntaxType-external_default", required = false, defaultValue = "") String[] externalFilesSyntaxType,
                                        @RequestParam(value = "loadImportsCheck", required = false, defaultValue = "false") Boolean loadImportsValue,
                                        @RequestParam(value = "contentQuery", defaultValue = "") String contentQuery,
                                        @RequestParam(value = "contentQueryEndpoint", defaultValue = "") String contentQueryEndpoint,
                                        @RequestParam(value = "contentQueryAuthenticate", defaultValue = "false") Boolean contentQueryAuthenticate,
                                        @RequestParam(value = "contentQueryUsername", defaultValue = "") String contentQueryUsername,
                                        @RequestParam(value = "contentQueryPassword", defaultValue = "") String contentQueryPassword,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        return handleUpload(domain, file, uri, string, contentType, validationType, contentSyntaxType, externalContentType, externalFiles, externalUri, externalString, externalFilesSyntaxType, loadImportsValue, contentQuery, contentQueryEndpoint, contentQueryAuthenticate, contentQueryUsername, contentQueryPassword, request, response);
    }

    /**
     * Handle the upload form's submission when the user interface is embedded in another web page.
     *
     * @see UploadController#handleUpload(String, MultipartFile, String, String, String, String, String, String[], MultipartFile[], String[], String[], String[], Boolean, String, String, Boolean, String, String, HttpServletRequest, HttpServletResponse)
     */
    @PostMapping(value = "/{domain}/upload", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView handleUploadEmbedded(@PathVariable("domain") String domain,
                                             @RequestParam(value = "file", required = false) MultipartFile file,
                                             @RequestParam(value = "uri", defaultValue = "") String uri,
                                             @RequestParam(value = "text", defaultValue = "") String string,
                                             @RequestParam(value = "contentType", defaultValue = "") String contentType,
                                             @RequestParam(value = "validationType", defaultValue = "") String validationType,
                                             @RequestParam(value = "contentSyntaxType", defaultValue = "") String contentSyntaxType,
                                             @RequestParam(value = "contentType-external_default", required = false, defaultValue = "") String[] externalContentType,
                                             @RequestParam(value = "inputFile-external_default", required= false) MultipartFile[] externalFiles,
                                             @RequestParam(value = "uri-external_default", required = false, defaultValue = "") String[] externalUri,
                                             @RequestParam(value = "text-external_default", required = false, defaultValue = "") String[] externalString,
                                             @RequestParam(value = "contentSyntaxType-external_default", required = false, defaultValue = "") String[] externalFilesSyntaxType,
                                             @RequestParam(value = "loadImportsCheck", required = false, defaultValue = "false") Boolean loadImportsValue,
                                             @RequestParam(value = "contentQuery", defaultValue = "") String contentQuery,
                                             @RequestParam(value = "contentQueryEndpoint", defaultValue = "") String contentQueryEndpoint,
                                             @RequestParam(value = "contentQueryAuthenticate", defaultValue = "false") Boolean contentQueryAuthenticate,
                                             @RequestParam(value = "contentQueryUsername", defaultValue = "") String contentQueryUsername,
                                             @RequestParam(value = "contentQueryPassword", defaultValue = "") String contentQueryPassword,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        var uploadForm = upload(domain, request, response);
        var uploadResult = handleUpload(domain, file, uri, string, contentType, validationType, contentSyntaxType, externalContentType, externalFiles, externalUri, externalString, externalFilesSyntaxType, loadImportsValue, contentQuery, contentQueryEndpoint, contentQueryAuthenticate, contentQueryUsername, contentQueryPassword, request, response);
        uploadForm.getModel().put(Constants.PARAM_REPORT_DATA, writeResultToString(uploadResult));
        return uploadForm;
    }

    /**
     * Handle the upload form's submission when the user interface is minimal and embedded in another web page.
     *
     * @see UploadController#handleUpload(String, MultipartFile, String, String, String, String, String, String[], MultipartFile[], String[], String[], String[], Boolean, String, String, Boolean, String, String, HttpServletRequest, HttpServletResponse)
     */
    @PostMapping(value = "/{domain}/uploadm", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView handleUploadMinimalEmbedded(@PathVariable("domain") String domain,
                                             @RequestParam(value = "file", required = false) MultipartFile file,
                                             @RequestParam(value = "uri", defaultValue = "") String uri,
                                             @RequestParam(value = "text", defaultValue = "") String string,
                                             @RequestParam(value = "contentType", defaultValue = "") String contentType,
                                             @RequestParam(value = "validationType", defaultValue = "") String validationType,
                                             @RequestParam(value = "contentSyntaxType", defaultValue = "") String contentSyntaxType,
                                             @RequestParam(value = "contentType-external_default", required = false, defaultValue = "") String[] externalContentType,
                                             @RequestParam(value = "inputFile-external_default", required= false) MultipartFile[] externalFiles,
                                             @RequestParam(value = "uri-external_default", required = false, defaultValue = "") String[] externalUri,
                                             @RequestParam(value = "text-external_default", required = false, defaultValue = "") String[] externalString,
                                             @RequestParam(value = "contentSyntaxType-external_default", required = false, defaultValue = "") String[] externalFilesSyntaxType,
                                             @RequestParam(value = "loadImportsCheck", required = false, defaultValue = "false") Boolean loadImportsValue,
                                             @RequestParam(value = "contentQuery", defaultValue = "") String contentQuery,
                                             @RequestParam(value = "contentQueryEndpoint", defaultValue = "") String contentQueryEndpoint,
                                             @RequestParam(value = "contentQueryAuthenticate", defaultValue = "false") Boolean contentQueryAuthenticate,
                                             @RequestParam(value = "contentQueryUsername", defaultValue = "") String contentQueryUsername,
                                             @RequestParam(value = "contentQueryPassword", defaultValue = "") String contentQueryPassword,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        return handleUploadEmbedded(domain, file, uri, string, contentType, validationType, contentSyntaxType, externalContentType, externalFiles, externalUri, externalString, externalFilesSyntaxType, loadImportsValue, contentQuery, contentQueryEndpoint, contentQueryAuthenticate, contentQueryUsername, contentQueryPassword, request, response);
    }

    /**
     * Check and/or determine the input submission type based on the received information.
     *
     * @param contentType The declared content type.
     * @param file The received file input (or null).
     * @param uri The received URI input (or null).
     * @param string The received text input (or null).
     * @param query The received query input (or null).
     * @return The content type value to consider.
     */
    private String checkInputType(String contentType, MultipartFile file, String uri, String string, String query) {
        contentType = super.checkInputType(contentType, file, uri, string);
        if (StringUtils.isEmpty(contentType)) {
            if (StringUtils.isNotEmpty(query)) {
                contentType = CONTENT_TYPE_QUERY;
            } else {
                throw new IllegalArgumentException("No explicit content type was declared and determining it from the provided inputs was not possible.");
            }
        }
        return contentType;
    }

    /**
     * Prepare translations for the UI.
     *
     * @param helper The localisation helper to use.
     * @param report The detailed report.
     * @return The translation object.
     */
    private Translations prepareTranslations(LocalisationHelper helper, TAR report, DomainConfig domainConfig) {
        var translations = new Translations(helper, report, domainConfig);
        translations.setDownloadReportButton(helper.localise("validator.label.downloadReportButton"));
        translations.setDownloadShapesButton(helper.localise("validator.label.downloadShapesButton"));
        translations.setDownloadInputButton(helper.localise("validator.label.downloadInputButton"));
        return translations;
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
     * Determine the RDF content type (mime type) from the provided file name.
     *
     * @param filename The file name to check.
     * @return The extracted mime type.
     */
    private String getExtensionContentTypeForFileName(String filename) {
        String contentType = null;
        Lang lang = RDFLanguages.filenameToLang(filename);
        if(lang != null) {
            contentType = lang.getContentType().getContentTypeStr();
        }
        return contentType;
    }

    /**
     * Determine the RDF content type (mime type) from the provided URL.
     *
     * @param urlString The URL to check.
     * @return The extracted mime type.
     */
    private String getExtensionContentTypeForURL(String urlString) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new ValidatorException("validator.label.exception.unableToProcessURI");
        }
        return getExtensionContentTypeForFileName(url.getPath());
    }

    /**
     * Store and return the list of user-provided SHACL shape files to consider.
     *
     * @param externalContentType Per shape file, the way in which it should be extracted.
     * @param externalFiles The shapes as uploaded files.
     * @param externalUri The shapes as URIs.
     * @param externalString The shapes as direct input.
     * @param externalFilesSyntaxType The syntax (mime type) of each shape file.
     * @param parentFolder The temp folder to use for storing the shape files.
     * @param httpVersion The HTTP version to use for remote lookups.
     * @return The list of stored shape files to be used.
     * @throws IOException If an IO error occurs.
     */
    private List<FileInfo> getExternalShapes(String[] externalContentType, MultipartFile[] externalFiles, String[] externalUri, String[] externalString, String[] externalFilesSyntaxType, File parentFolder, HttpClient.Version httpVersion) throws IOException {
        List<FileInfo> shapeFiles = new ArrayList<>();
        if (externalContentType != null) {
            for (int i=0; i<externalContentType.length; i++) {
                if (StringUtils.isNotBlank(externalContentType[i])) {
                    File inputFile = null;
                    String contentSyntaxType = "";
                    if (externalFilesSyntaxType.length > i) {
                        contentSyntaxType = externalFilesSyntaxType[i];
                    }
                    if (CONTENT_TYPE_FILE.equals(externalContentType[i])) {
                        if (!externalFiles[i].isEmpty()) {
                            if (StringUtils.isEmpty(contentSyntaxType) || contentSyntaxType.equals(EMPTY)) {
                                contentSyntaxType = getExtensionContentTypeForFileName(externalFiles[i].getOriginalFilename());
                            }
                            try (var stream = externalFiles[i].getInputStream()) {
                                inputFile = this.fileManager.getFileFromInputStream(parentFolder, stream, contentSyntaxType, null);
                            }
                        }
                    } else if (CONTENT_TYPE_URI.equals(externalContentType[i]) && externalUri.length > i && !externalUri[i].isEmpty()) {
                        boolean noContentSyntaxProvided = StringUtils.isEmpty(contentSyntaxType) || contentSyntaxType.equals(EMPTY);
                        if (noContentSyntaxProvided) {
                            contentSyntaxType = getExtensionContentTypeForURL(externalUri[i]);
                        }
                        FileInfo uriResult = this.fileManager.getFileFromURL(parentFolder, externalUri[i], null, null, null, null, null, getAcceptedContentTypes(contentSyntaxType), httpVersion);
                        inputFile = uriResult.getFile();
                        if (noContentSyntaxProvided) {
                            // Only override the content syntax if one has not been explicitly provided as part of the input.
                            contentSyntaxType = ShaclValidatorUtils.contentSyntaxToUse(contentSyntaxType, uriResult.getType());
                        }
                    } else if (CONTENT_TYPE_STRING.equals(externalContentType[i]) && externalString.length > i && !externalString[i].isEmpty()) {
                        inputFile = this.fileManager.getFileFromString(parentFolder, externalString[i]);
                    }
                    if (inputFile != null) {
                        shapeFiles.add(new FileInfo(inputFile, contentSyntaxType));
                    }
                }
            }
        }
        return shapeFiles;
    }

    /**
     * Return a list of the content types to specify in a remote URI's request Accept header.
     * <p>
     * This either wraps the provided content type or includes all supported RDF content types.
     *
     * @param providedContentType The user-provided content type.
     * @return The content types to use.
     */
    private List<String> getAcceptedContentTypes(String providedContentType) {
        if (Objects.equals(providedContentType, EMPTY)) {
            providedContentType = null;
        }
        return appConfig.getAcceptedContentTypes(providedContentType);
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
