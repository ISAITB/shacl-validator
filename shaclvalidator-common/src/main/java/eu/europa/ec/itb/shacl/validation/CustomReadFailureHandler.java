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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.rdf.model.Model;

import java.util.Set;

/**
 * Failure handler to record any remote resources that have failed to be loaded as part of a single model loading
 * operation. This class uses a {@link ThreadLocal} to record and communicate back the failed URLs so that they
 * can be reported accordingly.
 */
public class CustomReadFailureHandler implements OntDocumentManager.ReadFailureHandler {

    public static final ThreadLocal<Set<Pair<String, String>>> IMPORTS_WITH_ERRORS = new ThreadLocal<>();

    /**
     * Log and record the failure.
     *
     * {@inheritDoc}
     */
    @Override
    public void handleFailedRead(String url, Model model, Exception e) {
        // Use a thread local because this is a shared default instance.
        IMPORTS_WITH_ERRORS.get().add(Pair.of(url, e.getMessage()));
    }

}
