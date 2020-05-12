package eu.europa.ec.itb.shacl.validation;

import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.errors.ValidatorException;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ModelMaker;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.topbraid.jenax.util.JenaUtil;
import org.topbraid.shacl.validation.ValidationUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * Created by mfontsan on 26/02/2019
 *
 */
@Component
@Scope("prototype")
public class SHACLValidator {

    private static final Logger logger = LoggerFactory.getLogger(SHACLValidator.class);

    @Autowired
    private FileManager fileManager;

    private File inputFileToValidate;
    private final DomainConfig domainConfig;
    private String validationType;
    private String contentSyntax;
    private List<FileInfo> filesInfo;
    private Model aggregatedShapes;
    private Model importedShapes;

    /**
     * Constructor to start the SHACL validator.
     * @param inputFileToValidate The input RDF (or other) content to validate.
     * @param validationType The type of validation to perform.
     * @param contentSyntax The mime type of the provided RDF content.
     * @param remoteShaclFiles Any shapes to consider that are externally provided 
     * @param domainConfig Domain
     */
    public SHACLValidator(File inputFileToValidate, String validationType, String contentSyntax, List<FileInfo> remoteShaclFiles, DomainConfig domainConfig) {
    	this.contentSyntax = contentSyntax;
    	this.inputFileToValidate = inputFileToValidate;
        this.validationType = validationType;
        this.domainConfig = domainConfig;
        this.filesInfo = remoteShaclFiles;
        if (validationType == null) {
            this.validationType = domainConfig.getType().get(0);
        }
    }
    
    /**
     * Manager of the validation.
     * @return The Jena model with the report.
     */
    public Model validateAll() {
    	logger.info("Starting validation..");
        return validateAgainstShacl();
    }
    
    /**
     * Validation of the model
     * @return Model The Jena model with the report
     */
    private Model validateAgainstShacl() {
        try {
            fileManager.signalValidationStart(domainConfig.getDomainName());
            List<FileInfo> shaclFiles = fileManager.getAllShaclFiles(domainConfig, validationType, filesInfo);
            if (shaclFiles.isEmpty()) {
                throw new ValidatorException("No SHACL files are defined for the validation.");
            } else {
                return validateShacl(shaclFiles);
            }
        } finally {
            fileManager.signalValidationEnd(domainConfig.getDomainName(), inputFileToValidate, filesInfo);
        }
    }

    /**
     * Validate the RDF against one shape file
     * @param shaclFiles The SHACL files
     * @return Model The Jena Model with the report
     */
    private Model validateShacl(List<FileInfo> shaclFiles){
        // Get data to validate from file
        this.aggregatedShapes = getShapesModel(shaclFiles);
        Model dataModel = getDataModel(inputFileToValidate, this.aggregatedShapes);

        // Perform the validation of data, using the shapes model. Do not validate any shapes inside the data model.
        Resource resource = ValidationUtil.validateModel(dataModel, this.aggregatedShapes, false);
        Model reportModel = resource.getModel();
        reportModel.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
    	return reportModel;
    }

    /**
     * Return the aggregated model of a list of files
     * @return Model aggregated model
     */
    private Model getShapesModel(List<FileInfo> shaclFiles) {
        Model aggregateModel = JenaUtil.createMemoryModel();
        for (FileInfo shaclFile: shaclFiles) {
            logger.info("Validating against ["+shaclFile.getFile().getName()+"]");
            if (shaclFile.getContentLang() == null) {
                throw new ValidatorException("Unable to determine the content type of a SHACL shape file.");
            }
            try (InputStream dataStream = new FileInputStream(shaclFile.getFile())) {
                Model fileModel = JenaUtil.createMemoryModel();
                fileModel.read(dataStream, null, shaclFile.getContentLang());
                
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
        // Determine language.
        Lang lang = null;
        if (this.contentSyntax != null) {
            lang = RDFLanguages.contentTypeToLang(this.contentSyntax);
            if (lang != null) {
                logger.info("Using provided data content type ["+this.contentSyntax+"] as ["+lang.getName()+"]");
            }
        }
        if (lang == null) {
            lang = RDFLanguages.contentTypeToLang(RDFLanguages.guessContentType(dataFile.getName()));
            if (lang != null) {
                logger.info("Guessed lang ["+lang.getName()+"] from file ["+dataFile.getName()+"]");
            }
        }
        if (lang == null) {
            throw new ValidatorException("The RDF language could not be determined for the provided content.");
        }
        try (InputStream dataStream = new FileInputStream(dataFile)) {
            dataModel.read(dataStream, null, lang.getName());
        } catch (Exception e) {
            throw new ValidatorException("An error occurred while reading the provided content: "+e.getMessage(), e);
        }
		return dataModel;
	}
    
    public Model getAggregatedShapes() {
    	return this.aggregatedShapes;
    }

}
