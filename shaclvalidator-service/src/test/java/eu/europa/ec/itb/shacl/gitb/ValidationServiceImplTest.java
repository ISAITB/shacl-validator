package eu.europa.ec.itb.shacl.gitb;

import com.gitb.core.TypedParameter;
import com.gitb.core.UsageEnumeration;
import com.gitb.vs.Void;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.validation.ValidationConstants;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.ValidationArtifactInfo;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class ValidationServiceImplTest {

    @Test
    void testGetModuleDefinition() {
        var domainConfig = mock(DomainConfig.class);
        doReturn("domain1").when(domainConfig).getDomain();
        doReturn("service1").when(domainConfig).getWebServiceId();
        doReturn(true).when(domainConfig).hasMultipleValidationTypes();
        doReturn(true).when(domainConfig).isSupportsQueries();
        doReturn(true).when(domainConfig).supportsExternalArtifacts();
        doReturn(true).when(domainConfig).supportsUserProvidedLoadImports();
        doReturn(true).when(domainConfig).supportsUserProvidedMergeModelsBeforeValidation();
        doReturn(true).when(domainConfig).isSupportsQueries();
        doReturn(null).when(domainConfig).getQueryEndpoint();
        doReturn(null).when(domainConfig).getQueryUsername();
        doAnswer((Answer<?>) ctx -> {
            var info = Map.of("type1", new TypedValidationArtifactInfo(), "type2", new TypedValidationArtifactInfo());
            info.get("type1").add(TypedValidationArtifactInfo.DEFAULT_TYPE, new ValidationArtifactInfo());
            info.get("type1").get().setExternalArtifactSupport(ExternalArtifactSupport.NONE);
            info.get("type2").add(TypedValidationArtifactInfo.DEFAULT_TYPE, new ValidationArtifactInfo());
            info.get("type2").get().setExternalArtifactSupport(ExternalArtifactSupport.NONE);
            return info;
        }).when(domainConfig).getArtifactInfo();
        doReturn(Map.ofEntries(
                descriptionEntryOf(ValidationConstants.INPUT_CONTENT),
                descriptionEntryOf(ValidationConstants.INPUT_EMBEDDING_METHOD),
                descriptionEntryOf(ValidationConstants.INPUT_SYNTAX),
                descriptionEntryOf(ValidationConstants.INPUT_VALIDATION_TYPE),
                descriptionEntryOf(ValidationConstants.INPUT_EXTERNAL_RULES),
                descriptionEntryOf(ValidationConstants.INPUT_LOAD_IMPORTS),
                descriptionEntryOf(ValidationConstants.INPUT_MERGE_MODELS_BEFORE_VALIDATION),
                descriptionEntryOf(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT),
                descriptionEntryOf(ValidationConstants.INPUT_ADD_RULES_TO_REPORT),
                descriptionEntryOf(ValidationConstants.INPUT_ADD_RDF_REPORT_TO_REPORT),
                descriptionEntryOf(ValidationConstants.INPUT_RDF_REPORT_SYNTAX),
                descriptionEntryOf(ValidationConstants.INPUT_RDF_REPORT_QUERY),
                descriptionEntryOf(ValidationConstants.INPUT_CONTENT_QUERY),
                descriptionEntryOf(ValidationConstants.INPUT_CONTENT_QUERY_ENDPOINT),
                descriptionEntryOf(ValidationConstants.INPUT_CONTENT_QUERY_USERNAME),
                descriptionEntryOf(ValidationConstants.INPUT_CONTENT_QUERY_PASSWORD),
                descriptionEntryOf(ValidationConstants.INPUT_LOCALE)
        )).when(domainConfig).getWebServiceDescription();
        var service = new ValidationServiceImpl(domainConfig, domainConfig);
        var result = service.getModuleDefinition(new Void());
        assertNotNull(result);
        assertNotNull(result.getModule());
        assertNotNull(result.getModule().getInputs());
        assertEquals(17, result.getModule().getInputs().getParam().size());
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_CONTENT, result.getModule().getInputs().getParam().get(0), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_EMBEDDING_METHOD, result.getModule().getInputs().getParam().get(1), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_SYNTAX, result.getModule().getInputs().getParam().get(2), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_VALIDATION_TYPE, result.getModule().getInputs().getParam().get(3), UsageEnumeration.R);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_EXTERNAL_RULES, result.getModule().getInputs().getParam().get(4), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_LOAD_IMPORTS, result.getModule().getInputs().getParam().get(5), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_MERGE_MODELS_BEFORE_VALIDATION, result.getModule().getInputs().getParam().get(6), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_ADD_INPUT_TO_REPORT, result.getModule().getInputs().getParam().get(7), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_ADD_RULES_TO_REPORT, result.getModule().getInputs().getParam().get(8), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_ADD_RDF_REPORT_TO_REPORT, result.getModule().getInputs().getParam().get(9), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_RDF_REPORT_SYNTAX, result.getModule().getInputs().getParam().get(10), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_RDF_REPORT_QUERY, result.getModule().getInputs().getParam().get(11), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_CONTENT_QUERY, result.getModule().getInputs().getParam().get(12), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_CONTENT_QUERY_ENDPOINT, result.getModule().getInputs().getParam().get(13), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_CONTENT_QUERY_USERNAME, result.getModule().getInputs().getParam().get(14), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_CONTENT_QUERY_PASSWORD, result.getModule().getInputs().getParam().get(15), UsageEnumeration.O);
        assertWebServiceInputDocumentation(ValidationConstants.INPUT_LOCALE, result.getModule().getInputs().getParam().get(16), UsageEnumeration.O);
    }

    private Map.Entry<String, String> descriptionEntryOf(String inputName) {
        return Map.entry(inputName, "Description of "+inputName);
    }

    private void assertWebServiceInputDocumentation(String inputName, TypedParameter parameter, UsageEnumeration usage) {
        assertEquals(inputName, parameter.getName());
        assertEquals("Description of "+inputName, parameter.getDesc());
        assertEquals(usage, parameter.getUse());
    }
}
