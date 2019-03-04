package eu.europa.ec.itb.shacl.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
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
import org.topbraid.shacl.util.ModelPrinter;
import org.topbraid.shacl.validation.ValidationUtil;

import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TAR;

import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.util.Utils;

/**
 * 
 * Created by mfontsan on 26/02/2019
 *
 */
@Component
@Scope("prototype")
public class SHACLValidator implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(SHACLValidator.class);
    private static JAXBContext VALIDATION_REPORT_JAXB_CONTEXT;

    @Autowired
    private ApplicationConfig config;

    private InputStream inputToValidate;
    private File inputFileToValidate;
    private ApplicationContext ctx;
    private final DomainConfig domainConfig;
    private String validationType;
    private ObjectFactory gitbTRObjectFactory = new ObjectFactory();

    static {
    	//TODO: SHACLOutputType.java
        /*try {
        	VALIDATION_REPORT_JAXB_CONTEXT = JAXBContext.newInstance(SHACLOutputType.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create JAXB content for SHACLOutputType", e);
        }*/
    }

    public SHACLValidator(File inputFileToValidate, String validationType, DomainConfig domainConfig) {
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
    
    public TAR validateAll() {
    	logger.info("Starting validation..");
    	//For the moment, we just validate SHACL commited in Bitbucket.
        TAR overallResult = null;
		try {
			overallResult = validateAgainstSHACL();
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage());
		}
        
        return overallResult;
    }
    
    public TAR validateAgainstSHACL() throws FileNotFoundException {
        File shaclFile = getSHACLFile();
        List<TAR> reports = new ArrayList<TAR>();
        List<File> shaclFiles = new ArrayList<>();
        if (shaclFile != null && shaclFile.exists()) {
            if (shaclFile.isFile()) {
                // We are pointing to a single master SHACL file.
            	shaclFiles.add(shaclFile);
            } else {
                // All SHACL are to be processed.
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
            return null;
        } else {
            for (File aSHACLFile: shaclFiles) {
                logger.info("Validating against ["+aSHACLFile.getName()+"]");
                TAR report = validateSHACL(Utils.getInputStreamForValidation(inputToValidate), aSHACLFile);
                TARReport.logReport(report, aSHACLFile.getName());
                reports.add(report);
                logger.info("Validated against ["+aSHACLFile.getName()+"]");
            }
            TAR report = TARReport.mergeReports(reports.toArray(new TAR[reports.size()]));
            report = TARReport.completeReport(report, inputToValidate);
            
            return report;
        }
    }
    
    private TAR validateSHACL(InputStream inputSource, File shaclFile){
    	TAR report = new TAR();
        boolean convertXPathExpressions = false;
    	
    	try {
			// Get data to validate from file
	        Model shaclModel = getDataModel(shaclFile, null);
	        Model dataModel = getDataModel(inputFileToValidate, shaclModel);
	        
			// Perform the validation of data, using the shapes model. Do not validate any shapes inside the data model.
			Resource resource = ValidationUtil.validateModel(dataModel, shaclModel, false);		
			Model reportModel = resource.getModel();
			reportModel.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
	
			String ttlResult = ModelPrinter.get().print(reportModel);
			
			// Get the ttl Result and Table Result		
			List<ValidationResult> validationResultsList = SHACLValidationReport.formatOutput(reportModel);
			//String ttlResult = ModelPrinter.get().print(reportModel);
			
			// Get the data and SHACL as string from their models
			//String data = getStringFromModel(dataModel);
			//String shapes = getStringFromModel(shaclModel);
			
			SHACLValidationReport handler = new SHACLValidationReport(validationResultsList, convertXPathExpressions, false, false);
			report = handler.createReport();
			
    	}catch(Exception e){
    		logger.error("Error during the SHACL validation. " + e.getMessage());
    	}
    	
    	return report;
    }
    
    private File getSHACLFile() {  	
        return Paths.get(config.getResourceRoot(), domainConfig.getDomain(), domainConfig.getShaclFile().get(validationType)).toFile();
    }
    
	/**
     * Get data to validate from file, and combine with the vocabulary
     * @param shapesModel
     * 			The Jena model containing the shacl defintion (needed to set the proper prefixes on the input data)
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 * @throws ServletException 
     */
    private Model getDataModel(File dataFile, Model shapesModel) throws FileNotFoundException {    	
    	String extension = FilenameUtils.getExtension(dataFile.getName());
		InputStream dataStream = new FileInputStream(dataFile);
            	
		// Upload the data in the Model. First set the prefixes of the model to those of the shapes model to avoid mismatches.
		Model dataModel = JenaUtil.createMemoryModel();
		
		if(shapesModel!=null) {
			dataModel.setNsPrefixes(shapesModel.getNsPrefixMap());
		}
		
		dataModel.read(dataStream, null, RDFLanguages.fileExtToLang(extension).getName());

		return dataModel;
	}

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }
}
