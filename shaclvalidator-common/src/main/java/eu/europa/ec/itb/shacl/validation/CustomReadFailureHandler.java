package eu.europa.ec.itb.shacl.validation;

import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class CustomReadFailureHandler implements OntDocumentManager.ReadFailureHandler {

    public static final ThreadLocal<Set<String>> IMPORTS_WITH_ERRORS = new ThreadLocal<>();
    private static final Logger LOG = LoggerFactory.getLogger(CustomReadFailureHandler.class);

    @Override
    public void handleFailedRead(String url, Model model, Exception e) {
        LOG.warn("Failed to load import [{}]: {}", url, e.getMessage());
        // Use a thread local because this is a shared default instance.
        IMPORTS_WITH_ERRORS.get().add(url);
    }

}
