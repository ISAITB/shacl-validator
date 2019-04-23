package eu.europa.ec.itb.shacl.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eu.europa.ec.itb.shacl.validation.FileContent;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import static eu.europa.ec.itb.shacl.validation.FileContent.embedding_BASE64;
import static eu.europa.ec.itb.shacl.validation.FileContent.embedding_URL;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "A set of rules to apply to the validation.")
public class RuleSet {

    @ApiModelProperty(required = true, notes = "The RDF containing the rules to apply (shapes).")
    private String ruleSet;
    @ApiModelProperty(notes = "The way in which to interpret the value for ruleSet. If not provided, the method will be determined from the ruleSet value (i.e. check it is a valid URL).", allowableValues = embedding_URL+","+embedding_BASE64)
    private String embeddingMethod;
    @ApiModelProperty(notes = "The mime type of the provided RDF content (e.g. \"application/rdf+xml\", \"application/ld+json\", \"text/turtle\"). If not provided the type is determined from the provided content (if possible).")
    private String ruleSyntax;


    public String getRuleSet() { return this.ruleSet; }

    public String getEmbeddingMethod() { return this.embeddingMethod; }

    public void setRuleSet(String ruleSet) {
        this.ruleSet = ruleSet;
    }

    public void setEmbeddingMethod(String embeddingMethod) {
        this.embeddingMethod = embeddingMethod;
    }

    public String getRuleSyntax() {
        return ruleSyntax;
    }

    public void setRuleSyntax(String ruleSyntax) {
        this.ruleSyntax = ruleSyntax;
    }

    public FileContent toFileContent() {
        FileContent content = new FileContent();
        content.setContent(ruleSet);
        content.setEmbeddingMethod(embeddingMethod);
        content.setSyntax(ruleSyntax);
        return content;
    }
}
