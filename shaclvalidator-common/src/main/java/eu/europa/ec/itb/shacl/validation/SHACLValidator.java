package eu.europa.ec.itb.shacl.validation;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.BAR;
import com.gitb.tr.TestAssertionReportType;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.config.DomainPluginConfigProvider;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.plugin.PluginManager;
import eu.europa.ec.itb.validation.plugin.ValidationPlugin;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.topbraid.jenax.util.JenaUtil;
import org.topbraid.shacl.validation.ValidationUtil;

import javax.xml.bind.JAXBElement;
import java.io.*;
import java.util.*;

import static eu.europa.ec.itb.shacl.validation.SHACLResources.VALIDATION_REPORT;

/**
 * 
 * Created by mfontsan on 26/02/2019
 *
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
    private DomainPluginConfigProvider pluginConfigProvider = null;

    private File inputFileToValidate;
    private final DomainConfig domainConfig;
    private String validationType;
    private String contentSyntax;
    private Lang contentSyntaxLang;
    private List<FileInfo> externalShaclFiles;
    private Model aggregatedShapes;
    private Model importedShapes;
    private boolean loadImports;

    /**
     * Constructor to start the SHACL validator.
     * @param inputFileToValidate The input RDF (or other) content to validate.
     * @param validationType The type of validation to perform.
     * @param contentSyntax The mime type of the provided RDF content.
     * @param externalShaclFiles Any shapes to consider that are externally provided
     * @param domainConfig Domain
     */
    public SHACLValidator(File inputFileToValidate, String validationType, String contentSyntax, List<FileInfo> externalShaclFiles, boolean loadImports, DomainConfig domainConfig) {
    	this.contentSyntax = contentSyntax;
    	this.inputFileToValidate = inputFileToValidate;
        this.validationType = validationType;
        this.domainConfig = domainConfig;
        this.externalShaclFiles = externalShaclFiles;
        this.loadImports = loadImports;
        if (validationType == null) {
            this.validationType = domainConfig.getType().get(0);
        }
    }
    
    /**
     * Manager of the validation.
     * @return The Jena model with the report.
     */
    public Model validateAll() {
    	LOG.info("Starting validation..");
    	try {
            Model validationReport = validateAgainstShacl();
            return validateAgainstPlugins(validationReport);
        } finally {
    	    LOG.info("Completed validation.");
        }
    }

    private AnyContent createPluginInputItem(String name, String value) {
        AnyContent input = new AnyContent();
        input.setName(name);
        input.setValue(value);
        input.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        return input;
    }

    private ValidateRequest preparePluginInput(File pluginTmpFolder) {
        // The content to validate is provided to plugins as a copu of the content in RDF/XML (for simpler processing).
        File pluginInputFile = new File(pluginTmpFolder, UUID.randomUUID().toString()+".rdf");
        Lang contentSyntax = contextSyntaxToUse();
        if (Lang.RDFXML.equals(contentSyntax)) {
            // Use file as-is.
            try {
                FileUtils.copyFile(inputFileToValidate, pluginInputFile);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to copy input file for plugin", e);
            }
        } else {
            // Make a converted copy.
            Model fileModel = JenaUtil.createMemoryModel();
            try (FileInputStream in = new FileInputStream(inputFileToValidate); FileWriter out = new FileWriter(pluginInputFile)) {
                fileModel.read(in, null, contentSyntax.getContentType().getContentType());
                fileManager.writeRdfModel(out, fileModel, Lang.RDFXML.getContentType().getContentType());
            } catch (IOException e) {
                throw new IllegalStateException("Unable to convert input file for plugin", e);
            }
        }
        ValidateRequest request = new ValidateRequest();
        request.getInput().add(createPluginInputItem("contentToValidate", pluginInputFile.getAbsolutePath()));
        request.getInput().add(createPluginInputItem("domain", domainConfig.getDomainName()));
        request.getInput().add(createPluginInputItem("validationType", validationType));
        request.getInput().add(createPluginInputItem("tempFolder", pluginTmpFolder.getAbsolutePath()));
        return request;
    }

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
                                if (item.getValue() instanceof BAR) {
                                    if (StringUtils.isBlank(((BAR) item.getValue()).getLocation())) {
                                        LOG.warn("Plugin [{}] report item without location. Skipping.", pluginName);
                                    } else if (StringUtils.isBlank(((BAR) item.getValue()).getAssertionID())) {
                                        LOG.warn("Plugin [{}] report item without assertion ID. Skipping.", pluginName);
                                    } else {
                                        Resource itemResource = validationReport.createResource();
                                        statements.add(validationReport.createStatement(reportResource, SHACLResources.RESULT, itemResource));
                                        // Map TDL report item to validation result:
                                        statements.add(validationReport.createStatement(itemResource, RDF.type, validationReport.createResource(SHACLResources.SHACL_VALIDATION_RESULT)));
                                        // Description -> result message
                                        if (StringUtils.isNotBlank(((BAR)item.getValue()).getDescription())) {
                                            statements.add(validationReport.createLiteralStatement(itemResource, SHACLResources.RESULT_MESSAGE, ((BAR)item.getValue()).getDescription()));
                                        }
                                        // Location -> focus node (e.g. "http://my.sample.po/po#item3")
                                        statements.add(validationReport.createStatement(itemResource, SHACLResources.FOCUS_NODE, validationReport.createResource(((BAR) item.getValue()).getLocation())));
                                        // Item name -> severity
                                        statements.add(validationReport.createStatement(itemResource, SHACLResources.RESULT_SEVERITY, validationReport.createResource(getShaclSeverity(item.getName().getLocalPart()))));
                                        // Assertion ID -> source constraint component (e.g. "http://www.w3.org/ns/shacl#MinExclusiveConstraintComponent")
                                        statements.add(validationReport.createStatement(itemResource, SHACLResources.SOURCE_CONSTRAINT_COMPONENT, validationReport.createResource(((BAR) item.getValue()).getAssertionID())));
                                        // Value -> value
                                        if (StringUtils.isNotBlank(((BAR)item.getValue()).getValue())) {
                                            statements.add(validationReport.createLiteralStatement(itemResource, SHACLResources.VALUE, ((BAR) item.getValue()).getValue()));
                                        }
                                        // Test -> result path (e.g. "http://itb.ec.europa.eu/sample/po#quantity")
                                        if (StringUtils.isNotBlank(((BAR)item.getValue()).getTest())) {
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
     * Validation of the model
     * @return Model The Jena model with the report
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
     * Validate the RDF against one shape file
     * @param shaclFiles The SHACL files
     * @return Model The Jena Model with the report
     */
    private Model validateShacl(List<FileInfo> shaclFiles) {
        Model reportModel;
        if (shaclFiles.isEmpty()) {
            reportModel = emptyValidationReport();
            this.aggregatedShapes = ModelFactory.createDefaultModel();
        } else {
            // Get data to validate from file
            this.aggregatedShapes = getShapesModel(shaclFiles);
            Model dataModel = getDataModel(inputFileToValidate, this.aggregatedShapes);
            // Perform the validation of data, using the shapes model. Do not validate any shapes inside the data model.
            Resource resource = ValidationUtil.validateModel(dataModel, this.aggregatedShapes, false);
            reportModel = resource.getModel();
        }
        reportModel.setNsPrefix("sh", SHACLResources.NS_SHACL);
        return reportModel;
    }

    /**
     * Return the aggregated model of a list of files
     * @return Model aggregated model
     */
    private Model getShapesModel(List<FileInfo> shaclFiles) {
        Model aggregateModel = JenaUtil.createMemoryModel();
        for (FileInfo shaclFile: shaclFiles) {
            LOG.info("Validating against ["+shaclFile.getFile().getName()+"]");
            Lang rdfLanguage = RDFLanguages.contentTypeToLang(shaclFile.getType());
            if (rdfLanguage == null) {
                throw new ValidatorException("Unable to determine the content type of a SHACL shape file.");
            }
            try (InputStream dataStream = new FileInputStream(shaclFile.getFile())) {
                Model fileModel = JenaUtil.createMemoryModel();
                fileModel.read(dataStream, null, rdfLanguage.getName());
                aggregateModel.add(fileModel);
            } catch (IOException e) {
                throw new ValidatorException("An error occurred while reading a SHACL file.", e);
           }
        }
        if(this.importedShapes!=null) {
        	this.importedShapes.close();
        	this.importedShapes.removeAll();        	
        }
        
        this.importedShapes = JenaUtil.createMemoryModel();
        createImportedModels(aggregateModel);
        if(this.importedShapes != null) {
        	aggregateModel.add(importedShapes);
        	this.importedShapes.removeAll();
        }
        
        return aggregateModel;
    }    
    
    private void createImportedModels(Model aggregateModel) {
    	Set<String> reachedURIs = new HashSet<>();
    	
        ModelMaker modelMaker = ModelFactory.createMemModelMaker();   
        OntModelSpec spec = new OntModelSpec( OntModelSpec.OWL_MEM_RULE_INF );
        spec.setBaseModelMaker(modelMaker);
        spec.setImportModelMaker(modelMaker);
        
        OntModel baseOntModel = ModelFactory.createOntologyModel( spec, aggregateModel );
        
        addIncluded(baseOntModel, reachedURIs);
    }
    
    private Set<String> addIncluded(OntModel baseOntModel, Set<String> reachedURIs) {
    	baseOntModel.loadImports();
        Set<String> listImportedURI = baseOntModel.listImportedOntologyURIs();
                
        for(String importedURI : listImportedURI) {
        	if(!reachedURIs.contains(importedURI)) {
        		OntModel importedModel = baseOntModel.getImportedModel(importedURI);
        		
        		if(importedModel != null) {
        			this.importedShapes.add(importedModel.getBaseModel());
        			reachedURIs.add(importedURI);
        			
        			reachedURIs = addIncluded(importedModel, reachedURIs);
        		}
        	}
        }
        
        return reachedURIs;
    }

    private Lang contextSyntaxToUse() {
        if (contentSyntaxLang == null) {
            // Determine language.
            Lang lang = null;
            if (this.contentSyntax != null) {
                lang = RDFLanguages.contentTypeToLang(this.contentSyntax);
                if (lang != null) {
                    LOG.info("Using provided data content type ["+this.contentSyntax+"] as ["+lang.getName()+"]");
                }
            }
            if (lang == null) {
                lang = RDFLanguages.contentTypeToLang(RDFLanguages.guessContentType(inputFileToValidate.getName()));
                if (lang != null) {
                    LOG.info("Guessed lang ["+lang.getName()+"] from file ["+inputFileToValidate.getName()+"]");
                }
            }
            if (lang == null) {
                throw new ValidatorException("The RDF language could not be determined for the provided content.");
            }
            contentSyntaxLang = lang;
        }
        return contentSyntaxLang;
    }

    /**
     * 
     * @param dataFile File with RDF data
     * @param shapesModel The Jena model containing the shacl defintion (needed to set the proper prefixes on the input data)
     * @return Model Jena Model containing the data from dataFile
     */
    private Model getDataModel(File dataFile, Model shapesModel) {
        // Upload the data in the Model. First set the prefixes of the model to those of the shapes model to avoid mismatches.
        Model dataModel = JenaUtil.createMemoryModel();
        
        if (shapesModel != null) {
            dataModel.setNsPrefixes(shapesModel.getNsPrefixMap());
        }
        try (InputStream dataStream = new FileInputStream(dataFile)) {
            dataModel.read(dataStream, null, contextSyntaxToUse().getName());
            
            if(this.loadImports) {
                LOG.info("Loading imports...");
	            createImportedModels(dataModel);
	            
	            if(this.importedShapes != null) {
	            	dataModel.add(importedShapes);
	            	this.importedShapes.removeAll();
	            }
            }
        } catch (Exception e) {
            throw new ValidatorException("An error occurred while reading the provided content: "+e.getMessage(), e);
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
    
    public Model getAggregatedShapes() {
    	return this.aggregatedShapes;
    }

}
