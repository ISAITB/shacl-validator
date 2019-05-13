package eu.europa.ec.itb.shacl.gitb;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import javax.jws.WebParam;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.gitb.core.AnyContent;
import com.gitb.core.ConfigurationType;
import com.gitb.core.Metadata;
import com.gitb.core.TypedParameter;
import com.gitb.core.TypedParameters;
import com.gitb.core.UsageEnumeration;
import com.gitb.core.ValidationModule;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.TAR;
import com.gitb.vs.GetModuleDefinitionResponse;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.gitb.vs.ValidationService;
import com.gitb.vs.Void;

import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.ValidatorContent;
import eu.europa.ec.itb.shacl.rest.errors.ValidatorException;
import eu.europa.ec.itb.shacl.util.Utils;
import eu.europa.ec.itb.shacl.validation.FileContent;
import eu.europa.ec.itb.shacl.validation.FileInfo;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import eu.europa.ec.itb.shacl.validation.ValidationConstants;

/**
 * Spring component that realises the validation service.
 */
@Component
public class ValidationServiceImpl implements ValidationService {

    /** Logger. **/
    private static final Logger logger = LoggerFactory.getLogger(ValidationServiceImpl.class);
    private final DomainConfig domainConfig;

    @Autowired
    ApplicationContext ctx;
    @Autowired
	ValidatorContent validatorContent;
    @Autowired
	FileManager fileManager;

    public ValidationServiceImpl(DomainConfig domainConfig) {
        this.domainConfig = domainConfig;
    }
    
    /**
     * The purpose of the getModuleDefinition call is to inform its caller on how the service is supposed to be called.
     *
     * In this case its main purpose is to define the input parameters that are expected:
     * <ul>
     *     <li>The required input text (string).</li>
     *     <li>The required expected text (string).</li>
     *     <li>The optional flag to determine if a mismatch is an error (boolean).</li>
     * </ul>
     *
     * @param parameters No parameters are expected.
     * @return The response.
     */
    @Override
    public GetModuleDefinitionResponse getModuleDefinition(@WebParam(name = "GetModuleDefinitionRequest", targetNamespace = "http://www.gitb.com/vs/v1/", partName = "parameters") Void parameters) {
        MDC.put("domain", domainConfig.getDomain());
        GetModuleDefinitionResponse response = new GetModuleDefinitionResponse();
        response.setModule(new ValidationModule());
        response.getModule().setId(domainConfig.getWebServiceId());
        response.getModule().setOperation("V");
        response.getModule().setMetadata(new Metadata());
        response.getModule().getMetadata().setName(domainConfig.getWebServiceId());
        response.getModule().getMetadata().setVersion("1.0.0");
        response.getModule().setInputs(new TypedParameters());
        response.getModule().getInputs().getParam().add(createParameter(ValidationConstants.INPUT_CONTENT, "binary", UsageEnumeration.R, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_CONTENT)));
        response.getModule().getInputs().getParam().add(createParameter(ValidationConstants.INPUT_EMBEDDING_METHOD, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EMBEDDING_METHOD)));
        response.getModule().getInputs().getParam().add(createParameter(ValidationConstants.INPUT_SYNTAX, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_SYNTAX)));
        response.getModule().getInputs().getParam().add(createParameter(ValidationConstants.INPUT_VALIDATION_TYPE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_VALIDATION_TYPE)));
        response.getModule().getInputs().getParam().add(createParameter(ValidationConstants.INPUT_EXTERNAL_RULES, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EXTERNAL_RULES)));
        
        return response;
    }

    /**
     * Create a parameter definition.
     *
     * @param name The name of the parameter.
     * @param type The type of the parameter. This needs to match one of the GITB types.
     * @param use The use (required or optional).
     * @param kind The kind of parameter it is (whether it should be provided as the specific value, as BASE64 content or as a URL that needs to be looked up to obtain the value).
     * @param description The description of the parameter.
     * @return The created parameter.
     */
    private TypedParameter createParameter(String name, String type, UsageEnumeration use, ConfigurationType kind, String description) {
        TypedParameter parameter =  new TypedParameter();
        parameter.setName(name);
        parameter.setType(type);
        parameter.setUse(use);
        parameter.setKind(kind);
        parameter.setDesc(description);
        return parameter;
    }

    /**
     * The validate operation is called to validate the input and produce a validation report.
     *
     * The expected input is described for the service's client through the getModuleDefinition call.
     *
     * @param parameters The input parameters and configuration for the validation.
     * @return The response containing the validation report.
     */
    @Override
    public ValidationResponse validate(@WebParam(name = "ValidateRequest", targetNamespace = "http://www.gitb.com/vs/v1/", partName = "parameters") ValidateRequest validateRequest) {
    	MDC.put("domain", domainConfig.getDomain());

    	//Validation of the input data
    	String contentSyntax = validateContentSyntax(validateRequest);
    	String contentEmbeddingMethod = validateContentEmbeddingMethod(validateRequest);
        File contentToValidate = validateContentToValidate(validateRequest, contentEmbeddingMethod, contentSyntax);
    	String validationType = validateValidationType(validateRequest);
    	List<FileInfo> externalShapes = validateExternalShapes(validateRequest);
    	
    	//Execution of the validation
    	TAR report = executeValidation(contentToValidate, validationType, contentSyntax, domainConfig, externalShapes);
    	
        ValidationResponse result = new ValidationResponse();  
        result.setReport(report);
        return result;
    }
    
    /**
     * Executes the SHACL validation, returning the report of errors.
     * @param inputFile The file to validate.
     * @param validationType The mime type of the provided RDF content.
     * @param contentSyntax The way in which to interpret the contentToValidate.
     * @param domainConfig he domain where the SHACL validator is executed. 
     * @param remoteShaclFiles Any shapes to consider that are externally provided.
     * @return The response containing the validation report.
     */
    private TAR executeValidation(File inputFile, String validationType, String contentSyntax, DomainConfig domainConfig, List<FileInfo> remoteShaclFiles) {
    	TAR report = null;
    	
    	try {	
			SHACLValidator validator = ctx.getBean(SHACLValidator.class, inputFile, validationType, contentSyntax, remoteShaclFiles, domainConfig);
			
			Model reportModel = validator.validateAll();
			
			report = Utils.getTAR(reportModel, inputFile.toPath(), contentSyntax, validator.getAggregatedShapes());
    	}catch(Exception e){
            logger.error("Error during the validation", e);
			throw new ValidatorException(ValidatorException.message_default);
    	}
		
		return report;
    }
    
    /**
     * Validation of the mime type of the provided RDF content.
     * @param validateRequest The request's parameters.
     * @return The type of validation.
     */
    private String validateValidationType(ValidateRequest validateRequest) {
        List<AnyContent> listValidationType = getInputFor(validateRequest, ValidationConstants.INPUT_VALIDATION_TYPE);
        
        if(!listValidationType.isEmpty()) {
	    	AnyContent content = listValidationType.get(0);
	    	String validationType = null;
	    	
	    	if(content.getEmbeddingMethod()==ValueEmbeddingEnumeration.STRING) {
	    		validationType = validatorContent.validateValidationType(content.getValue(), domainConfig);
	    	}else {
			    logger.error(ValidatorException.message_parameters, validationType);    			    
				throw new ValidatorException(ValidatorException.message_parameters);    		
	    	}
	    	
	    	return validationType;
        }else {
        	return null;
        }
    }
    
    /**
     * Validation of the contentSyntax
     * @param validateRequest The request's parameters.
     * @return The type of syntax.
     */
    private String validateContentSyntax(ValidateRequest validateRequest){
        List<AnyContent> listContentSyntax = getInputFor(validateRequest, ValidationConstants.INPUT_SYNTAX);        
        
        if(!listContentSyntax.isEmpty()) {
        	AnyContent content = listContentSyntax.get(0);

        	if(content.getEmbeddingMethod()!=ValueEmbeddingEnumeration.STRING) {
    		    logger.error(ValidatorException.message_parameters, content.getName());    			    
    			throw new ValidatorException(ValidatorException.message_parameters);    		
        	}
        	
        	return content.getValue();
        }else {
        	return null;
        }
    }

    private String validateContentEmbeddingMethod(ValidateRequest validateRequest){
        List<AnyContent> listContentEmbeddingMethod = getInputFor(validateRequest, ValidationConstants.INPUT_EMBEDDING_METHOD);        
        
        if(!listContentEmbeddingMethod.isEmpty()) {
        	AnyContent content = listContentEmbeddingMethod.get(0);

        	return getEmbeddingMethod(content);
        }else {
        	return null;
        }
    }
    
    private String getEmbeddingMethod(AnyContent content) {
    	String value = content.getValue();
    	if (StringUtils.isBlank(value)) {
    		value = FileContent.embedding_BASE64;
    	}
    	
    	if(content.getEmbeddingMethod()!=ValueEmbeddingEnumeration.STRING || !FileContent.isValid(value)) {
		    logger.error(ValidatorException.message_parameters, content.getName());    			    
			throw new ValidatorException(ValidatorException.message_parameters);    		
    	}
    	
    	return value;    	
    }
    
    /**
     * Validation of the content.
     * @param validateRequest The request's parameters.
     * @param contentSyntax2 
     * @return The file to validate.
     */
    private File validateContentToValidate(ValidateRequest validateRequest, String contentEmbeddingMethod, String contentSyntax) {
        List<AnyContent> listContentToValidate = getInputFor(validateRequest, ValidationConstants.INPUT_CONTENT);
        
        if(!listContentToValidate.isEmpty()) {
	    	AnyContent content = listContentToValidate.get(0);
	    	String parameterEmbeddingMethod = getEmbeddingMethodString(content.getEmbeddingMethod());
	    	// As this is declared as "binary" this will always be BASE64.
	    	String valueToProcess = content.getValue();
	    	if (ValueEmbeddingEnumeration.URI.toString().equals(contentEmbeddingMethod)) {
	    		// This is a BASE64 encoded String that is a URI or a CDATA string section.
	    		valueToProcess = new String(Base64.getDecoder().decode(valueToProcess));
	    		parameterEmbeddingMethod = FileContent.embedding_URL;
	    	}
	    	return validatorContent.getContentToValidate(parameterEmbeddingMethod, valueToProcess, contentSyntax);
        }else {
		    logger.error(ValidatorException.message_contentToValidate);    			    
			throw new ValidatorException(ValidatorException.message_contentToValidate);  
        }
    }
    
    private String getEmbeddingMethodString(ValueEmbeddingEnumeration embeddingMethod) {
    	String value = "";
    	if(embeddingMethod.equals(ValueEmbeddingEnumeration.URI)) value = FileContent.embedding_URL;
    	if(embeddingMethod.equals(ValueEmbeddingEnumeration.BASE_64)) value = FileContent.embedding_BASE64;
    	if(embeddingMethod.equals(ValueEmbeddingEnumeration.STRING)) value = FileContent.embedding_STRING;
    	
    	return value;
    }
    
    /**
     * Validation of the external shapes.
     * @param validateRequest The request's parameters.
     * @return The list of external shapes.
     */
    private List<FileInfo> validateExternalShapes(ValidateRequest validateRequest) {
    	List<FileContent> filesContent = new ArrayList<>();
    	List<AnyContent> listInput = getInputFor(validateRequest, ValidationConstants.INPUT_EXTERNAL_RULES);
    	
    	if(!listInput.isEmpty()) {
	    	AnyContent listRuleSets = listInput.get(0);

    		FileContent ruleFileContent = getFileContent(listRuleSets);    		
	    	if(!StringUtils.isEmpty(ruleFileContent.getContent())) filesContent.add(ruleFileContent);
	    	
	    	for(AnyContent content : listRuleSets.getItem()) {
				FileContent fileContent = getFileContent(content);

				if (!StringUtils.isEmpty(fileContent.getContent())) {
					filesContent.add(fileContent);
				}
	    	}
	    	
	    	return getExternalShapes(filesContent);
    	}else {
    		return Collections.emptyList();
    	}
    }
    
    private FileContent getFileContent(AnyContent content) {
		FileContent fileContent = new FileContent();
		String embeddingMethod = "";
		String contentEmbeddingMethod = "";
		
    	for(AnyContent ruleSet : content.getItem()) {
    		if(StringUtils.equals(ruleSet.getName(), ValidationConstants.INPUT_RULE_SET)) {
    			embeddingMethod = getEmbeddingMethodString(ruleSet.getEmbeddingMethod());
        		fileContent.setContent(ruleSet.getValue());
    		}		
    		if(StringUtils.equals(ruleSet.getName(), ValidationConstants.INPUT_RULE_SYNTAX)) {
    			fileContent.setSyntax(ruleSet.getValue());
    		}
    		if(StringUtils.equals(ruleSet.getName(), ValidationConstants.INPUT_EMBEDDING_METHOD)) {
    			contentEmbeddingMethod = getEmbeddingMethod(ruleSet);
    		}
    	}

    	if (ValueEmbeddingEnumeration.URI.toString().equals(contentEmbeddingMethod)) {
    		fileContent.setEmbeddingMethod(FileContent.embedding_URL);
    	}else {
    		if(StringUtils.isNotEmpty(contentEmbeddingMethod)) {
    			fileContent.setEmbeddingMethod(contentEmbeddingMethod);
    		}else {
    			fileContent.setEmbeddingMethod(embeddingMethod);
    		}
    	}
    	
    	return fileContent;
    }

    /**
     * Transforms the list of FileContent to FileInfo.
     * @param externalRules The list of external shapes as FileContent.
     * @return The list of external shapes as FileInfo.
     */
    private List<FileInfo> getExternalShapes(List<FileContent> externalRules) {
		List<FileInfo> shaclFiles;
		if (externalRules != null) {
			shaclFiles = fileManager.getRemoteExternalShapes(externalRules);
		} else {
			shaclFiles = Collections.emptyList();
		}
    	return shaclFiles;
    }

    /**
     * Lookup a provided input from the received request parameters.
     *
     * @param validateRequest The request's parameters.
     * @param name The name of the input to lookup.
     * @return The inputs found to match the parameter name (not null).
     */
    private List<AnyContent> getInputFor(ValidateRequest validateRequest, String name) {
        List<AnyContent> inputs = new ArrayList<>();
        if (validateRequest != null && validateRequest.getInput() != null) {
            for (AnyContent anInput: validateRequest.getInput()) {
                if (name.equals(anInput.getName())) {
                    inputs.add(anInput);
                }
            }
        }
        return inputs;
    }

}
