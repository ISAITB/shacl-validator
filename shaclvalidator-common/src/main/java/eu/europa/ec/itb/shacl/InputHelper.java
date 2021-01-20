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
    
    public SparqlQueryConfig validateSparqlConfiguration(DomainConfig domainConfig, SparqlQueryConfig inputConfig) {
    	if (!domainConfig.isSupportsQueries()) {
    		throw new ValidatorException("SPARQL queries are not supported.");
		}
		SparqlQueryConfig config = new SparqlQueryConfig();
		// Endpoint
		if (StringUtils.isEmpty(domainConfig.getQueryEndpoint())) {
			if (StringUtils.isEmpty(inputConfig.getEndpoint())) {
				throw new ValidatorException("A SPARQL endpoint is needed to execute the query against.");
			} else {
				config.setEndpoint(inputConfig.getEndpoint());
			}
		} else {
			if (!StringUtils.isEmpty(inputConfig.getEndpoint())) {
				throw new ValidatorException("You cannot provide your own SPARQL endpoint for the validation.");
			} else {
				config.setEndpoint(domainConfig.getQueryEndpoint());
			}
		}
		// Credentials
		if (StringUtils.isNotBlank(inputConfig.getUsername()) || StringUtils.isNotBlank((inputConfig.getPassword()))) {
			// Provided.
			if (domainConfig.getQueryAuthentication() == ExternalArtifactSupport.NONE) {
				throw new ValidatorException("You are not expected to provide credentials for the SPARQL endpoint.");
			}
			config.setUsername(inputConfig.getUsername());
			config.setPassword(inputConfig.getPassword());
		} else {
			// Not provided.
			if (domainConfig.getQueryAuthentication() == ExternalArtifactSupport.REQUIRED) {
				throw new ValidatorException("You must provide your credentials for the SPARQL endpoint.");
			}
			config.setUsername(domainConfig.getQueryUsername());
			config.setPassword(domainConfig.getQueryPassword());
		}
		// Query result content type
		if (StringUtils.isNotBlank(inputConfig.getPreferredContentType())) {
			config.setPreferredContentType(inputConfig.getPreferredContentType());
		} else {
			config.setPreferredContentType(domainConfig.getQueryContentType());
		}
		// Query
		if (StringUtils.isBlank(inputConfig.getQuery())) {
			throw new ValidatorException("You must provide the query for the SPARQL endpoint.");
		} else {
			config.setQuery(inputConfig.getQuery());
		}
		return config;
    }
}
