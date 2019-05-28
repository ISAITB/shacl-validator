package eu.europa.ec.itb.shacl;

import eu.europa.ec.itb.shacl.rest.errors.ValidatorException;
import eu.europa.ec.itb.shacl.validation.FileContent;
import eu.europa.ec.itb.shacl.validation.FileManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class ValidatorContent {

    @Autowired
	FileManager fileManager;

    public String validateValidationType(String validationType, DomainConfig domainConfig) {
    	if((validationType!=null && !domainConfig.getType().contains(validationType)) || (validationType==null && domainConfig.getType().size()!=1)) {
			throw new ValidatorException(String.format("The provided validation type [%s] is not valid for domain [%s].", validationType, domainConfig.getDomainName()));
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
						throw new ValidatorException("An error occurred while trying to read the content to validate from the provided URL.", e);
					}
    				break;
    			case FileContent.embedding_BASE64:
    			    contentFile = getBase64File(contentToValidate, contentSyntax);
    			    break;
    			case FileContent.embedding_STRING:
    				try{
        				contentFile = fileManager.getStringFile(contentToValidate, contentSyntax);
    				}catch(IOException e) {
						throw new ValidatorException(String.format("An error occurred while trying to read the content to validate as a string (provided syntax [%s]).", contentSyntax), e);
					}
    				break;
    			default:
    				throw new ValidatorException(String.format("The provided embedding method [%s] is not supported.", embeddingMethod));
    		}
    	}else {
			try {
				contentFile = fileManager.getFileAsUrlOrBase64(contentToValidate);
			} catch (IOException e) {
				throw new ValidatorException("An error occurred while trying to read the provided content.");
			}
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
			throw new ValidatorException("No content syntax was provided. This is required when a file is provided as BASE64.");
		}
		try {
			return fileManager.getBase64File(base64Convert);
		} catch (Exception e) {
			throw new ValidatorException("An error occurred while trying to read a file from a BASE64 text", e);
		}
    }
}
