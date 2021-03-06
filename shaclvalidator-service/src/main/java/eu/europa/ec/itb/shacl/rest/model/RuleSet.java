package eu.europa.ec.itb.shacl.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gitb.core.ValueEmbeddingEnumeration;
import eu.europa.ec.itb.validation.commons.FileContent;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A user-provided set of SHACL shapes to use in the validation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "A set of rules to apply to the validation.")
public class RuleSet {

    @Schema(required = true, description = "The RDF containing the rules to apply (shapes).")
    private String ruleSet;
    @Schema(description = "The way in which to interpret the value for ruleSet. If not provided, the method will be determined from the ruleSet value.", allowableValues = FileContent.EMBEDDING_STRING+","+FileContent.EMBEDDING_URL+","+FileContent.EMBEDDING_BASE_64)
    private String embeddingMethod;
    @Schema(description = "The mime type of the provided RDF content (e.g. \"application/rdf+xml\", \"application/ld+json\", \"text/turtle\"). If not provided the type is determined from the provided content (if possible).")
    private String ruleSyntax;

    /**
     * @return The RDF containing the rules to apply (shapes).
     */
    public String getRuleSet() { return this.ruleSet; }

    /**
     * @return The way in which to interpret the value for ruleSet. If not provided, the method will be determined from
     * the ruleSet value.
     */
    public String getEmbeddingMethod() { return this.embeddingMethod; }

    /**
     * @param ruleSet The RDF containing the rules to apply (shapes).
     */
    public void setRuleSet(String ruleSet) {
        this.ruleSet = ruleSet;
    }

    /**
     * @param embeddingMethod The way in which to interpret the value for ruleSet. If not provided, the method will be
     *                        determined from the ruleSet value.
     */
    public void setEmbeddingMethod(String embeddingMethod) {
        this.embeddingMethod = embeddingMethod;
    }

    /**
     * @return The syntax (mime type) to consider for this specific rule set (shapes).
     */
    public String getRuleSyntax() {
        return ruleSyntax;
    }

    /**
     * @param ruleSyntax The syntax (mime type) to consider for this specific rule set (shapes).
     */
    public void setRuleSyntax(String ruleSyntax) {
        this.ruleSyntax = ruleSyntax;
    }

    /**
     * Wrap the rule set's information metadata into a FileContent instance.
     *
     * @return The rule set information.
     */
    public FileContent toFileContent() {
        if (ruleSyntax == null && FileContent.isValidEmbeddingMethod(embeddingMethod) && FileContent.embeddingMethodFromString(embeddingMethod) == ValueEmbeddingEnumeration.BASE_64) {
            throw new ValidatorException("validator.label.exception.externalBase64ShapesNeedAlsoSyntax");
        }
        FileContent content = new FileContent();
        content.setContent(ruleSet);
        content.setEmbeddingMethod(FileContent.embeddingMethodFromString(embeddingMethod));
        content.setContentType(ruleSyntax);
        return content;
    }
}
