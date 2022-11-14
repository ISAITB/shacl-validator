package eu.europa.ec.itb.shacl.gitb;

import com.gitb.core.*;
import com.gitb.vs.Void;
import com.gitb.vs.*;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.InputHelper;
import eu.europa.ec.itb.shacl.ModelPair;
import eu.europa.ec.itb.shacl.SparqlQueryConfig;
import eu.europa.ec.itb.shacl.util.ShaclValidatorUtils;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.ReportSpecs;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import eu.europa.ec.itb.shacl.validation.ValidationConstants;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.ReportPair;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.WebServiceContextProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.xml.ws.WebServiceContext;
import java.io.File;
import java.util.List;

/**
 * Spring component that realises the validation SOAP service.
 */
@Component
@Scope("prototype")
public class ValidationServiceImpl implements ValidationService, WebServiceContextProvider {

    private static final Logger logger = LoggerFactory.getLogger(ValidationServiceImpl.class);
    private final DomainConfig domainConfig;

    @Autowired
	InputHelper inputHelper;
    @Autowired
    ApplicationContext ctx;
    @Autowired
	FileManager fileManager;
    @Resource
    WebServiceContext wsContext;

    /**
     * Constructor.
     *
     * @param domainConfig The domain configuration (each domain has its own instance).
     */
    public ValidationServiceImpl(DomainConfig domainConfig) {
        this.domainConfig = domainConfig;
    }
    
    /**
     * The purpose of the getModuleDefinition call is to inform its caller on how the service is supposed to be called.
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
        UsageEnumeration contentUsage = UsageEnumeration.R;
        if (domainConfig.isSupportsQueries()) {
            contentUsage = UsageEnumeration.O;
        }
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_CONTENT, "binary", contentUsage, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_CONTENT)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_EMBEDDING_METHOD, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EMBEDDING_METHOD)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_SYNTAX, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_SYNTAX)));
        if (domainConfig.hasMultipleValidationTypes() && domainConfig.getDefaultType() == null) {
            response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_VALIDATION_TYPE, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_VALIDATION_TYPE)));
        } else {
            response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_VALIDATION_TYPE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_VALIDATION_TYPE)));
        }
        if (domainConfig.supportsExternalArtifacts()) {
            response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_EXTERNAL_RULES, "list[map]", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_EXTERNAL_RULES)));
        }
        if (domainConfig.supportsUserProvidedLoadImports()) {
            response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_LOAD_IMPORTS, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_LOAD_IMPORTS)));
        }
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_ADD_RULES_TO_REPORT, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_ADD_RULES_TO_REPORT)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_ADD_RDF_REPORT_TO_REPORT, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_ADD_RDF_REPORT_TO_REPORT)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_RDF_REPORT_SYNTAX, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_RDF_REPORT_SYNTAX)));
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_RDF_REPORT_QUERY, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_RDF_REPORT_QUERY)));
        if (domainConfig.isSupportsQueries()) {
            // Query
            response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_CONTENT_QUERY, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_CONTENT_QUERY)));
            // Endpoint
            if (domainConfig.getQueryEndpoint() == null) {
                response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_CONTENT_QUERY_ENDPOINT, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_CONTENT_QUERY_ENDPOINT)));
            }
            // Credentials
            if (domainConfig.getQueryUsername() == null) {
                response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_CONTENT_QUERY_USERNAME, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_CONTENT_QUERY_USERNAME)));
                response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_CONTENT_QUERY_PASSWORD, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_CONTENT_QUERY_PASSWORD)));
            }
        }
        response.getModule().getInputs().getParam().add(Utils.createParameter(ValidationConstants.INPUT_LOCALE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, domainConfig.getWebServiceDescription().get(ValidationConstants.INPUT_LOCALE)));
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
		File contentToValidate;
        var localiser = new LocalisationHelper(domainConfig, Utils.getSupportedLocale(LocaleUtils.toLocale(getInputAsString(validateRequest, ValidationConstants.INPUT_LOCALE, null)), domainConfig));
		try {
			// Validation of the input data
			String contentSyntax = validateContentSyntax(validateRequest);
			ValueEmbeddingEnumeration contentEmbeddingMethod = inputHelper.validateContentEmbeddingMethod(validateRequest, ValidationConstants.INPUT_EMBEDDING_METHOD);
            var queryConfig = parseQueryConfiguration(validateRequest);
			if (queryConfig == null) {
				contentToValidate = inputHelper.validateContentToValidate(validateRequest, ValidationConstants.INPUT_CONTENT, contentEmbeddingMethod, parentFolder);
			} else {
                queryConfig.setPreferredContentType(contentSyntax);
				queryConfig = inputHelper.validateSparqlConfiguration(domainConfig, queryConfig);
                contentToValidate = fileManager.getContentFromSparqlEndpoint(queryConfig, parentFolder).toFile();
                contentSyntax = queryConfig.getPreferredContentType();
			}
			String validationType = inputHelper.validateValidationType(domainConfig, validateRequest, ValidationConstants.INPUT_VALIDATION_TYPE);
			List<FileInfo> externalShapes = inputHelper.validateExternalArtifacts(domainConfig, validateRequest, ValidationConstants.INPUT_EXTERNAL_RULES, ValidationConstants.INPUT_RULE_SET, ValidationConstants.INPUT_EMBEDDING_METHOD, validationType, null, parentFolder);
			Boolean loadImports = inputHelper.validateLoadInputs(domainConfig, getInputLoadImports(validateRequest), validationType);
			boolean addInputToReport = getInputAsBoolean(validateRequest, ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, false);
            boolean addShapesToReport = getInputAsBoolean(validateRequest, ValidationConstants.INPUT_ADD_RULES_TO_REPORT, false);
            boolean addRdfReportToReport = getInputAsBoolean(validateRequest, ValidationConstants.INPUT_ADD_RDF_REPORT_TO_REPORT, false);
			SHACLValidator validator = ctx.getBean(SHACLValidator.class, contentToValidate, validationType, contentSyntax, externalShapes, loadImports, domainConfig, localiser);
			ModelPair models = validator.validateAll();
            var reportSpecs = ReportSpecs.builder(models.getInputModel(), models.getReportModel(), localiser, domainConfig);
            if (addRdfReportToReport) reportSpecs = reportSpecs.withReportContentToInclude(getRdfReportToInclude(models.getReportModel(), validateRequest));
            if (addInputToReport) reportSpecs = reportSpecs.withInputContentToInclude(contentToValidate.toPath());
            if (addShapesToReport) reportSpecs = reportSpecs.withShapesToInclude(validator.getAggregatedShapes());
			ReportPair report = ShaclValidatorUtils.getTAR(reportSpecs.build());
			ValidationResponse result = new ValidationResponse();
			result.setReport(report.getDetailedReport());
			return result;
		} catch (ValidatorException e) {
    		logger.error(e.getMessageForLog(), e);
    		throw new ValidatorException(e.getMessageForDisplay(localiser), true);
		} catch (Exception e) {
			logger.error("Unexpected error", e);
            var message = localiser.localise(ValidatorException.MESSAGE_DEFAULT);
			throw new ValidatorException(message, e, true, (Object[]) null);
		} finally {
			FileUtils.deleteQuietly(parentFolder);
		}
    }

    /**
     * Get the RDF validation report content to include in the resulting TAR report's context.
     *
     * @param reportModel The model of the RDF validation report.
     * @param validateRequest The input parameters.
     * @return The content to inject in the TAR report.
     */
    private String getRdfReportToInclude(Model reportModel, ValidateRequest validateRequest) {
        String mimeType = getInputAsString(validateRequest, ValidationConstants.INPUT_RDF_REPORT_SYNTAX, domainConfig.getDefaultReportSyntax());
        String reportQuery = getInputAsString(validateRequest, ValidationConstants.INPUT_RDF_REPORT_QUERY, null);
        return ShaclValidatorUtils.getRdfReportToIncludeInTAR(reportModel, mimeType, reportQuery, fileManager);
    }

    /**
     * Get the content syntax to use from the provided arguments.
     *
     * @param validateRequest The request's parameters.
     * @return The type of syntax or null if none was provided.
     */
    private String validateContentSyntax(ValidateRequest validateRequest) {
        List<AnyContent> listContentSyntax = Utils.getInputFor(validateRequest, ValidationConstants.INPUT_SYNTAX);
        if (!listContentSyntax.isEmpty()) {
        	AnyContent content = listContentSyntax.get(0);
        	return content.getValue();
        } else {
        	return null;
        }
    }

    /**
     * Get the input on whether to load OWL imports from the provided RDF input.
     *
     * @param validateRequest The input parameters.
     * @return The flag value (null if not provided).
     */
    private Boolean getInputLoadImports(ValidateRequest validateRequest){
        List<AnyContent> listLoadImports = Utils.getInputFor(validateRequest, ValidationConstants.INPUT_LOAD_IMPORTS);
        if (!listLoadImports.isEmpty()) {
        	AnyContent content = listLoadImports.get(0);
        	return Boolean.valueOf(content.getValue());
        } else {
        	return null;
        }
    }

    /**
     * Extract the SPARQL query configuration to consider from the provided input parameters.
     *
     * @param request The input parameters.
     * @return The query configuration to consider (null if not provided).
     */
    private SparqlQueryConfig parseQueryConfiguration(ValidateRequest request) {
        SparqlQueryConfig config = null;
        String query = null;
        String queryEndpoint = null;
        String queryUsername = null;
        String queryPassword = null;
        for (AnyContent inputItem : request.getInput()) {
             if (StringUtils.equals(inputItem.getName(), ValidationConstants.INPUT_CONTENT_QUERY)) {
                 query = inputItem.getValue();
             }
             if (StringUtils.equals(inputItem.getName(), ValidationConstants.INPUT_CONTENT_QUERY_ENDPOINT)) {
                 queryEndpoint = inputItem.getValue();
             }
             if (StringUtils.equals(inputItem.getName(), ValidationConstants.INPUT_CONTENT_QUERY_USERNAME)) {
                 queryUsername = inputItem.getValue();
             }
             if (StringUtils.equals(inputItem.getName(), ValidationConstants.INPUT_CONTENT_QUERY_PASSWORD)) {
                 queryPassword = inputItem.getValue();
             }
         }
        if (query != null || queryEndpoint != null || queryUsername != null || queryPassword != null) {
            config = new SparqlQueryConfig(queryEndpoint, query, queryUsername, queryPassword, null);
        }
        return config;
    }

    /**
     * Get the provided (optional) input as a boolean value.
     *
     * @param validateRequest The input parameters.
     * @param inputName The name of the input to look for.
     * @param defaultIfMissing The default value to use if the input is not provided.
     * @return The value to use.
     */
    private boolean getInputAsBoolean(ValidateRequest validateRequest, String inputName, boolean defaultIfMissing) {
        List<AnyContent> input = Utils.getInputFor(validateRequest, inputName);
        if (!input.isEmpty()) {
            return Boolean.parseBoolean(input.get(0).getValue());
        }
        return defaultIfMissing;
    }

    /**
     * Get the provided (optional) input as a string value.
     *
     * @param validateRequest The input parameters.
     * @param inputName The name of the input to look for.
     * @param defaultIfMissing The default value to use if the input is not provided.
     * @return The value to use.
     */
    private String getInputAsString(ValidateRequest validateRequest, String inputName, String defaultIfMissing) {
        List<AnyContent> input = Utils.getInputFor(validateRequest, inputName);
        if (!input.isEmpty()) {
            return input.get(0).getValue();
        }
        return defaultIfMissing;
    }

    /**
     * @return The web service context.
     */
    @Override
    public WebServiceContext getWebServiceContext(){
        return this.wsContext;
    }

}
