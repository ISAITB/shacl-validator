package eu.europa.ec.itb.shacl;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Component to load, record and share the domain configurations.
 */
@Component
public class DomainConfigCache extends WebDomainConfigCache<DomainConfig> {

    @Autowired
    private ApplicationConfig appConfig = null;

    @Override
    @PostConstruct
    public void init() {
        super.init();
    }

    /**
     * Create a new and empty domain configuration object.
     *
     * @return The object.
     */
    @Override
    protected DomainConfig newDomainConfig() {
        return new DomainConfig();
    }

    /**
     * @see eu.europa.ec.itb.validation.commons.config.DomainConfigCache#getSupportedChannels()
     *
     * @return Form, SOAP and REST API.
     */
    @Override
    protected ValidatorChannel[] getSupportedChannels() {
        return new ValidatorChannel[] {ValidatorChannel.FORM, ValidatorChannel.SOAP_API, ValidatorChannel.REST_API};
    }

    /**
     * Extend the domain configuration loading with JSON-specific information.
     *
     * @param domainConfig The domain configuration to enrich.
     * @param config The configuration properties to consider.
     */
    @Override
    protected void addDomainConfiguration(DomainConfig domainConfig, Configuration config) {
        super.addDomainConfiguration(domainConfig, config);
        addValidationArtifactInfo("validator.shaclFile", "validator.externalShapes", null, domainConfig, config);
        domainConfig.setDefaultReportSyntax(config.getString("validator.defaultReportSyntax", appConfig.getDefaultReportSyntax()));
        domainConfig.setWebContentSyntax(Arrays.stream(StringUtils.split(config.getString("validator.contentSyntax", ""), ',')).map(String::trim).collect(Collectors.toList()));
        domainConfig.setReportsOrdered(config.getBoolean("validator.reportsOrdered", false));
        domainConfig.setMergeModelsBeforeValidation(config.getBoolean("validator.mergeModelsBeforeValidation", true));
        // SPARQL query configuration - start
        domainConfig.setQueryEndpoint(config.getString("validator.queryEndpoint"));
        domainConfig.setQueryUsername(config.getString("validator.queryUsername"));
        domainConfig.setQueryPassword(config.getString("validator.queryPassword"));
        ExternalArtifactSupport queryAuthenticationInput;
        if (domainConfig.getQueryUsername() == null && domainConfig.getQueryPassword() == null) {
            // No predefined credentials
            queryAuthenticationInput = ExternalArtifactSupport.byName(config.getString("validator.queryAuthenticationInput", ExternalArtifactSupport.OPTIONAL.getName()));
        } else {
            // Predefined credentials.
            queryAuthenticationInput = ExternalArtifactSupport.NONE;
        }
        domainConfig.setQueryAuthentication(queryAuthenticationInput);
        domainConfig.setQueryContentType(config.getString("validator.queryPreferredContentType", appConfig.getQueryPreferredContentType()));
        boolean hasQueryConfiguration = (domainConfig.getQueryEndpoint() != null || domainConfig.getQueryUsername() != null || domainConfig.getQueryPassword() != null);
        // If not explicitly set, we allow queries if there are query-related configuration properties.
        domainConfig.setSupportsQueries(config.getBoolean("validator.supportsQueries", hasQueryConfiguration));
        // SPARQL query configuration - end
        domainConfig.setDefaultLoadImportsType(parseBooleanMap("validator.loadImports", config, domainConfig.getType(), config.getBoolean("validator.loadImports", false)));
        domainConfig.setUserInputForLoadImportsType(parseEnumMap("validator.input.loadImports", ExternalArtifactSupport.byName(config.getString("validator.input.loadImports", ExternalArtifactSupport.NONE.getName())), config, domainConfig.getType(), ExternalArtifactSupport::byName));
        addMissingDefaultValues(domainConfig.getWebServiceDescription(), appConfig.getDefaultLabels());
    }

}
