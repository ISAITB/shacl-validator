package eu.europa.ec.itb.shacl.validation;

import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfig.RemoteInfo;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        File shaclFile = getSHACLFile();	//Internal file
        Map<File, String> shaclFiles = new HashMap<File, String>();
        
        shaclFiles.putAll(getFilesContent(shaclFile));
        shaclFiles.putAll(getRemoteSHACLFiles());
        
        if (shaclFiles.isEmpty()) {
            logger.info("No SHACL to validate against ["+shaclFile+"]");
            throw new FileNotFoundException();
        } else {
            Model shaclReport = validateSHACL(shaclFiles);
            return shaclReport;
        }
    }
    
    /**
     * Remove all SHACL files that has been created in the temporary folder.
     * @param Map<File, String> HashMap of all SHACL files.
     */
    private void removeSHACLFiles(Map<File, String> shaclFiles) {
    	Set<File> files = shaclFiles.keySet();
    	String tmpFolder = config.getTmpFolder();
    	
    	for(File file: files) {
    		if(file.getAbsolutePath().contains(tmpFolder)) {
    			FileManager.removeContentToValidate(file);
    		}
    	}
    }
    
    /**
     * Returns the list of files (if it is a directory) or the file and the corresponding content type.
     * @param File File or directory of the SHACL files
     * @return Map<File, String> with all files and content types
     */
    private Map<File, String> getFilesContent(File shaclFile){
        Map<File, String> shaclFiles = new HashMap<File, String>();
        
    	if (shaclFile != null && shaclFile.exists()) {
            if (shaclFile.isFile()) {
                // We are pointing to a single master SHACL file.
            	shaclFiles.put(shaclFile, getContentLang(shaclFile));
            } else {
                // All SHACL are processed.
                File[] files = shaclFile.listFiles();
                if (files != null) {
                    for (File aSHACLFile: files) {
                        if (aSHACLFile.isFile()) {
                        	shaclFiles.put(aSHACLFile, getContentLang(aSHACLFile));
                        }
                    }
                }
            }
        }
    	
    	return shaclFiles;
    }

    /**
     * Return the SHACL files loaded for a given validation type
     * @return File
     */
    private File getSHACLFile() {
    	String localFolder = domainConfig.getShaclFile().get(validationType).getLocalFolder();
    	File f = null;
    	
    	if(!localFolder.equals(null) && !localFolder.equals("")) {
            f = Paths.get(config.getResourceRoot(), domainConfig.getDomain(), domainConfig.getShaclFile().get(validationType).getLocalFolder()).toFile();    		
    	}
    	
    	return f;
    }
    
    /**
     * Return the remote SHACL files loaded for a given validation type
     * @return Map<File, String>
     */
    private Map<File, String> getRemoteSHACLFiles() {
        Map<File, String> shaclFiles = new HashMap<File, String>();
    	List<RemoteInfo> ri = domainConfig.getShaclFile().get(validationType).getRemote();
    	
		try {
			for(RemoteInfo info: ri) {
				File infoFile = FileManager.getURLFile(info.getUrl(), config.getTmpFolder());
				String contentType = info.getType();
				
				if(RDFLanguages.contentTypeToLang(contentType) != null) {
					shaclFiles.put(infoFile, info.getType());
				}else {
					shaclFiles.put(infoFile, getContentLang(infoFile));
				}
    		}
		} catch (IOException e) {
    		logger.error("Error to load the remote SHACL file: " + e.getMessage());
		}
		
		return shaclFiles;
    }
    
    /**
     * Validate the RDF against one shape file
     * @param shaclFiles The SHACL files
     * @return Model The Jena Model with the report
     */
    private Model validateSHACL(Map<File, String> shaclFiles){
    	Model reportModel = null;
    	
    	try {
			// Get data to validate from file
            Model shaclModel = getShapesModel(shaclFiles);
	        Model dataModel = getDataModel(inputFileToValidate, shaclModel);
	        
			// Perform the validation of data, using the shapes model. Do not validate any shapes inside the data model.
			Resource resource = ValidationUtil.validateModel(dataModel, shaclModel, false);		
			reportModel = resource.getModel();
			reportModel.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");

            removeSHACLFiles(shaclFiles);
    	}catch(Exception e){
    		logger.error("Error during the SHACL validation. " + e.getMessage());
            throw new IllegalStateException(e);
    	}
    	
    	return reportModel;
    }
    
    /**
     * Return the content type as Lang of the File
     * @param File
     * @return String content type as String
     */
    private String getContentLang(File file) {
    	String contentLang = this.contentSyntax;
    	if(contentLang.equals(null) || contentLang.equals("")) {
    		contentLang = RDFLanguages.contentTypeToLang(RDFLanguages.guessContentType(file.getName())).getName();    	
    	}
    	
    	return contentLang;
    }

    /**
     * Return the aggregated model of a list of files
     * @param Map<File, String> SHACL files
     * @return Model aggregated model
     * @throws FileNotFoundException
     */
    private Model getShapesModel(Map<File, String> shaclFiles) throws FileNotFoundException {
        Model aggregateModel = JenaUtil.createMemoryModel();
        Set<File> listFiles = shaclFiles.keySet();
        
        for (File shaclFile: listFiles) {
            logger.info("Validating against ["+shaclFile.getName()+"]");
            
            try (InputStream dataStream = new FileInputStream(shaclFile)) {
                Model fileModel = JenaUtil.createMemoryModel();
                fileModel.read(dataStream, null, shaclFiles.get(shaclFile));
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

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }
}
