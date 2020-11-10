package eu.europa.ec.itb.shacl;

import com.gitb.core.AnyContent;

import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.ValidationConstants;
import eu.europa.ec.itb.validation.commons.BaseInputHelper;
import eu.europa.ec.itb.validation.commons.FileContent;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class InputHelper extends BaseInputHelper<FileManager, DomainConfig, ApplicationConfig> {

    @Override
    public void populateFileContentFromInput(FileContent fileContent, AnyContent inputItem) {
        if (StringUtils.equals(inputItem.getName(), ValidationConstants.INPUT_RULE_SYNTAX)) {
            fileContent.setContentType(inputItem.getValue());
        }
    }
    
    public Boolean validateLoadInputs(DomainConfig domainConfig, Boolean artifactInput, String validationType) {
    	ExternalArtifactSupport inputLoadImportsType = domainConfig.getUserInputForLoadImportsType().get(validationType);
    	boolean loadImportsType = domainConfig.getDefaultLoadImportsType().get(validationType);

    	if (inputLoadImportsType == ExternalArtifactSupport.REQUIRED && artifactInput==null) {
            throw new ValidatorException(String.format("Validation type [%s] expects the choice of whether or not imports are to be loaded (%s).", validationType, ValidationConstants.INPUT_LOAD_IMPORTS));
    	} 
		if (inputLoadImportsType == ExternalArtifactSupport.NONE && artifactInput!=null) {
			throw new ValidatorException(String.format("Validation type [%s] does not expect the choice of whether or not imports are to be loaded (%s).", validationType, ValidationConstants.INPUT_LOAD_IMPORTS));			
		}
		if((inputLoadImportsType == ExternalArtifactSupport.OPTIONAL || inputLoadImportsType == ExternalArtifactSupport.NONE) && artifactInput==null) {
			artifactInput = loadImportsType;
		}
		
    	
    	return artifactInput;
    }
}
