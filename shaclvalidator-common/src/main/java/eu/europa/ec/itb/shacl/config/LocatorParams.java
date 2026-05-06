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

import eu.europa.ec.itb.validation.commons.ImportedFileAuthorizer;
import eu.europa.ec.itb.validation.commons.ImportedUriAuthorizer;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Parameters defining how remote resources are to be handled.
 *
 * @param urisToSkip The URIs to skip looking.
 * @param httpVersion The HTTP version to use in the lookups.
 * @param uriMappings The local URI to path mappings to consider.
 * @param requestDecorator The request decorator to use for HTTP requests.
 * @param uriAuthorizer The authorizer for imported resource URIs.
 * @param fileAuthorizer The authorizer for imported resource files.
 */
public record LocatorParams(Set<String> urisToSkip, HttpClient.Version httpVersion, Map<String, Path> uriMappings, Consumer<HttpRequest.Builder> requestDecorator, ImportedUriAuthorizer uriAuthorizer, ImportedFileAuthorizer fileAuthorizer) {

    /**
     * ThreadLocal to access the locator parameters.
     */
    public static final ThreadLocal<LocatorParams> PARAMS = new ThreadLocal<>();

}
