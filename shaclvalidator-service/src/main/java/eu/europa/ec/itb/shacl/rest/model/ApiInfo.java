package eu.europa.ec.itb.shacl.rest.model;

import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Object providing the information on the validator's supported domains and validation types.
 */
@ApiModel(description = "The information on how to call the API methods (domain and validation types).")
public class ApiInfo {

    @ApiModelProperty(notes = "The domain value to use in all calls.")
    private String domain;
    @ApiModelProperty(notes = "The supported validation types.")
    private List<ValidationType> validationTypes = new ArrayList<>();

    /**
     * @param domain The value to use to identify the domain.
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * @return The value to use to identify the domain.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @return The list of supported validation types for the domain.
     */
    public List<ValidationType> getValidationTypes() {
        return validationTypes;
    }

    /**
     * @param validationTypes The list of supported validation types for the domain.
     */
    public void setValidationTypes(List<ValidationType> validationTypes) {
        this.validationTypes = validationTypes;
    }

    /**
     * Construct the information on a specific domain from the domain's configuration.
     *
     * @param config The domain configuration.
     * @return The domain's API information (domain and validation types).
     */
    public static ApiInfo fromDomainConfig(DomainConfig config) {
        ApiInfo info = new ApiInfo();
        info.setDomain(config.getDomainName());
        var localisationHelper = new LocalisationHelper(config, Locale.ENGLISH);
        for (String type: config.getType()) {
            ValidationType typeInfo = new ValidationType();
            typeInfo.setType(type);
            typeInfo.setDescription(config.getCompleteTypeOptionLabel(type, localisationHelper));
            info.getValidationTypes().add(typeInfo);
        }
        return info;
    }

}
