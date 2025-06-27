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

package eu.europa.ec.itb.shacl.config;

import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.irix.IRIs;
import org.apache.jena.riot.system.stream.LocatorClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replacement of Jena's built-in {@link LocatorClassLoader} which depending on the JRE could result in a different
 * type of classloader that could, for HTTP/HTTPS URIs, attempt to retrieve the remote resource. If the resource was
 * not found this could be a text or HTML response that should never have been returned. This implementation ensures
 * that only URIs with no scheme or at least the file or jar schema are attempted to be loaded..
 */
public class CustomClassLoaderLocator extends LocatorClassLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomClassLoaderLocator.class);

    /**
     * @param classLoader The classloader to use.
     */
    public CustomClassLoaderLocator(ClassLoader classLoader) {
        super(classLoader);
    }

    /**
     * Only attempt to load the resource if it could be an internal resource on the classpath (not a remote resource).
     *
     * @param resourceName The resource name.
     */
    @Override
    public TypedInputStream open(String resourceName) {
        String uriSchemeName = IRIs.scheme(resourceName) ;
        if (uriSchemeName == null || uriSchemeName.equalsIgnoreCase("file") || uriSchemeName.equalsIgnoreCase("jar")) {
            return super.open(resourceName);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Skipping resource: {}", resourceName);
            }
            return null ;
        }
    }

}
