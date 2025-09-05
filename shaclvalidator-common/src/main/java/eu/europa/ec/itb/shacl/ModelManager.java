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

package eu.europa.ec.itb.shacl;

import eu.europa.ec.itb.shacl.validation.FileManager;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Class used to track Jena RDF Model instances in use and ensure their cleanup.
 */
public class ModelManager {

    private static final Logger LOG = LoggerFactory.getLogger(ModelManager.class);

    private final FileManager fileManager;
    private final Set<Model> trackedModels = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * Constructor.
     *
     * @param fileManager The file manager to use.
     */
    public ModelManager(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    /**
     * Track the provided model.
     *
     * @param model The model to track.
     */
    public void track(Model model) {
        if (model != null) {
            if (fileManager.isCachedModel(model)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Model not tracked as it is cached");
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Tracking model");
                }
                trackedModels.add(model);
            }
        }
    }

    /**
     * Close all tracked models.
     */
    public void close() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Closing {} models", trackedModels.size());
        }
        try {
            trackedModels.stream().filter(model -> model != null && !fileManager.isCachedModel(model) && !model.isClosed()).forEach(Model::close);
        } catch (Exception e) {
            LOG.warn("Error while closing tracked model", e);
        }
    }

}
