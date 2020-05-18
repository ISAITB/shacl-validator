package eu.europa.ec.itb.shacl.gitb;

import com.gitb.core.*;
import com.gitb.tr.TAR;
import com.gitb.vs.Void;
import com.gitb.vs.*;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.ValidatorContent;
import eu.europa.ec.itb.shacl.errors.ValidatorException;
import eu.europa.ec.itb.shacl.util.Utils;
import eu.europa.ec.itb.shacl.validation.*;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.jws.WebParam;
import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * Spring component that realises the validation service.
 */
@Component
@Scope("prototype")
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
        response.getModule().getInputs().getParam().add(createParameter(ValidationConstants.INPUT_EXTERNAL_RULES, "list[map]", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EXTERNAL_RULES)));
        
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
     * @param validateRequest The input parameters and configuration for the validation.
     * @return The response containing the validation report.
     */
    @Override
    public ValidationResponse validate(@WebParam(name = "ValidateRequest", targetNamespace = "http://www.gitb.com/vs/v1/", partName = "parameters") ValidateRequest validateRequest) {
    	MDC.put("domain", domainConfig.getDomain());
		File contentToValidate = null;
		List<FileInfo> externalShapes = null;
		try {
			//Validation of the input data
			String contentSyntax = validateContentSyntax(validateRequest);
			String contentEmbeddingMethod = validateContentEmbeddingMethod(validateRequest);
			contentToValidate = validateContentToValidate(validateRequest, contentEmbeddingMethod, contentSyntax);
			String validationType = validateValidationType(validateRequest);
			externalShapes = validateExternalShapes(validateRequest);

			//Execution of the validation
			TAR report = executeValidation(contentToValidate, validationType, contentSyntax, domainConfig, externalShapes);

			ValidationResponse result = new ValidationResponse();
			result.setReport(report);
			return result;
		} catch (ValidatorException e) {
    		logger.error(e.getMessage(), e);
    		throw e;
		} catch (Exception e) {
			logger.error("Unexpected error", e);
			throw new ValidatorException(e);
		} finally {
			fileManager.removeContentToValidate(contentToValidate, externalShapes);
		}
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
		SHACLValidator validator = ctx.getBean(SHACLValidator.class, inputFile, validationType, contentSyntax, remoteShaclFiles, domainConfig);
		Model reportModel = validator.validateAll();
		return Utils.getTAR(reportModel, inputFile.toPath(), validator.getAggregatedShapes(), domainConfig);
    }
    
    /**
     * Validation of the mime type of the provided RDF content.
     * @param validateRequest The request's parameters.
     * @return The type of validation.
     */
    private String validateValidationType(ValidateRequest validateRequest) {
        List<AnyContent> listValidationType = getInputFor(validateRequest, ValidationConstants.INPUT_VALIDATION_TYPE);
        String validationType = null;
        if (!listValidationType.isEmpty()) {
	    	AnyContent content = listValidationType.get(0);
	    	if (content.getEmbeddingMethod() == ValueEmbeddingEnumeration.STRING) {
				validationType = content.getValue();
	    	} else {
				throw new ValidatorException(String.format("The validation type to perform must be provided with a [STRING] embeddingMethod. This was provided as [%s].", content.getEmbeddingMethod()));
	    	}
        }
		return validatorContent.validateValidationType(validationType, domainConfig);
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
    	if (!StringUtils.isBlank(value)) {
			if (!FileContent.isValid(value)) {
				throw new ValidatorException(String.format("The provided embedding method [%s] is not valid.", value));
			}
		}
    	return value;
    }
    
    /**
     * Validation of the content.
     * @param validateRequest The request's parameters.
     * @param explicitEmbeddingMethod
	 * @param contentSyntax
	 * @return The file to validate.
     */
    private File validateContentToValidate(ValidateRequest validateRequest, String explicitEmbeddingMethod, String contentSyntax) {
        List<AnyContent> listContentToValidate = getInputFor(validateRequest, ValidationConstants.INPUT_CONTENT);
        
        if(!listContentToValidate.isEmpty()) {
	    	AnyContent content = listContentToValidate.get(0);
			if (explicitEmbeddingMethod == null) {
				explicitEmbeddingMethod = FileContent.fromValueEmbeddingEnumeration(content.getEmbeddingMethod());
			}
			if (explicitEmbeddingMethod == null) {
				// Embedding method not provided as input nor as parameter.
				throw new ValidatorException(String.format("The embedding method needs to be provided either as input parameter [%s] or be set as an attribute on the [%s] input.", ValidationConstants.INPUT_EMBEDDING_METHOD, ValidationConstants.INPUT_CONTENT));
			}
			String valueToProcess = content.getValue();
			if (content.getEmbeddingMethod() == ValueEmbeddingEnumeration.BASE_64 && !FileContent.embedding_BASE64.equals(explicitEmbeddingMethod)) {
				// This is a URI or a plain text string encoded as BASE64.
				valueToProcess = new String(Base64.getDecoder().decode(valueToProcess));
			}
	    	return validatorContent.getContentToValidate(explicitEmbeddingMethod, valueToProcess, contentSyntax);
        } else {
        	throw new ValidatorException(String.format("No content was provided for validation (input parameter [%s]).", ValidationConstants.INPUT_CONTENT));
        }
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
		ValueEmbeddingEnumeration embeddingMethod = null;
		String explicitEmbeddingMethod = null;
		if (content.getItem() != null && !content.getItem().isEmpty()) {
			boolean isRuleSet = false;
			for (AnyContent ruleSet : content.getItem()) {
				if (StringUtils.equals(ruleSet.getName(), ValidationConstants.INPUT_RULE_SET)) {
					embeddingMethod = ruleSet.getEmbeddingMethod();
					fileContent.setContent(ruleSet.getValue());
					isRuleSet = true;
				}
				if (StringUtils.equals(ruleSet.getName(), ValidationConstants.INPUT_RULE_SYNTAX)) {
					fileContent.setSyntax(ruleSet.getValue());
				}
				if(StringUtils.equals(ruleSet.getName(), ValidationConstants.INPUT_EMBEDDING_METHOD)) {
					explicitEmbeddingMethod = getEmbeddingMethod(ruleSet);
				}
			}
			if (isRuleSet) {
				if (explicitEmbeddingMethod == null) {
					explicitEmbeddingMethod = FileContent.fromValueEmbeddingEnumeration(embeddingMethod);
				}
				if (explicitEmbeddingMethod == null) {
					// Embedding method not provided as input nor as parameter.
					throw new ValidatorException(String.format("For user-provided SHACL shapes the embedding method needs to be provided either as a separate input [%s] or as an attribute of the [%s] input.", ValidationConstants.INPUT_EMBEDDING_METHOD, ValidationConstants.INPUT_RULE_SET));
				}
				if (embeddingMethod == ValueEmbeddingEnumeration.BASE_64 && !FileContent.embedding_BASE64.equals(explicitEmbeddingMethod)) {
					// This is a URI or a plain text string encoded as BASE64.
					fileContent.setContent(new String(Base64.getDecoder().decode(fileContent.getContent())));
				}
				fileContent.setEmbeddingMethod(explicitEmbeddingMethod);
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
			try {
				shaclFiles = fileManager.getRemoteExternalShapes(externalRules);
			} catch (Exception e) {
				throw new ValidatorException("An error occurred while trying to read the provided external shapes.", e);
			}
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
