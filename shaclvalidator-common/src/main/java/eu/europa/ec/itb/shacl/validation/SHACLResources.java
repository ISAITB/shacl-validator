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

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * RDF resource constants for SHACL.
 */
public class SHACLResources {

    /**
     * Constructor to prevent instantiation.
     */
    private SHACLResources() { throw new IllegalStateException("Utility class"); }

    /** The SHACL namespace. */
    static final String NS_SHACL = "http://www.w3.org/ns/shacl#";
    /** The SHACL information message namespace. */
    static final String SHACL_INFO = NS_SHACL+"Info";
    /** The SHACL warning namespace. */
    static final String SHACL_WARNING = NS_SHACL+"Warning";
    /** The SHACL violation namespace. */
    static final String SHACL_VIOLATION = NS_SHACL+"Violation";
    /** The SHACL validation result namespace. */
    static final String SHACL_VALIDATION_RESULT = NS_SHACL+"ValidationResult";

    /** The conforms property. */
    static final Property CONFORMS = ResourceFactory.createProperty(NS_SHACL, "conforms");
    /** The result property. */
    static final Property RESULT = ResourceFactory.createProperty(NS_SHACL, "result");
    /** The focusNode property. */
    static final Property FOCUS_NODE = ResourceFactory.createProperty(NS_SHACL, "focusNode");
    /** The resultMessage property. */
    static final Property RESULT_MESSAGE = ResourceFactory.createProperty(NS_SHACL, "resultMessage");
    /** The resultSeverity property. */
    static final Property RESULT_SEVERITY = ResourceFactory.createProperty(NS_SHACL, "resultSeverity");
    /** The resultPath property. */
    static final Property RESULT_PATH = ResourceFactory.createProperty(NS_SHACL, "resultPath");
    /** The sourceConstraintComponent property. */
    static final Property SOURCE_CONSTRAINT_COMPONENT = ResourceFactory.createProperty(NS_SHACL, "sourceConstraintComponent");
    /** The value property. */
    static final Property VALUE = ResourceFactory.createProperty(NS_SHACL, "value");
    /** The ValidationReport property. */
    static final Property VALIDATION_REPORT = ResourceFactory.createProperty(NS_SHACL, "ValidationReport");

}
