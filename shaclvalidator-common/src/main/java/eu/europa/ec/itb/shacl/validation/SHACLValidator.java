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

import com.gitb.tr.BAR;
import com.gitb.tr.TestAssertionReportType;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import eu.europa.ec.itb.shacl.*;
import eu.europa.ec.itb.shacl.config.CustomLocatorHTTP;
import eu.europa.ec.itb.shacl.util.ShaclValidatorUtils;
import eu.europa.ec.itb.shacl.util.StatementTranslator;
import eu.europa.ec.itb.validation.commons.FileInfo;
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
import org.apache.jena.shared.JenaException;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.topbraid.jenax.util.JenaUtil;
import org.topbraid.shacl.validation.ValidationUtil;
import org.topbraid.shacl.vocabulary.SH;

import java.io.*;
import java.util.*;

import static eu.europa.ec.itb.shacl.util.ShaclValidatorUtils.*;

/**
 * Component used to validate RDF content against SHACL shapes.
 */
@Component
@Scope("prototype")
public class SHACLValidator {

    private static final Logger LOG = LoggerFactory.getLogger(SHACLValidator.class);

    @Autowired
    private FileManager fileManager = null;
    @Autowired
    private PluginManager pluginManager = null;
    @Autowired
    private DomainPluginConfigProvider<DomainConfig> pluginConfigProvider = null;

    private final ValidationSpecs specs;

    private boolean inputReady = false;
    private Lang contentSyntaxLang;
    private Model aggregatedShapes;
    private Model importedShapes;
    private Model dataModel;
    private final List<String> errorsWhileLoadingOwlImports = new ArrayList<>();
    private boolean errorsWhileLoadingOwlImportsToReport = false;

    /**
     * Constructor to start the SHACL validator.
     *
     * @param specs The validation specifications.
     */
    public SHACLValidator(ValidationSpecs specs) {
        this.specs = specs;
    }

    /**
     * @return The domain's identifier.
     */
    public String getDomain(){
        return specs.getDomainConfig().getDomain();
    }

    /**
     * @return The requested validation type.
     */
    public String getValidationType(){
        return this.specs.getValidationType();
    }
    
    /**
     * Validate the content against all SHACL shapes and plugins.
     *
     * @return The Jena model with the report.
     */
    public ModelPair validateAll() {
        if (specs.isLogProgress()) {
            LOG.info("Starting validation..");
        }
        try {
            Model validationReport = validateAgainstShacl();
            if (specs.isUsePlugins()) {
                validateAgainstPlugins(validationReport);
            }
            setOverallResult(validationReport);
            processForLocale(validationReport);
            return new ModelPair(dataModel, validationReport);
        } finally {
            if (specs.isLogProgress()) {
                LOG.info("Completed validation.");
            }
        }
    }

    /**
     * If configured to do so, remove all translations from the report that are not relevant to the requested locale.
     *
     * @param report The report to process.
     */
    private void processForLocale(Model report) {
        if (!specs.getDomainConfig().isReturnMessagesForAllLocales()) {
            // Filter out result messages for locales other than the requested one.
            var resultProperty = report.getProperty(SH.result.getURI());
            var resultMessageProperty = report.getProperty(SH.resultMessage.getURI());
            var resultIterator = report.listObjectsOfProperty(resultProperty);
            while (resultIterator.hasNext()) {
                var node = resultIterator.next();
                var statementIterator = report.listStatements(node.asResource(), resultMessageProperty, (RDFNode) null);
                var translator = new StatementTranslator();
                while (statementIterator.hasNext()) {
                    translator.processStatement(statementIterator.next());
                }
                report.remove(translator.getTranslation(specs.getLocaliser().getLocale()).getUnmatchedStatements());
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
                Model fileModel = fileManager.readModel(in, contentSyntaxToUse, null);
                fileManager.writeRdfModel(out, fileModel, Lang.RDFXML.getContentType().getContentTypeStr());
            } catch (IOException e) {
                throw new IllegalStateException("Unable to convert input file for plugin", e);
            }
        }
        ValidateRequest request = new ValidateRequest();
        request.getInput().add(Utils.createInputItem("contentToValidate", pluginInputFile.getAbsolutePath()));
        request.getInput().add(Utils.createInputItem("domain", specs.getDomainConfig().getDomainName()));
        request.getInput().add(Utils.createInputItem("validationType", getValidationType()));
        request.getInput().add(Utils.createInputItem("tempFolder", pluginTmpFolder.getAbsolutePath()));
        request.getInput().add(Utils.createInputItem("locale", specs.getLocaliser().getLocale().toString()));
        return request;
    }

    /**
     * Ensure that violations in the report always result in an overall report failure.
     *
     * @param validationReport The report.
     */
    private void setOverallResult(Model validationReport) {
        Resource reportResource = validationReport.listSubjectsWithProperty(RDF.type, SH.ValidationReport).nextResource();
        if (reportResource != null) {
            Statement conformsStatement = reportResource.getProperty(SH.conforms);
            if (conformsStatement.getBoolean() && hasViolations(validationReport)) {
                conformsStatement.changeLiteralObject(false);
            }
        }
    }

    /**
     * Check to see whether the provided report includes at least one violation.
     *
     * @param validationReport The report to check.
     * @return The check result.
     */
    private boolean hasViolations(Model validationReport) {
        NodeIterator resultIterator = validationReport.listObjectsOfProperty(validationReport.getProperty(SH.result.getURI()));
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
     */
    private void validateAgainstPlugins(Model validationReport) {
        ValidationPlugin[] plugins =  pluginManager.getPlugins(pluginConfigProvider.getPluginClassifier(specs.getDomainConfig(), getValidationType()));
        if (plugins != null && plugins.length > 0) {
            File pluginTmpFolder = new File(specs.getInputFileToValidate().getParentFile(), UUID.randomUUID().toString());
            try {
                pluginTmpFolder.mkdirs();
                ValidateRequest pluginInput = preparePluginInput(pluginTmpFolder);
                for (ValidationPlugin plugin: plugins) {
                    String pluginName = plugin.getName();
                    ValidationResponse response = plugin.validate(pluginInput);
                    if (response != null && response.getReport() != null && response.getReport().getReports() != null) {
                        if (specs.isLogProgress()) {
                            LOG.info("Plugin [{}] produced [{}] report item(s).", pluginName, response.getReport().getReports().getInfoOrWarningOrError().size());
                        }
                        Resource reportResource = validationReport.listSubjectsWithProperty(RDF.type, SH.ValidationReport).nextResource();
                        if (!response.getReport().getReports().getInfoOrWarningOrError().isEmpty()) {
                            // Ensure the overall result is set to a failure if needed.
                            Statement conformsStatement = reportResource.getProperty(SH.conforms);
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
                                        statements.add(validationReport.createStatement(reportResource, SH.result, itemResource));
                                        // Map TDL report item to validation result:
                                        statements.add(validationReport.createStatement(itemResource, RDF.type, validationReport.createResource(SH.ValidationResult)));
                                        // Description -> result message
                                        if (StringUtils.isNotBlank(barItem.getDescription())) {
                                            statements.add(validationReport.createLiteralStatement(itemResource, SH.resultMessage, ((BAR)item.getValue()).getDescription()));
                                        }
                                        // Location -> focus node (e.g. "http://my.sample.po/po#item3")
                                        statements.add(validationReport.createStatement(itemResource, SH.focusNode, validationReport.createResource(((BAR) item.getValue()).getLocation())));
                                        // Item name -> severity
                                        statements.add(validationReport.createStatement(itemResource, SH.resultSeverity, validationReport.createResource(getShaclSeverity(item.getName().getLocalPart()))));
                                        // Assertion ID -> source constraint component (e.g. "http://www.w3.org/ns/shacl#MinExclusiveConstraintComponent")
                                        statements.add(validationReport.createStatement(itemResource, SH.sourceConstraintComponent, validationReport.createResource(((BAR) item.getValue()).getAssertionID())));
                                        // Value -> value
                                        if (StringUtils.isNotBlank(barItem.getValue())) {
                                            statements.add(validationReport.createLiteralStatement(itemResource, SH.value, ((BAR) item.getValue()).getValue()));
                                        }
                                        // Test -> result path (e.g. "http://itb.ec.europa.eu/sample/po#quantity")
                                        if (StringUtils.isNotBlank(barItem.getTest())) {
                                            statements.add(validationReport.createStatement(itemResource, SH.resultPath, validationReport.createResource(((BAR) item.getValue()).getTest())));
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
    }

    /**
     * Get the SHACL message namespace to use for the provided GITB TDL report item level.
     *
     * @param tdlItemType The report item level from, the TAR report.
     * @return The SHACL namespace to use.
     */
    private String getShaclSeverity(String tdlItemType) {
        if ("error".equals(tdlItemType)) {
            return SH.Violation.getURI();
        } else if ("warning".equals(tdlItemType)) {
            return SH.Warning.getURI();
        } else {
            return SH.Info.getURI();
        }
    }

    /**
     * Validate the inout against all SHACL shapes.
     *
     * @return Model The Jena model with the SHACL validation report.
     */
    private Model validateAgainstShacl() {
        try {
            fileManager.signalValidationStart(specs.getDomainConfig().getDomainName());
            List<FileInfo> shaclFiles = fileManager.getPreconfiguredValidationArtifacts(specs.getDomainConfig(), getValidationType());
            shaclFiles.addAll(specs.getExternalShaclFiles());
            return validateShacl(shaclFiles);
        } finally {
            fileManager.signalValidationEnd(specs.getDomainConfig().getDomainName());
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
        statements.add(reportModel.createStatement(reportResource, RDF.type, SH.ValidationReport));
        statements.add(reportModel.createLiteralStatement(reportResource, SH.conforms, true));
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
            // Construct the shapes graph.
            this.aggregatedShapes = fileManager.getShapeModel(specs, () -> getShapesModel(shaclFiles));
            // Get data to validate from file
            this.dataModel = getDataModel(getInputFileToUse(), this.aggregatedShapes);
            // Perform the validation of data, using the shapes model. Do not validate any shapes inside the data model.
            Resource resource = ValidationUtil.validateModel(dataModel, this.aggregatedShapes, false);
            reportModel = resource.getModel();
        }
        specs.track(this.dataModel);
        specs.track(this.aggregatedShapes);
        specs.track(reportModel);
        reportModel.setNsPrefix("sh", SH.getURI());
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
            if (specs.isLogProgress()) {
                LOG.info("Validating against [{}]", shaclFile.getFile().getName());
            }
            Lang rdfLanguage = processRdfLanguage(determineRdfLanguage(shaclFile));
            if (rdfLanguage == null) {
                throw new ValidatorException("validator.label.exception.unableToDetermineShaclContentType");
            }
            try (InputStream dataStream = new FileInputStream(shaclFile.getFile())) {
                aggregateModel.add(fileManager.readModel(dataStream, rdfLanguage, null));
            } catch (IOException e) {
                throw new ValidatorException("validator.label.exception.errorReadingShaclFile", e);
            }
        }
        if (this.importedShapes!=null) {
        	this.importedShapes.close();
        	this.importedShapes.removeAll();        	
        }
        
        this.importedShapes = JenaUtil.createMemoryModel();
        specs.track(this.importedShapes);
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

        CustomLocatorHTTP.PARAMS.set(new CustomLocatorHTTP.LocatorParams(specs.getDomainConfig().getUrisToSkipWhenImporting(), specs.getDomainConfig().getHttpVersion(), specs.getDomainConfig().getOwlImportMappings()));
        CustomJenaFileManager.PARAMS.set(new CustomJenaFileManager.CacheParams(specs.getDomainConfig().getOwlImportMappings().keySet(), new HashMap<>()));
        CustomReadFailureHandler.IMPORTS_WITH_ERRORS.set(new LinkedHashSet<>());
        Set<Pair<String, String>> importsWithErrors;
        try {
            OntModel baseOntModel = ModelFactory.createOntologyModel(spec, aggregateModel);
            addIncluded(baseOntModel, reachedURIs);
            importsWithErrors = CustomReadFailureHandler.IMPORTS_WITH_ERRORS.get();
        } finally {
            CustomReadFailureHandler.IMPORTS_WITH_ERRORS.remove();
            CustomLocatorHTTP.PARAMS.remove();
            CustomJenaFileManager.PARAMS.remove();
        }

        if (!importsWithErrors.isEmpty()) {
            // Make sure the relevant models are closed, otherwise they are cached and don't result in additional failures, nor retries.
            var informationMessages = new ArrayList<String>();
            for (var errorInfo: importsWithErrors) {
                if (!specs.getDomainConfig().getUrisToSkipWhenImporting().contains(errorInfo.getKey())) {
                    LOG.warn("Failed to load import [{}]: {}", errorInfo.getKey(), errorInfo.getValue());
                    informationMessages.add(String.format("URI [%s] produced error [%s]", errorInfo.getKey(), errorInfo.getValue()));
                    if (!specs.getDomainConfig().getUrisToIgnoreForImportErrors().contains(errorInfo.getKey())) {
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
            if (errorsWhileLoadingOwlImportsToReport && specs.getDomainConfig().getResponseForImportedShapeFailure(getValidationType()) == ErrorResponseTypeEnum.FAIL) {
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
            if (this.specs.getContentSyntax() != null) {
                String contentSyntaxToConsider = handleEquivalentContentSyntaxes(this.specs.getContentSyntax());
                lang = RDFLanguages.contentTypeToLang(contentSyntaxToConsider);
                if (specs.isLogProgress() && lang != null) {
                    LOG.info("Using provided data content type [{}] as [{}]", contentSyntaxToConsider, lang.getName());
                }
            }
            if (lang == null) {
                lang = RDFLanguages.contentTypeToLang(RDFLanguages.guessContentType(specs.getInputFileToValidate().getName()));
                if (specs.isLogProgress() && lang != null) {
                    LOG.info("Guessed lang [{}] from file [{}]", lang.getName(), specs.getInputFileToValidate().getName());
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
            dataModel = fileManager.readModel(dataStream, contextSyntaxToUse(), shapesModel == null ? null : shapesModel.getNsPrefixMap());
            if (specs.isLoadImports()) {
                if (specs.isLogProgress()) {
                    LOG.info("Loading imports...");
                }
                createImportedModels(dataModel);
                if (importedShapes != null) {
                    dataModel.add(importedShapes);
                    importedShapes.removeAll();
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
        if (specs.isMergeModelsBeforeValidation()) {
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
            var constructQuery = specs.getDomainConfig().getInputPreprocessorPerType().get(getValidationType());
            if (constructQuery != null) {
                // preprocessing: execute the CONSTRUCT query
                Model inputModel;
                try (InputStream dataStream = new FileInputStream(specs.getInputFileToValidate())) {
                    inputModel = fileManager.readModel(dataStream, contextSyntaxToUse(), null);
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
                try (FileWriter writer = new FileWriter(specs.getInputFileToValidate())) {
                    preprocessedModel.write(writer, contentSyntaxLang.getName());
                } catch (IOException e) {
                    throw new ValidatorException("validator.label.exception.preprocessingError", e, constructQuery);
                }
            }
            inputReady = true;
        }
        return specs.getInputFileToValidate();
    }

    /**
     * @return The aggregated SHACL shape model used for the validation.
     */
    public Model getAggregatedShapes() {
    	return this.aggregatedShapes;
    }

}
