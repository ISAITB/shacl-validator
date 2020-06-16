package eu.europa.ec.itb.shacl.validation;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * RDF resource constants for SHACL.
 */
public class SHACLResources {

    static final String NS_SHACL = "http://www.w3.org/ns/shacl#";
    static final String SHACL_INFO = NS_SHACL+"Info";
    static final String SHACL_WARNING = NS_SHACL+"Warning";
    static final String SHACL_VIOLATION = NS_SHACL+"Violation";
    static final String SHACL_VALIDATION_RESULT = NS_SHACL+"ValidationResult";

    static final Property CONFORMS = ResourceFactory.createProperty(NS_SHACL, "conforms");
    static final Property RESULT = ResourceFactory.createProperty(NS_SHACL, "result");
    static final Property FOCUS_NODE = ResourceFactory.createProperty(NS_SHACL, "focusNode");
    static final Property RESULT_MESSAGE = ResourceFactory.createProperty(NS_SHACL, "resultMessage");
    static final Property RESULT_SEVERITY = ResourceFactory.createProperty(NS_SHACL, "resultSeverity");
    static final Property RESULT_PATH = ResourceFactory.createProperty(NS_SHACL, "resultPath");
    static final Property SOURCE_CONSTRAINT_COMPONENT = ResourceFactory.createProperty(NS_SHACL, "sourceConstraintComponent");
    static final Property VALUE = ResourceFactory.createProperty(NS_SHACL, "value");
    static final Property VALIDATION_REPORT = ResourceFactory.createProperty(NS_SHACL, "ValidationReport");

}
