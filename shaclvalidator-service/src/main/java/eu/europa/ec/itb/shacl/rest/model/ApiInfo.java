package eu.europa.ec.itb.shacl.rest.model;

import eu.europa.ec.itb.shacl.DomainConfig;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "The information on how to call the API methods (domain and validation types).")
public class ApiInfo {

    @ApiModelProperty(notes = "The domain value to use in all calls.")
    private String domain;
    @ApiModelProperty(notes = "The supported validation types.")
    private List<ValidationType> validationTypes = new ArrayList<>();

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }

    public List<ValidationType> getValidationTypes() {
        return validationTypes;
    }

    public void setValidationTypes(List<ValidationType> validationTypes) {
        this.validationTypes = validationTypes;
    }

    @ApiModel(description = "Information on an available validation type that can be requested.")
    public static class ValidationType {

        @ApiModelProperty(notes = "The value to use when requesting the validation type.")
        private String type;
        @ApiModelProperty(notes = "The validation type's description.")
        private String description;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static ApiInfo fromDomainConfig(DomainConfig config) {
        ApiInfo info = new ApiInfo();
        info.setDomain(config.getDomainName());
        for (String type: config.getType()) {
            ValidationType typeInfo = new ValidationType();
            typeInfo.setType(type);
            typeInfo.setDescription(config.getTypeLabel().getOrDefault(type, type));
            info.getValidationTypes().add(typeInfo);
        }
        return info;
    }

}
