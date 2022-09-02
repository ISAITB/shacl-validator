package eu.europa.ec.itb.shacl.validation;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Failure handler to record any remote resources that have failed to be loaded as part of a single model loading
 * operation. This class uses a {@link ThreadLocal} to record and communicate back the failed URLs so that they
 * can be reported accordingly.
 */
public class CustomReadFailureHandler implements OntDocumentManager.ReadFailureHandler {

    public static final ThreadLocal<Set<Pair<String, String>>> IMPORTS_WITH_ERRORS = new ThreadLocal<>();
    private static final Logger LOG = LoggerFactory.getLogger(CustomReadFailureHandler.class);

    /**
     * Log and record the failure.
     *
     * {@inheritDoc}
     */
    @Override
    public void handleFailedRead(String url, Model model, Exception e) {
        LOG.warn("Failed to load import [{}]: {}", url, e.getMessage());
        // Use a thread local because this is a shared default instance.
        IMPORTS_WITH_ERRORS.get().add(Pair.of(url, e.getMessage()));
    }

}
