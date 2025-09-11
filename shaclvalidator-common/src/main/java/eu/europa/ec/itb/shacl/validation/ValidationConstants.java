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

package eu.europa.ec.itb.shacl.validation;

/**
 * Constants used to name inputs.
 */
public class ValidationConstants {

    /**
     * Constructor to prevent instantiation.
     */
    private ValidationConstants() { throw new IllegalStateException("Utility class"); }

    /** The RDF content to validate. */
    public static final String INPUT_CONTENT = "contentToValidate";
    /** The explicit content embedding method. */
    public static final String INPUT_EMBEDDING_METHOD = "embeddingMethod";
    /** The RDF syntax (as a mime type) of the provided content. */
    public static final String INPUT_SYNTAX = "contentSyntax";
    /** The validation type. */
    public static final String INPUT_VALIDATION_TYPE = "validationType";
    /** The list of user-provided sets of shapes. */
    public static final String INPUT_EXTERNAL_RULES = "externalRules";
    /** A user-provided set of shapes in one rule file. */
    public static final String INPUT_RULE_SET = "ruleSet";
    /** The RDF syntax of the user-provided shapes file. */
    public static final String INPUT_RULE_SYNTAX = "ruleSyntax";
    /** Whether OWL imports in the provided content should be loaded before validation. */
    public static final String INPUT_LOAD_IMPORTS = "loadImports";
    /** Whether the input graph should be merged with the shape graph before validation. */
    public static final String INPUT_MERGE_MODELS_BEFORE_VALIDATION = "mergeModelsBeforeValidation";
    /** Whether the validated content should be added to the TAR report. */
    public static final String INPUT_ADD_INPUT_TO_REPORT = "addInputToReport";
    /** Whether the shapes used for the validation should be added to the TAR report. */
    public static final String INPUT_ADD_RULES_TO_REPORT = "addRulesToReport";
    /** Whether the SHACL validation report should be added to the TAR report. */
    public static final String INPUT_ADD_RDF_REPORT_TO_REPORT = "addRdfReportToReport";
    /** The syntax for the resulting SHACL validation report. */
    public static final String INPUT_RDF_REPORT_SYNTAX = "rdfReportSyntax";
    /** The SPARQL query to use for post-processing the SHACL validation report. */
    public static final String INPUT_RDF_REPORT_QUERY = "rdfReportQuery";
    /** The SPARQL query to use to load the content to validate. */
    public static final String INPUT_CONTENT_QUERY = "contentQuery";
    /** The endpoint URL for the SPARQL query. */
    public static final String INPUT_CONTENT_QUERY_ENDPOINT = "contentQueryEndpoint";
    /** The username to authenticate against the SPARQL endpoint. */
    public static final String INPUT_CONTENT_QUERY_USERNAME = "contentQueryUsername";
    /** The password to authenticate against the SPARQL endpoint. */
    public static final String INPUT_CONTENT_QUERY_PASSWORD = "contentQueryPassword";
    /** The locale string to consider. */
    public static final String INPUT_LOCALE = "locale";

}
