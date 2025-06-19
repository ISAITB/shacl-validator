/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
