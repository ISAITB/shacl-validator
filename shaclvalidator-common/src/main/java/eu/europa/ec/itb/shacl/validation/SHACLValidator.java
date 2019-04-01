package eu.europa.ec.itb.shacl.validation;

import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.util.Utils;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.topbraid.jenax.util.JenaUtil;
import org.topbraid.shacl.validation.ValidationUtil;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Created by mfontsan on 26/02/2019
 *
 */
@Component
@Scope("prototype")
public class SHACLValidator implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(SHACLValidator.class);

    @Autowired
    private ApplicationConfig config;

    private InputStream inputToValidate;
    private File inputFileToValidate;
    private ApplicationContext ctx;
    private final DomainConfig domainConfig;
    private String validationType;
    private String contentSyntax;

    public SHACLValidator(File inputFileToValidate, String validationType, String contentSyntax, DomainConfig domainConfig) {
    	this.contentSyntax = contentSyntax;
    	this.inputFileToValidate = inputFileToValidate;
		try {
			this.inputToValidate = new FileInputStream(inputFileToValidate);
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage());
		}
        this.validationType = validationType;
        this.domainConfig = domainConfig;
        if (validationType == null) {
            this.validationType = domainConfig.getType().get(0);
        }
    }
    
    public Model validateAll() {
    	logger.info("Starting validation..");
    	
        Model overallResult = null;
		try {
			overallResult = validateAgainstSHACL();
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage());
		}
        
        return overallResult;
    }
    
    /**
     * Validation of the model
     * @return Model The Jena model with the report
     * @throws FileNotFoundException
     */
    public Model validateAgainstSHACL() throws FileNotFoundException {
        File shaclFile = getSHACLFile();
        List<File> shaclFiles = new ArrayList<>();
        if (shaclFile != null && shaclFile.exists()) {
            if (shaclFile.isFile()) {
                // We are pointing to a single master SHACL file.
            	shaclFiles.add(shaclFile);
            } else {
                // All SHACL are processed.
                File[] files = shaclFile.listFiles();
                if (files != null) {
                    for (File aSHACLFile: files) {
                        if (aSHACLFile.isFile() && config.getAcceptedSHACLExtensions().contains(FilenameUtils.getExtension(aSHACLFile.getName().toLowerCase()))) {
                        	shaclFiles.add(aSHACLFile);
                        }
                    }
                }
            }
        }
        if (shaclFiles.isEmpty()) {
            logger.info("No SHACL to validate against ["+shaclFile+"]");
            throw new FileNotFoundException();
        } else {
            for (File aSHACLFile: shaclFiles) {
                logger.info("Validating against ["+aSHACLFile.getName()+"]");
            }
            Model shaclReport = validateSHACL(Utils.getInputStreamForValidation(inputToValidate), shaclFiles);
            return shaclReport;
        }
    }
    
    /**
     * Validate the RDF against one shape file
     * @param inputSource File to validate as InputStream
     * @param shaclFiles The SHACL files
     * @return Model The Jena Model with the report
     */
    private Model validateSHACL(InputStream inputSource, List<File> shaclFiles){
    	Model reportModel = null;
    	
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

    private Model getShapesModel(List<File> shaclFiles) throws FileNotFoundException {
        Model aggregateModel = JenaUtil.createMemoryModel();
        for (File shaclFile: shaclFiles) {
            Lang lang = RDFLanguages.contentTypeToLang(RDFLanguages.guessContentType(shaclFile.getName()));
            try (InputStream dataStream = new FileInputStream(shaclFile)) {
                Model fileModel = JenaUtil.createMemoryModel();
                fileModel.read(dataStream, null, lang.getName());
                aggregateModel.add(fileModel);
            } catch (IOException e) {
                logger.error("Error while reading SHACL file.", e);
                throw new IllegalStateException("Error while reading SHACL file.");
           }
        }
        return aggregateModel;
    }


    private File getSHACLFile() {
        return Paths.get(config.getResourceRoot(), domainConfig.getDomain(), domainConfig.getShaclFile().get(validationType)).toFile();
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

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }
}
