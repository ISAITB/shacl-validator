package eu.europa.ec.itb.shacl.gitb;

import com.gitb.core.*;
import com.gitb.tr.TAR;
import com.gitb.vs.Void;
import com.gitb.vs.*;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.InputHelper;
import eu.europa.ec.itb.shacl.util.Utils;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import eu.europa.ec.itb.shacl.validation.ValidationConstants;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.commons.io.FileUtils;
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
	InputHelper inputHelper;
    @Autowired
    ApplicationContext ctx;
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
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_CONTENT, "binary", UsageEnumeration.R, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_CONTENT)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_EMBEDDING_METHOD, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EMBEDDING_METHOD)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_SYNTAX, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_SYNTAX)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_VALIDATION_TYPE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_VALIDATION_TYPE)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_EXTERNAL_RULES, "list[map]", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EXTERNAL_RULES)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_LOAD_IMPORTS, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_LOAD_IMPORTS)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_ADD_RULES_TO_REPORT, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_ADD_RULES_TO_REPORT)));
        return response;
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
		File parentFolder = fileManager.createTemporaryFolderPath();
		try {
			// Validation of the input data
			String contentSyntax = validateContentSyntax(validateRequest);
			ValueEmbeddingEnumeration contentEmbeddingMethod = inputHelper.validateContentEmbeddingMethod(validateRequest, ValidationConstants.INPUT_EMBEDDING_METHOD);
			File contentToValidate = inputHelper.validateContentToValidate(validateRequest, ValidationConstants.INPUT_CONTENT, contentEmbeddingMethod, parentFolder);
			String validationType = inputHelper.validateValidationType(domainConfig, validateRequest, ValidationConstants.INPUT_VALIDATION_TYPE);
			List<FileInfo> externalShapes = inputHelper.validateExternalArtifacts(domainConfig, validateRequest, ValidationConstants.INPUT_EXTERNAL_RULES, ValidationConstants.INPUT_RULE_SET, ValidationConstants.INPUT_EMBEDDING_METHOD, validationType, null, parentFolder);
			Boolean loadImports = inputHelper.validateLoadInputs(domainConfig, getInputLoadImports(validateRequest), validationType);
			boolean addInputToReport = getInputAsBoolean(validateRequest, ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, false);
            boolean addShapesToReport = getInputAsBoolean(validateRequest, ValidationConstants.INPUT_ADD_RULES_TO_REPORT, false);
			SHACLValidator validator = ctx.getBean(SHACLValidator.class, contentToValidate, validationType, contentSyntax, externalShapes, loadImports, domainConfig);
			Model reportModel = validator.validateAll();
			TAR report = Utils.getTAR(
			        reportModel,
                    addInputToReport?contentToValidate.toPath():null,
                    addShapesToReport?validator.getAggregatedShapes():null,
                    domainConfig
            );
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
			FileUtils.deleteQuietly(parentFolder);
		}
    }

    /**
     * Validation of the contentSyntax
     * @param validateRequest The request's parameters.
     * @return The type of syntax.
     */
    private String validateContentSyntax(ValidateRequest validateRequest){
        List<AnyContent> listContentSyntax = Utils.getInputFor(validateRequest, ValidationConstants.INPUT_SYNTAX);
        if (!listContentSyntax.isEmpty()) {
        	AnyContent content = listContentSyntax.get(0);
        	return content.getValue();
        } else {
        	return null;
        }
    }
    private Boolean getInputLoadImports(ValidateRequest validateRequest){
        List<AnyContent> listLoadImports = Utils.getInputFor(validateRequest, ValidationConstants.INPUT_LOAD_IMPORTS);
        if (!listLoadImports.isEmpty()) {
        	AnyContent content = listLoadImports.get(0);
        	return Boolean.valueOf(content.getValue());
        } else {
        	return null;
        }
    }

    private boolean getInputAsBoolean(ValidateRequest validateRequest, String inputName, boolean defaultIfMissing) {
        List<AnyContent> input = Utils.getInputFor(validateRequest, inputName);
        if (!input.isEmpty()) {
            return Boolean.parseBoolean(input.get(0).getValue());
        }
        return defaultIfMissing;
    }
}
