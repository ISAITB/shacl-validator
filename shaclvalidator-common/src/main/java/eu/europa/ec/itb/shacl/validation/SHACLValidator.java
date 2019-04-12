package eu.europa.ec.itb.shacl.validation;

import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;

import org.apache.jena.rdf.model.Model;
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
import java.util.List;

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
    private ApplicationConfig config;

    @Autowired
    private FileManager fileManager;

    private File inputFileToValidate;
    private final DomainConfig domainConfig;
    private String validationType;
    private String contentSyntax;
    private List<FileInfo> filesInfo;

    public SHACLValidator(File inputFileToValidate, String validationType, String contentSyntax, List<FileInfo> fi, DomainConfig domainConfig) {
    	this.contentSyntax = contentSyntax;
    	this.inputFileToValidate = inputFileToValidate;
        this.validationType = validationType;
        this.domainConfig = domainConfig;
        this.filesInfo = fi;
        if (validationType == null) {
            this.validationType = domainConfig.getType().get(0);
        }
    }
    
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
                logger.info("No SHACL files to validate against");
                throw new IllegalStateException("No SHACL files to validate against");
            } else {
                return validateShacl(shaclFiles);
            }
        } finally {
            fileManager.signalValidationEnd(domainConfig.getDomainName());
        }
    }

    /**
     * Validate the RDF against one shape file
     * @param shaclFiles The SHACL files
     * @return Model The Jena Model with the report
     */
    private Model validateShacl(List<FileInfo> shaclFiles){
    	Model reportModel;
    	
    	try {
			// Get data to validate from file
            Model shaclModel = getShapesModel(shaclFiles);
	        Model dataModel = getDataModel(inputFileToValidate, shaclModel);
	        
			// Perform the validation of data, using the shapes model. Do not validate any shapes inside the data model.
			Resource resource = ValidationUtil.validateModel(dataModel, shaclModel, false);		
			reportModel = resource.getModel();
			reportModel.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");

    	}catch(Exception e){
    		logger.error("Error during the SHACL validation. " + e.getMessage());
            throw new IllegalStateException(e);
    	}
    	
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
            try (InputStream dataStream = new FileInputStream(shaclFile.getFile())) {
                Model fileModel = JenaUtil.createMemoryModel();
                fileModel.read(dataStream, null, shaclFile.getContentLang());
                aggregateModel.add(fileModel);
            } catch (IOException e) {
                logger.error("Error while reading SHACL file.", e);
                throw new IllegalStateException("Error while reading SHACL file.");
           }
        }
        return aggregateModel;
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
            logger.error("RDF Language could not be determined for data.");
            throw new IllegalStateException("RDF Language could not be determined for data.");
        }
        try (InputStream dataStream = new FileInputStream(dataFile)) {
            dataModel.read(dataStream, null, lang.getName());
        } catch (IOException e) {
            logger.error("Error while reading data.", e);
            throw new IllegalStateException("Error while reading data.");
        }
		return dataModel;
	}

}
