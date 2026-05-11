/*
 * Copyright (C) 2026 European Union
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
import org.apache.jena.riot.system.streammgr.LocatorFile;

import java.net.URI;
import java.nio.file.Path;

import static eu.europa.ec.itb.shacl.config.LocatorParams.PARAMS;

/**
 * Replacement of Jena's built-in {@link LocatorFile} that ensures resources are not loaded from locations that are
 * not permitted.
 */
public class CustomLocatorFile extends LocatorFile {

    /**
     * {@inheritDoc}
     */
    @Override
    public TypedInputStream open(String filenameIRI) {
        var params = PARAMS.get();
        if (params != null && params.fileAuthorizer() != null) {
            String fileName = toFileName(filenameIRI);
            if (fileName != null) {
                if (fileName.startsWith("file:")) {
                    params.fileAuthorizer().isPathAllowed(Path.of(URI.create(fileName)));
                } else {
                    params.fileAuthorizer().isPathAllowed(Path.of(fileName));
                }
            }
        }
        return super.open(filenameIRI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "CustomLocatorFile";
    }
}
