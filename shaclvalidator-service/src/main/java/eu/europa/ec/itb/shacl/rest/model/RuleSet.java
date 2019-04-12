package eu.europa.ec.itb.shacl.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "A set of rules to apply to the validation.")
public class RuleSet {

    @ApiModelProperty(required = true, notes = "The RDF containing the rules to apply (shapes).")
    private String ruleSet;
    @ApiModelProperty(notes = "The way in which to interpret the value for ruleSet. If not provided, the method will be determined from the ruleSet value (i.e. check it is a valid URL).", allowableValues = Input.embedding_URL+","+Input.embedding_BASE64)
    private String embeddingMethod;

    public String getRuleSet() { return this.ruleSet; }

    public String getEmbeddingMethod() { return this.embeddingMethod; }

    public void setRuleSet(String ruleSet) {
        this.ruleSet = ruleSet;
    }

    public void setEmbeddingMethod(String embeddingMethod) {
        this.embeddingMethod = embeddingMethod;
    }

}
