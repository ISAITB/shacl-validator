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

import com.gitb.core.AnyContent;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.ValidationConstants;
import eu.europa.ec.itb.validation.commons.BaseInputHelper;
import eu.europa.ec.itb.validation.commons.FileContent;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

/**
 * Component to validate and parse user-provided input parameters.
 */
@Component
public class InputHelper extends BaseInputHelper<ApplicationConfig, FileManager, DomainConfig> {

    /**
     * Populate a file's content type from the provided input.
     *
     * @param fileContent The file's information.
     * @param inputItem The input to process.
     * @see BaseInputHelper#populateFileContentFromInput(FileContent, AnyContent)
     */
    @Override
    public void populateFileContentFromInput(FileContent fileContent, AnyContent inputItem) {
        if (Strings.CS.equals(inputItem.getName(), ValidationConstants.INPUT_RULE_SYNTAX)) {
            fileContent.setContentType(inputItem.getValue());
        }
    }

    /**
     * Validate and return the flag determining whether OWL imports should be loaded from the provided content.
     *
     * @param domainConfig The domain configuration.
     * @param userProvidedFlag The user-provided value.
     * @param validationType The requested validation type.
     * @return The flag to consider.
     * @throws ValidatorException If the provided input value is invalid.
     */
    public Boolean validateLoadInputs(DomainConfig domainConfig, Boolean userProvidedFlag, String validationType) {
        ExternalArtifactSupport inputLoadImportsType = domainConfig.getUserInputForLoadImportsType().get(validationType);
        boolean loadImportsType = domainConfig.getDefaultLoadImportsType().get(validationType);

        if (inputLoadImportsType == ExternalArtifactSupport.REQUIRED && userProvidedFlag==null) {
            throw new ValidatorException("validator.label.exception.validationTypeExpectsLoadImports", validationType, ValidationConstants.INPUT_LOAD_IMPORTS);
        }
        if (inputLoadImportsType == ExternalArtifactSupport.NONE && userProvidedFlag!=null) {
            throw new ValidatorException("validator.label.exception.validationTypeDoesNotExpectLoadImports", validationType, ValidationConstants.INPUT_LOAD_IMPORTS);
        }
        if ((inputLoadImportsType == ExternalArtifactSupport.OPTIONAL || inputLoadImportsType == ExternalArtifactSupport.NONE) && userProvidedFlag==null) {
            userProvidedFlag = loadImportsType;
        }
        return userProvidedFlag;
    }

    /**
     * Validate and return the SPARQL query configuration to use in the validation call.
     *
     * @param domainConfig The domain configuration.
     * @param inputConfig The provided configuration.
     * @return The configuration to use.
     * @throws ValidatorException If the provided configuration is invalid.
     */
    public SparqlQueryConfig validateSparqlConfiguration(DomainConfig domainConfig, SparqlQueryConfig inputConfig) {
        if (!domainConfig.isSupportsQueries()) {
            throw new ValidatorException("validator.label.exception.queriesNotSupported");
        }
        SparqlQueryConfig config = new SparqlQueryConfig();
        // Endpoint
        if (StringUtils.isEmpty(domainConfig.getQueryEndpoint())) {
            if (StringUtils.isEmpty(inputConfig.getEndpoint())) {
                throw new ValidatorException("validator.label.exception.sparqlEndpointNeeded");
            } else {
                config.setEndpoint(inputConfig.getEndpoint());
            }
        } else {
            if (!StringUtils.isEmpty(inputConfig.getEndpoint())) {
                throw new ValidatorException("validator.label.exception.ownSparqlEndpointNotAllowed");
            } else {
                config.setEndpoint(domainConfig.getQueryEndpoint());
            }
        }
        // Credentials
        if (StringUtils.isNotBlank(inputConfig.getUsername()) || StringUtils.isNotBlank((inputConfig.getPassword()))) {
            // Provided.
            if (domainConfig.getQueryAuthentication() == ExternalArtifactSupport.NONE) {
                throw new ValidatorException("validator.label.exception.sparqlCredentialsNotAllowed");
            }
            config.setUsername(inputConfig.getUsername());
            config.setPassword(inputConfig.getPassword());
        } else {
            // Not provided.
            if (domainConfig.getQueryAuthentication() == ExternalArtifactSupport.REQUIRED) {
                throw new ValidatorException("validator.label.exception.sparqlCredentialsRequired");
            }
            config.setUsername(domainConfig.getQueryUsername());
            config.setPassword(domainConfig.getQueryPassword());
        }
        // Query result content type
        if (StringUtils.isNotBlank(inputConfig.getPreferredContentType())) {
            config.setPreferredContentType(inputConfig.getPreferredContentType());
        } else {
            config.setPreferredContentType(domainConfig.getQueryContentType());
        }
        // Query
        if (StringUtils.isBlank(inputConfig.getQuery())) {
            throw new ValidatorException("validator.label.exception.sparqlQueryExpected");
        } else {
            config.setQuery(inputConfig.getQuery());
        }
        return config;
    }
}
