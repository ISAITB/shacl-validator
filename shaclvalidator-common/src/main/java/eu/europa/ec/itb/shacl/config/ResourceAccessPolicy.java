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
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;

/**
 * The restrictions to apply when RDF content refers to other resources (e.g. a JSON-LD {@literal @context}).
 *
 * @param uriAuthorizer The authorizer for referenced HTTP(S) URIs (null if no restrictions apply).
 * @param fileAuthorizer The authorizer for referenced files (null if no file access is permitted at all).
 */
public record ResourceAccessPolicy(ImportedUriAuthorizer uriAuthorizer, ImportedFileAuthorizer fileAuthorizer) {

    /**
     * Policy for content provided by the user at request time (e.g. the content to validate), which is always untrusted.
     *
     * @param appConfig The application configuration.
     * @param domainConfig The domain configuration.
     * @return The policy.
     */
    public static ResourceAccessPolicy forUserContent(ApplicationConfig appConfig, DomainConfig domainConfig) {
        return new ResourceAccessPolicy(ImportedUriAuthorizer.forUserContent(appConfig), ImportedFileAuthorizer.from(appConfig, domainConfig));
    }

    /**
     * Policy for validation artefacts (i.e. SHACL shapes). Artefacts that are only configured by the validator's
     * administrator are trusted, whereas restrictions apply if users can provide artefacts for the validation type.
     *
     * @param appConfig The application configuration.
     * @param domainConfig The domain configuration.
     * @param validationType The validation type.
     * @return The policy.
     */
    public static ResourceAccessPolicy forValidationArtefacts(ApplicationConfig appConfig, DomainConfig domainConfig, String validationType) {
        return new ResourceAccessPolicy(ImportedUriAuthorizer.from(appConfig, domainConfig, validationType).orElse(null), ImportedFileAuthorizer.from(appConfig, domainConfig));
    }

}
