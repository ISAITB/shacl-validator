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

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.adapters.AdapterFileManager;
import org.apache.jena.riot.system.stream.StreamManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Custom Jena FileManager that allows us to skip the caching of specific resources defined through a ThreadLocal.
 */
public class CustomJenaFileManager extends AdapterFileManager {

    public static final ThreadLocal<CustomJenaFileManager.CacheParams> PARAMS = new ThreadLocal<>();
    private static final Logger LOG = LoggerFactory.getLogger(CustomJenaFileManager.class);

    private static CustomJenaFileManager instance = null;

    /**
     * Private constructor.
     *
     * @param streamManager The stream manager to use.
     */
    private CustomJenaFileManager(StreamManager streamManager) {
        super(streamManager);
    }

    /**
     * @return The global file manager.
     */
    public static CustomJenaFileManager get() {
        if (instance == null) {
            instance = makeGlobal() ;
        }
        return instance ;
    }

    /**
     * @return The global filemanager.
     */
    public static CustomJenaFileManager makeGlobal() {
        return new CustomJenaFileManager(StreamManager.get());
    }

    /**
     * We override the method caching loaded models to allow us to avoid caching for specific URIs based on ThreadLocal
     * settings.
     * <p/>
     * This is done because the file manager and cache are global, meaning that they span several configuration domains.
     * This means that if a specific domain defines local mappings for certain URIs, these should never be cached given
     * that other domains would then be loading the same locally provided resources. To avoid this, just before triggering
     * the loading of graphs, we mark the URIs that are locally mapped for the specific domain and set them on a ThreadLocal.
     * Later on, when this class is asked to cache the loaded models, we avoid caching for the ones that the specific
     * ThreadLocal states as never to be cached.
     * <p/>
     * USing ThreadLocals for this allows us to have this behaviour differ per calling thread, thus covering different
     * domains.
     */
    @Override
    public void addCacheModel(String uri, Model m) {
        var params = PARAMS.get();
        if (uri != null && params.urisToNotCache().contains(uri)) {
            LOG.debug("Skipping caching for URI {}", uri);
            params.nonCachedModels.put(uri, m);
        } else {
            super.addCacheModel(uri, m);
        }
    }

    /**
     * When retrieving a model from the cache make sure to also check models recorded in the
     * ThreadLocal cache.
     */
    @Override
    public Model getFromCache(String filenameOrURI) {
        Model loadedModel =  super.getFromCache(filenameOrURI);
        if (loadedModel == null) {
            // Check also to see if this was loaded but not cached.
            loadedModel = PARAMS.get().nonCachedModels.get(filenameOrURI);
        }
        return loadedModel;
    }

    /**
     * When retrieving a model from the cache make sure to also check models recorded in the
     * ThreadLocal cache.
     */
    @Override
    public boolean hasCachedModel(String filenameOrURI) {
        return super.hasCachedModel(filenameOrURI) || PARAMS.get().nonCachedModels.containsKey(filenameOrURI);
    }

    /**
     * ThreadLocal data relevant to managing the caching behaviour.
     *
     * @param urisToNotCache The URIs to not cache.
     * @param nonCachedModels The models that were loaded but not cached.
     */
    public record CacheParams(Set<String> urisToNotCache, Map<String, Model> nonCachedModels) {}

}
