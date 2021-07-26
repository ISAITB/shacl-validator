package eu.europa.ec.itb.shacl.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A supported validation type for a specific domain.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "Information on an available validation type that can be requested.")
public class ValidationType {

    @ApiModelProperty(notes = "The value to use when requesting the validation type.")
    private String type;
    @ApiModelProperty(notes = "The validation type's description.")
    private String description;

    /**
     * @return The validation type identifier.
     */
    public String getType() {
        return type;
    }

    /**
     * @param type The validation type identifier.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return The validation type description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description The validation type description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

}
