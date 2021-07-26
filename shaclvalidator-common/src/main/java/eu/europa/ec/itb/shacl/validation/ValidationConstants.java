package eu.europa.ec.itb.shacl.validation;

/**
 * Constants used to name inputs.
 */
public class ValidationConstants {

    /** The RDF content to validate. */
    public static String INPUT_CONTENT = "contentToValidate";
    /** The explicit content embedding method. */
    public static String INPUT_EMBEDDING_METHOD = "embeddingMethod";
    /** The RDF syntax (as a mime type) of the provided content. */
    public static String INPUT_SYNTAX = "contentSyntax";
    /** The validation type. */
    public static String INPUT_VALIDATION_TYPE = "validationType";
    /** The list of user-provided sets of shapes. */
    public static String INPUT_EXTERNAL_RULES = "externalRules";
    /** A user-provided set of shapes in one rule file. */
    public static String INPUT_RULE_SET = "ruleSet";
    /** The RDF syntax of the user-provided shapes file. */
    public static String INPUT_RULE_SYNTAX = "ruleSyntax";
    /** Whether OWL imports in the provided content should be loaded before validation. */
    public static String INPUT_LOAD_IMPORTS = "loadImports";
    /** Whether the validated content should be added to the TAR report. */
    public static String INPUT_ADD_INPUT_TO_REPORT = "addInputToReport";
    /** Whether the shapes used for the validation should be added to the TAR report. */
    public static String INPUT_ADD_RULES_TO_REPORT = "addRulesToReport";
    /** Whether the SHACL validation report should be added to the TAR report. */
    public static String INPUT_ADD_RDF_REPORT_TO_REPORT = "addRdfReportToReport";
    /** The syntax for the resulting SHACL validation report. */
    public static String INPUT_RDF_REPORT_SYNTAX = "rdfReportSyntax";
    /** The SPARQL query to use for post-processing the SHACL validation report. */
    public static String INPUT_RDF_REPORT_QUERY = "rdfReportQuery";
    /** The SPARQL query to use to load the content to validate. */
    public static String INPUT_CONTENT_QUERY = "contentQuery";
    /** The endpoint URL for the SPARQL query. */
    public static String INPUT_CONTENT_QUERY_ENDPOINT = "contentQueryEndpoint";
    /** The username to authenticate against the SPARQL endpoint. */
    public static String INPUT_CONTENT_QUERY_USERNAME = "contentQueryUsername";
    /** The password to authenticate against the SPARQL endpoint. */
    public static String INPUT_CONTENT_QUERY_PASSWORD = "contentQueryPassword";

}
