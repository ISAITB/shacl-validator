package eu.europa.ec.itb.shacl.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "Information on an available validation type that can be requested.")
public class ValidationType {

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
