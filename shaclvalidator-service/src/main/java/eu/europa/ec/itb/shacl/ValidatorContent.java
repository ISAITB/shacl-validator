package eu.europa.ec.itb.shacl;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.europa.ec.itb.shacl.rest.errors.ValidatorException;
import eu.europa.ec.itb.shacl.validation.FileContent;
import eu.europa.ec.itb.shacl.validation.FileManager;

@Component
public class ValidatorContent {
    private static final Logger logger = LoggerFactory.getLogger(ValidatorContent.class);
    
    @Autowired
	FileManager fileManager;

    public String validateValidationType(String validationType, DomainConfig domainConfig) {
    	if((validationType!=null && !domainConfig.getType().contains(validationType)) || (validationType==null && domainConfig.getType().size()!=1)) {
		    logger.error(ValidatorException.message_parameters, validationType);    			    
			throw new ValidatorException(ValidatorException.message_parameters);
    	}
    	
    	return validationType==null ? domainConfig.getType().get(0) : validationType;
    }
    
	public File getContentToValidate(String embeddingMethod, String contentToValidate, String contentSyntax) {
		File contentFile;
		
    	//EmbeddingMethod validation
    	if(embeddingMethod!=null) {
    		switch(embeddingMethod) {
    			case FileContent.embedding_URL:
    				try{
    					contentFile = fileManager.getURLFile(contentToValidate);
    				}catch(IOException e) {
						logger.error("Error when transforming the URL into File.", e);
						throw new ValidatorException(ValidatorException.message_contentToValidate);
					}
    				break;
    			case FileContent.embedding_BASE64:
    			    contentFile = getBase64File(contentToValidate, contentSyntax);
    			    break;
    			case FileContent.embedding_STRING:
    				try{
        				contentFile = fileManager.getStringFile(contentToValidate, contentSyntax);
    				}catch(IOException e) {
						logger.error("Error when transforming the STRING into File.", e);
						throw new ValidatorException(ValidatorException.message_contentToValidate);
					}
    				break;
    			default:
    			    logger.error(ValidatorException.message_parameters, embeddingMethod);    			    
    				throw new ValidatorException(ValidatorException.message_parameters);
    		}
    	}else {
			contentFile = fileManager.getFileAsUrlOrBase64(contentToValidate);
    	}
    	
    	return contentFile;
	}
    
    /**
     * From Base64 string to File
     * @param base64Convert Base64 as String
     * @return File
     */
    private File getBase64File(String base64Convert, String contentSyntax) {
		if (contentSyntax == null) {
			logger.error(ValidatorException.message_parameters, "");
			throw new ValidatorException(ValidatorException.message_parameters);
		}
		try {
			return fileManager.getBase64File(base64Convert);
		} catch (Exception e) {
			logger.error("Error when transforming the Base64 into File.", e);
			throw new ValidatorException(ValidatorException.message_contentToValidate);
		}
    }
}
