/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.shacl.validation;

import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.http.DefaultHttpClient;
import com.apicatalog.jsonld.http.media.MediaType;
import com.apicatalog.jsonld.loader.FileLoader;
import com.apicatalog.jsonld.loader.HttpLoader;
import com.apicatalog.jsonld.loader.SchemeRouter;
import com.gitb.tr.BAR;
import com.gitb.tr.TestAssertionReportType;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.ExtendedValidatorException;
import eu.europa.ec.itb.shacl.ModelPair;
import eu.europa.ec.itb.shacl.config.CustomLocatorHTTP;
import eu.europa.ec.itb.shacl.util.ShaclValidatorUtils;
import eu.europa.ec.itb.shacl.util.StatementTranslator;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.config.DomainPluginConfigProvider;
import eu.europa.ec.itb.validation.commons.config.ErrorResponseTypeEnum;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.plugin.PluginManager;
import eu.europa.ec.itb.validation.plugin.ValidationPlugin;
import jakarta.xml.bind.JAXBElement;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.models.ModelMaker;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapStd;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.topbraid.jenax.util.JenaUtil;
import org.topbraid.shacl.validation.ValidationUtil;

import java.io.*;
import java.util.*;

import static eu.europa.ec.itb.shacl.util.ShaclValidatorUtils.getStatementSafe;
import static eu.europa.ec.itb.shacl.util.ShaclValidatorUtils.handleEquivalentContentSyntaxes;
import static eu.europa.ec.itb.shacl.validation.SHACLResources.VALIDATION_REPORT;
import static org.apache.jena.riot.lang.LangJSONLD11.JSONLD_OPTIONS;

/**
 * Component used to validate RDF content against SHACL shapes.
 */
@Component
@Scope("prototype")
public class SHACLValidator {

    /** The URI for result predicates. */
    public static final String RESULT_URI = "http://www.w3.org/ns/shacl#result";
    /** The URI for result message predicates. */
    public static final String RESULT_MESSAGE_URI = "http://www.w3.org/ns/shacl#resultMessage";
    private static final Logger LOG = LoggerFactory.getLogger(SHACLValidator.class);

    @Autowired
    private FileManager fileManager = null;
    @Autowired
    private PluginManager pluginManager = null;
    @Autowired
    private DomainPluginConfigProvider<DomainConfig> pluginConfigProvider = null;

    private final File inputFileToValidate;
    private boolean inputReady = false;
    private final DomainConfig domainConfig;
    private final LocalisationHelper localiser;
    private String validationType;
    private final String contentSyntax;
    private Lang contentSyntaxLang;
    private final List<FileInfo> externalShaclFiles;
    private Model aggregatedShapes;
    private Model importedShapes;
    private final boolean loadImports;
    private Model dataModel;
    private final List<String> errorsWhileLoadingOwlImports = new ArrayList<>();
    private boolean errorsWhileLoadingOwlImportsToReport = false;

    /**
     * Constructor to start the SHACL validator.
     * @param inputFileToValidate The input RDF (or other) content to validate.
     * @param validationType The type of validation to perform.
     * @param contentSyntax The mime type of the provided RDF content.
     * @param externalShaclFiles Any shapes to consider that are externally provided
     * @param loadImports True if OWL imports in the content should be loaded before validation.
     * @param domainConfig The domain in question.
     * @param localiser Helper class for localisations.
     */
    public SHACLValidator(File inputFileToValidate, String validationType, String contentSyntax, List<FileInfo> externalShaclFiles, boolean loadImports, DomainConfig domainConfig, LocalisationHelper localiser) {
    	this.contentSyntax = contentSyntax;
    	this.inputFileToValidate = inputFileToValidate;
        this.validationType = validationType;
        this.domainConfig = domainConfig;
        this.externalShaclFiles = externalShaclFiles;
        this.loadImports = loadImports;
        this.localiser = localiser;
        if (validationType == null) {
            this.validationType = domainConfig.getType().get(0);
        }
    }

    /**
     * @return The domain's identifier.
     */
    public String getDomain(){
        return this.domainConfig.getDomain();
    }

    /**
     * @return The requested validation type.
     */
    public String getValidationType(){
        return this.validationType;
    }
    
    /**
     * Validate the content against all SHACL shapes and plugins.
     *
     * @return The Jena model with the report.
     */
    public ModelPair validateAll() {
    	LOG.info("Starting validation..");
    	try {
            Model validationReport = setOverallResult(validateAgainstPlugins(validateAgainstShacl()));
            processForLocale(validationReport);
            return new ModelPair(dataModel, validationReport);
        } finally {
    	    LOG.info("Completed validation.");
        }
    }

    /**
     * If configured to do so, remove all translations from the report that are not relevant to the requested locale.
     *
     * @param report The report to process.
     */
    private void processForLocale(Model report) {
        if (!domainConfig.isReturnMessagesForAllLocales()) {
            // Filter out result messages for locales other than the requested one.
            var resultProperty = report.getProperty(RESULT_URI);
            var resultMessageProperty = report.getProperty(RESULT_MESSAGE_URI);
            var resultIterator = report.listObjectsOfProperty(resultProperty);
            while (resultIterator.hasNext()) {
                var node = resultIterator.next();
                var statementIterator = report.listStatements(node.asResource(), resultMessageProperty, (RDFNode) null);
                var translator = new StatementTranslator();
                while (statementIterator.hasNext()) {
                    translator.processStatement(statementIterator.next());
                }
                report.remove(translator.getTranslation(localiser.getLocale()).getUnmatchedStatements());
            }
        }
    }

    /**
     * Prepare the input for any configured custom validator plugins.
     *
     * @param pluginTmpFolder The temp folder to use for plugin processing.
     * @return The request to pass to the plugin(s).
     */
    private ValidateRequest preparePluginInput(File pluginTmpFolder) {
        // The content to validate is provided to plugins as a copu of the content in RDF/XML (for simpler processing).
        File pluginInputFile = new File(pluginTmpFolder, UUID.randomUUID() +".rdf");
        Lang contentSyntaxToUse = contextSyntaxToUse();
        if (Lang.RDFXML.equals(contentSyntaxToUse)) {
            // Use file as-is.
            try {
                FileUtils.copyFile(getInputFileToUse(), pluginInputFile);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to copy input file for plugin", e);
            }
        } else {
            // Make a converted copy.
            try (FileInputStream in = new FileInputStream(getInputFileToUse()); FileWriter out = new FileWriter(pluginInputFile)) {
                Model fileModel = readModel(in, contentSyntaxToUse, null);
                fileManager.writeRdfModel(out, fileModel, Lang.RDFXML.getContentType().getContentTypeStr());
            } catch (IOException e) {
                throw new IllegalStateException("Unable to convert input file for plugin", e);
            }
        }
        ValidateRequest request = new ValidateRequest();
        request.getInput().add(Utils.createInputItem("contentToValidate", pluginInputFile.getAbsolutePath()));
        request.getInput().add(Utils.createInputItem("domain", domainConfig.getDomainName()));
        request.getInput().add(Utils.createInputItem("validationType", validationType));
        request.getInput().add(Utils.createInputItem("tempFolder", pluginTmpFolder.getAbsolutePath()));
        request.getInput().add(Utils.createInputItem("locale", localiser.getLocale().toString()));
        return request;
    }

    /**
     * Ensure that violations in the report always result in an overall report failure.
     *
     * @param validationReport The original report.
     * @return The updated report.
     */
    private Model setOverallResult(Model validationReport) {
        Resource reportResource = validationReport.listSubjectsWithProperty(RDF.type, VALIDATION_REPORT).nextResource();
        if (reportResource != null) {
            Statement conformsStatement = reportResource.getProperty(SHACLResources.CONFORMS);
            if (conformsStatement.getBoolean() && hasViolations(validationReport)) {
                conformsStatement.changeLiteralObject(false);
            }
        }
        return validationReport;
    }

    /**
     * Check to see whether the provided report includes at least one violation.
     *
     * @param validationReport The report to check.
     * @return The check result.
     */
    private boolean hasViolations(Model validationReport) {
        NodeIterator resultIterator = validationReport.listObjectsOfProperty(validationReport.getProperty(RESULT_URI));
        while (resultIterator.hasNext()) {
            RDFNode node = resultIterator.next();
            StmtIterator statementIterator = validationReport.listStatements(node.asResource(), null, (RDFNode) null);
            while (statementIterator.hasNext()) {
                Statement statement = statementIterator.next();
                if (statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#resultSeverity")) {
                    String severity = getStatementSafe(statement);
                    if (ShaclValidatorUtils.isErrorSeverity(severity)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Validate the provided input against the configured plugins.
     *
     * @param validationReport The validation report to add produced plugin reports to.
     * @return The extended validation report.
     */
    private Model validateAgainstPlugins(Model validationReport) {
        ValidationPlugin[] plugins =  pluginManager.getPlugins(pluginConfigProvider.getPluginClassifier(domainConfig, validationType));
        if (plugins != null && plugins.length > 0) {
            File pluginTmpFolder = new File(inputFileToValidate.getParentFile(), UUID.randomUUID().toString());
            try {
                pluginTmpFolder.mkdirs();
                ValidateRequest pluginInput = preparePluginInput(pluginTmpFolder);
                for (ValidationPlugin plugin: plugins) {
                    String pluginName = plugin.getName();
                    ValidationResponse response = plugin.validate(pluginInput);
                    if (response != null && response.getReport() != null && response.getReport().getReports() != null) {
                        LOG.info("Plugin [{}] produced [{}] report item(s).", pluginName, response.getReport().getReports().getInfoOrWarningOrError().size());
                        Resource reportResource = validationReport.listSubjectsWithProperty(RDF.type, VALIDATION_REPORT).nextResource();
                        if (!response.getReport().getReports().getInfoOrWarningOrError().isEmpty()) {
                            // Ensure the overall result is set to a failure if needed.
                            Statement conformsStatement = reportResource.getProperty(SHACLResources.CONFORMS);
                            if (conformsStatement.getBoolean()) {
                                conformsStatement.changeLiteralObject(false);
                            }
                            // Add plugin results to report.
                            List<Statement> statements = new ArrayList<>();
                            for (JAXBElement<TestAssertionReportType> item: response.getReport().getReports().getInfoOrWarningOrError()) {
                                if (item.getValue() instanceof BAR barItem) {
                                    if (StringUtils.isBlank(barItem.getLocation())) {
                                        LOG.warn("Plugin [{}] report item without location. Skipping.", pluginName);
                                    } else if (StringUtils.isBlank(barItem.getAssertionID())) {
                                        LOG.warn("Plugin [{}] report item without assertion ID. Skipping.", pluginName);
                                    } else {
                                        Resource itemResource = validationReport.createResource();
                                        statements.add(validationReport.createStatement(reportResource, SHACLResources.RESULT, itemResource));
                                        // Map TDL report item to validation result:
                                        statements.add(validationReport.createStatement(itemResource, RDF.type, validationReport.createResource(SHACLResources.SHACL_VALIDATION_RESULT)));
                                        // Description -> result message
                                        if (StringUtils.isNotBlank(barItem.getDescription())) {
                                            statements.add(validationReport.createLiteralStatement(itemResource, SHACLResources.RESULT_MESSAGE, ((BAR)item.getValue()).getDescription()));
                                        }
                                        // Location -> focus node (e.g. "http://my.sample.po/po#item3")
                                        statements.add(validationReport.createStatement(itemResource, SHACLResources.FOCUS_NODE, validationReport.createResource(((BAR) item.getValue()).getLocation())));
                                        // Item name -> severity
                                        statements.add(validationReport.createStatement(itemResource, SHACLResources.RESULT_SEVERITY, validationReport.createResource(getShaclSeverity(item.getName().getLocalPart()))));
                                        // Assertion ID -> source constraint component (e.g. "http://www.w3.org/ns/shacl#MinExclusiveConstraintComponent")
                                        statements.add(validationReport.createStatement(itemResource, SHACLResources.SOURCE_CONSTRAINT_COMPONENT, validationReport.createResource(((BAR) item.getValue()).getAssertionID())));
                                        // Value -> value
                                        if (StringUtils.isNotBlank(barItem.getValue())) {
                                            statements.add(validationReport.createLiteralStatement(itemResource, SHACLResources.VALUE, ((BAR) item.getValue()).getValue()));
                                        }
                                        // Test -> result path (e.g. "http://itb.ec.europa.eu/sample/po#quantity")
                                        if (StringUtils.isNotBlank(barItem.getTest())) {
                                            statements.add(validationReport.createStatement(itemResource, SHACLResources.RESULT_PATH, validationReport.createResource(((BAR) item.getValue()).getTest())));
                                        }
                                    }
                                } else {
                                    LOG.warn("Plugin [{}] report item that is not instance of BAR. Skipping.", pluginName);
                                }
                            }
                            validationReport.add(statements);
                        }
                    }
                }
            } finally {
                // Cleanup plugin tmp folder.
                FileUtils.deleteQuietly(pluginTmpFolder);
            }
        }
        return validationReport;
    }

    /**
     * Get the SHACL message namespace to use for the provided GITB TDL report item level.
     *
     * @param tdlItemType The report item level from, the TAR report.
     * @return The SHACL namespace to use.
     */
    private String getShaclSeverity(String tdlItemType) {
        if ("error".equals(tdlItemType)) {
            return SHACLResources.SHACL_VIOLATION;
        } else if ("warning".equals(tdlItemType)) {
            return SHACLResources.SHACL_WARNING;
        } else {
            return SHACLResources.SHACL_INFO;
        }
    }

    /**
     * Validate the inout against all SHACL shapes.
     *
     * @return Model The Jena model with the SHACL validation report.
     */
    private Model validateAgainstShacl() {
        try {
            fileManager.signalValidationStart(domainConfig.getDomainName());
            List<FileInfo> shaclFiles = fileManager.getPreconfiguredValidationArtifacts(domainConfig, validationType);
            shaclFiles.addAll(externalShaclFiles);
            return validateShacl(shaclFiles);
        } finally {
            fileManager.signalValidationEnd(domainConfig.getDomainName());
        }
    }

    /**
     * Create an empty SHACL validation report.
     *
     * @return The report's model.
     */
    private Model emptyValidationReport() {
        Model reportModel = ModelFactory.createDefaultModel();
        List<Statement> statements = new ArrayList<>();
        Resource reportResource = reportModel.createResource();
        statements.add(reportModel.createStatement(reportResource, RDF.type, VALIDATION_REPORT));
        statements.add(reportModel.createLiteralStatement(reportResource, SHACLResources.CONFORMS, true));
        reportModel.add(statements);
        return reportModel;
    }

    /**
     * Validate the RDF against a set of shape files.
     *
     * @param shaclFiles The SHACL files.
     * @return The Jena Model with the SHACL validation report.
     */
    private Model validateShacl(List<FileInfo> shaclFiles) {
        Model reportModel;
        if (shaclFiles.isEmpty()) {
            reportModel = emptyValidationReport();
            this.dataModel = ModelFactory.createDefaultModel();
            this.aggregatedShapes = ModelFactory.createDefaultModel();
        } else {
            // Get data to validate from file
            this.aggregatedShapes = getShapesModel(shaclFiles);
            this.dataModel = getDataModel(getInputFileToUse(), this.aggregatedShapes);
            // Perform the validation of data, using the shapes model. Do not validate any shapes inside the data model.
            Resource resource = ValidationUtil.validateModel(dataModel, this.aggregatedShapes, false);
            reportModel = resource.getModel();
        }
        reportModel.setNsPrefix("sh", SHACLResources.NS_SHACL);
        return reportModel;
    }

    /**
     * Preprocess the RDF language to use (ensuring JSONLD1.1 support).
     *
     * @param inputLanguage The current language.
     * @return The language to use.
     */
    private Lang processRdfLanguage(Lang inputLanguage) {
        if (Lang.JSONLD.equals(inputLanguage)) {
            return Lang.JSONLD11;
        } else {
            return inputLanguage;
        }
    }

    /**
     * Return the aggregated model of a list of SHACL shape files.
     *
     * @return The aggregated model.
     */
    private Model getShapesModel(List<FileInfo> shaclFiles) {
        Model aggregateModel = JenaUtil.createMemoryModel();
        for (FileInfo shaclFile: shaclFiles) {
            LOG.info("Validating against [{}]", shaclFile.getFile().getName());
            Lang rdfLanguage = processRdfLanguage(RDFLanguages.contentTypeToLang(handleEquivalentContentSyntaxes(shaclFile.getType())));
            if (rdfLanguage == null) {
                throw new ValidatorException("validator.label.exception.unableToDetermineShaclContentType");
            }
            try (InputStream dataStream = new FileInputStream(shaclFile.getFile())) {
                aggregateModel.add(readModel(dataStream, rdfLanguage, null));
            } catch (IOException e) {
                throw new ValidatorException("validator.label.exception.errorReadingShaclFile", e);
           }
        }
        if (this.importedShapes!=null) {
        	this.importedShapes.close();
        	this.importedShapes.removeAll();        	
        }
        
        this.importedShapes = JenaUtil.createMemoryModel();
        createImportedModels(aggregateModel);
        if (this.importedShapes != null) {
        	aggregateModel.add(importedShapes);
        	this.importedShapes.removeAll();
        }
        return aggregateModel;
    }

    /**
     * Add imported models to the aggregated shape graph.
     *
     * @param aggregateModel The aggregated model to extend.
     */
    private void createImportedModels(Model aggregateModel) {
    	Set<String> reachedURIs = new HashSet<>();
    	
        ModelMaker modelMaker = ModelFactory.createMemModelMaker();
        OntModelSpec spec = new OntModelSpec( OntModelSpec.OWL_MEM_RULE_INF );
        spec.setBaseModelMaker(modelMaker);
        spec.setImportModelMaker(modelMaker);

        CustomLocatorHTTP.PARAMS.set(new CustomLocatorHTTP.LocatorParams(domainConfig.getUrisToSkipWhenImporting(), domainConfig.getHttpVersion()));
        CustomReadFailureHandler.IMPORTS_WITH_ERRORS.set(new LinkedHashSet<>());
        Set<Pair<String, String>> importsWithErrors;
        try {
            OntModel baseOntModel = ModelFactory.createOntologyModel(spec, aggregateModel);
            addIncluded(baseOntModel, reachedURIs);
            importsWithErrors = CustomReadFailureHandler.IMPORTS_WITH_ERRORS.get();
        } finally {
            CustomReadFailureHandler.IMPORTS_WITH_ERRORS.remove();
            CustomLocatorHTTP.PARAMS.remove();
        }

        if (!importsWithErrors.isEmpty()) {
            // Make sure the relevant models are closed, otherwise they are cached and don't result in additional failures, nor retries.
            var informationMessages = new ArrayList<String>();
            for (var errorInfo: importsWithErrors) {
                if (!domainConfig.getUrisToSkipWhenImporting().contains(errorInfo.getKey())) {
                    LOG.warn("Failed to load import [{}]: {}", errorInfo.getKey(), errorInfo.getValue());
                    informationMessages.add(String.format("URI [%s] produced error [%s]", errorInfo.getKey(), errorInfo.getValue()));
                    if (!domainConfig.getUrisToIgnoreForImportErrors().contains(errorInfo.getKey())) {
                        errorsWhileLoadingOwlImportsToReport = true;
                    }
                }
                // Close the relevant model so that we don't cache an error response.
                var model = OntDocumentManager.getInstance().getModel(errorInfo.getKey());
                if (model != null && !model.isClosed()) {
                    try {
                        model.close();
                    } catch (Exception e) {
                        // Ignore.
                    }
                }
            }
            errorsWhileLoadingOwlImports.addAll(informationMessages);
            if (errorsWhileLoadingOwlImportsToReport && domainConfig.getResponseForImportedShapeFailure(validationType) == ErrorResponseTypeEnum.FAIL) {
                throw new ExtendedValidatorException("validator.label.exception.failureToLoadRemoteArtefactsError", informationMessages);
            }
        }
    }

    /**
     * @return Whether errors were recorded while loading owl:imports that must be reported.
     */
    public boolean hasErrorsWhileLoadingOwlImportsToReport() {
        return errorsWhileLoadingOwlImportsToReport;
    }

    /**
     * @return Whether errors were recorded while loading owl:imports.
     */
    public boolean hasErrorsDuringOwlImports() {
        return !errorsWhileLoadingOwlImports.isEmpty();
    }

    /**
     * @return The list of produced errors while loading owl:imports.
     */
    public List<String> getErrorsWhileLoadingOwlImports() {
        return errorsWhileLoadingOwlImports;
    }

    /**
     * Extend the shape graph with imported shaped.
     *
     * @param baseOntModel The base model to add to.
     * @param reachedURIs The URIs that have already been processed.
     * @return The processed URIs.
     */
    private Set<String> addIncluded(OntModel baseOntModel, Set<String> reachedURIs) {
    	baseOntModel.loadImports();
        Set<String> listImportedURI = baseOntModel.listImportedOntologyURIs();
                
        for (String importedURI : listImportedURI) {
        	if (!reachedURIs.contains(importedURI)) {
        		OntModel importedModel = baseOntModel.getImportedModel(importedURI);
        		
        		if (importedModel != null) {
        			this.importedShapes.add(importedModel.getBaseModel());
        			reachedURIs.add(importedURI);
        			
        			reachedURIs = addIncluded(importedModel, reachedURIs);
        		}
        	}
        }
        
        return reachedURIs;
    }

    /**
     * Get the Jena RDF language to use for the input content.
     *
     * @return The language.
     */
    private Lang contextSyntaxToUse() {
        if (contentSyntaxLang == null) {
            // Determine language.
            Lang lang = null;
            if (this.contentSyntax != null) {
                String contentSyntaxToConsider = handleEquivalentContentSyntaxes(this.contentSyntax);
                lang = RDFLanguages.contentTypeToLang(contentSyntaxToConsider);
                if (lang != null) {
                    LOG.info("Using provided data content type [{}] as [{}]", contentSyntaxToConsider, lang.getName());
                }
            }
            if (lang == null) {
                lang = RDFLanguages.contentTypeToLang(RDFLanguages.guessContentType(inputFileToValidate.getName()));
                if (lang != null) {
                    LOG.info("Guessed lang [{}] from file [{}]", lang.getName(), inputFileToValidate.getName());
                }
            }
            if (lang == null) {
                throw new ValidatorException("validator.label.exception.rdfLanguageCouldNotBeDetermined");
            }
            contentSyntaxLang = processRdfLanguage(lang);
        }
        return contentSyntaxLang;
    }

    /**
     * Build the RDF model from the provided stream.
     *
     * @param dataStream The stream to read from.
     * @param rdfLanguage The content type of the stream's data.
     * @return The parsed model.
     */
    private Model readModel(InputStream dataStream, Lang rdfLanguage, Map<String, String> nsPrefixes) {
        var builder = RDFParserBuilder
                .create()
                .lang(rdfLanguage)
                .source(dataStream);
        if (nsPrefixes != null) {
            // Before parsing set the prefixes of the model to avoid mismatches.
            PrefixMap prefixes = new PrefixMapStd();
            prefixes.putAll(nsPrefixes);
            builder = builder.prefixes(prefixes);
        }
        if (Lang.JSONLD11.equals(rdfLanguage) || Lang.JSONLD.equals(rdfLanguage)) {
            var options = new JsonLdOptions();
            var httpLoader = new HttpLoader(DefaultHttpClient.defaultInstance());
            /*
             * Set fallback type for remote contexts to avoid errors for non JSON/JSON-LD Content Types.
             * This allows us to proceed if e.g. the Content Type originally returned is "text/plain".
             */
            httpLoader.setFallbackContentType(MediaType.JSON);
            options.setDocumentLoader(new SchemeRouter()
                    .set("http", httpLoader)
                    .set("https", httpLoader)
                    .set("file", new FileLoader()));
            builder = builder.context(Context.create().set(JSONLD_OPTIONS, options));
        }
        return builder.build().toModel();
    }

    /**
     * Prepare the data graph model for the provided inputs.
     *
     * @param dataFile File with RDF data.
     * @param shapesModel The Jena model containing the shacl definitions (needed to set the proper prefixes on the input data).
     * @return Jena Model containing the data from dataFile.
     */
    private Model getDataModel(File dataFile, Model shapesModel) {
        // Upload the data in the Model.
        Model dataModel;
        try (InputStream dataStream = new FileInputStream(dataFile)) {
            dataModel = readModel(dataStream, contextSyntaxToUse(), shapesModel == null ? null : shapesModel.getNsPrefixMap());
            if (this.loadImports) {
                LOG.info("Loading imports...");
                createImportedModels(dataModel);
                if (this.importedShapes != null) {
                    dataModel.add(importedShapes);
                    this.importedShapes.removeAll();
                }
            }
        } catch (ValidatorException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidatorException("validator.label.exception.errorWhileReadingProvidedContent", e, e.getMessage());
        }
        /*
         * Add the aggregated shapes' model to the input data model. This is done so that any vocabulary definitions
         * provided through the SHACL shape inputs (directly or as owl:imports) are also considered in the data graph.
         * This is primarily done to ensure subclass checks are correctly done (see SHACL specification section 3.2, "Data Graph".).
         * 
         * We can disable this merging for a domain if merging is undesirable. The only such case would be if we are
         * validating SHACL shapes themselves.
         */
        if (domainConfig.isMergeModelsBeforeValidation()) {
            dataModel.add(shapesModel);
        }
		return dataModel;
	}

    /**
     * Preprocesses the file.
     *
     * @return inputFileToUse.
     */
    private File getInputFileToUse() {
        if (!inputReady) {
            // obtain the SPARQL CONSTRUCT query for the validationType
            var constructQuery = domainConfig.getInputPreprocessorPerType().get(validationType);
            if (constructQuery != null) {
                // preprocessing: execute the CONSTRUCT query
                Model inputModel;
                try (InputStream dataStream = new FileInputStream(inputFileToValidate)) {
                    inputModel = readModel(dataStream, contextSyntaxToUse(), null);
                } catch (IOException e) {
                    throw new ValidatorException("validator.label.exception.errorWhileReadingProvidedContent", e, e.getMessage());
                } catch (JenaException e) {
                    throw new ValidatorException("validator.label.exception.preprocessingError", e, constructQuery);
                }
                Model preprocessedModel = JenaUtil.createMemoryModel();
                try (QueryExecution queryExecution = QueryExecutionFactory.create(constructQuery, inputModel)) {
                    queryExecution.execConstruct(preprocessedModel);
                }
                // check that the processed model is not empty
                if (preprocessedModel.isEmpty()) {
                    throw new ValidatorException("validator.label.exception.emptyPreprocessingResult", constructQuery);
                }
                try (FileWriter writer = new FileWriter(inputFileToValidate)) {
                    preprocessedModel.write(writer, contentSyntaxLang.getName());
                } catch (IOException e) {
                    throw new ValidatorException("validator.label.exception.preprocessingError", e, constructQuery);
                }
            }
            inputReady = true;
        }
        return inputFileToValidate;
    }

    /**
     * @return The aggregated SHACL shape model used for the validation.
     */
    public Model getAggregatedShapes() {
    	return this.aggregatedShapes;
    }

}
