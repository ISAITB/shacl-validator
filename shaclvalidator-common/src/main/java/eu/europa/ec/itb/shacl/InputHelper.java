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

package eu.europa.ec.itb.shacl;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.vs.ValidateRequest;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.ValidationConstants;
import eu.europa.ec.itb.validation.commons.BaseInputHelper;
import eu.europa.ec.itb.validation.commons.FileContent;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        ExternalArtifactSupport flagExpectation = domainConfig.getUserInputForLoadImportsType().get(validationType);
        boolean defaultSetting = domainConfig.getDefaultLoadImportsType().get(validationType);

        if (flagExpectation == ExternalArtifactSupport.REQUIRED && userProvidedFlag == null) {
            throw new ValidatorException("validator.label.exception.validationTypeExpectsLoadImports", validationType, ValidationConstants.INPUT_LOAD_IMPORTS);
        }
        if (flagExpectation == ExternalArtifactSupport.NONE && userProvidedFlag != null) {
            throw new ValidatorException("validator.label.exception.validationTypeDoesNotExpectLoadImports", validationType, ValidationConstants.INPUT_LOAD_IMPORTS);
        }
        if ((flagExpectation == ExternalArtifactSupport.OPTIONAL || flagExpectation == ExternalArtifactSupport.NONE) && userProvidedFlag==null) {
            userProvidedFlag = defaultSetting;
        }
        return userProvidedFlag;
    }

    /**
     * Validate and return the flag determining whether the shape model should be merged with the input before validation..
     *
     * @param domainConfig The domain configuration.
     * @param userProvidedFlag The user-provided value.
     * @param validationType The requested validation type.
     * @return The flag to consider.
     * @throws ValidatorException If the provided input value is invalid.
     */
    public Boolean validateMergeModelsBeforeValidation(DomainConfig domainConfig, Boolean userProvidedFlag, String validationType) {
        ExternalArtifactSupport flagExpectation = domainConfig.getUserInputForMergeModelsType().get(validationType);
        boolean defaultSetting = domainConfig.getDefaultMergeModelsType().get(validationType);

        if (flagExpectation == ExternalArtifactSupport.REQUIRED && userProvidedFlag == null) {
            throw new ValidatorException("validator.label.exception.validationTypeExpectsMergeModelsBeforeValidation", validationType, ValidationConstants.INPUT_MERGE_MODELS_BEFORE_VALIDATION);
        }
        if (flagExpectation == ExternalArtifactSupport.NONE && userProvidedFlag != null) {
            throw new ValidatorException("validator.label.exception.validationTypeDoesNotExpectMergeModelsBeforeValidation", validationType, ValidationConstants.INPUT_MERGE_MODELS_BEFORE_VALIDATION);
        }
        if ((flagExpectation == ExternalArtifactSupport.OPTIONAL || flagExpectation == ExternalArtifactSupport.NONE) && userProvidedFlag == null) {
            userProvidedFlag = defaultSetting;
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

    /**
     * Align a list of values (e.g. content syntaxes or embedding methods) against the number of content items
     * provided for validation.
     * <p>
     * The provided values may be empty (in which case a null is used for every content item), have a single entry
     * (applied to every content item), or match the number of content items exactly (applied positionally). Any
     * other case is considered invalid.
     *
     * @param expectedCount The number of content items to align against.
     * @param values The values to align (may be null or empty).
     * @param valueInputName The name of the input that provided the values to align (used for error reporting).
     * @param contentInputName The name of the content input (used for error reporting).
     * @param <T> The type of value being aligned.
     * @return The aligned list of values (of size expectedCount).
     * @throws ValidatorException If the provided values cannot be aligned against the content items.
     */
    private <T> List<T> alignInputList(int expectedCount, List<T> values, String valueInputName, String contentInputName) {
        if (values == null || values.isEmpty()) {
            return Collections.nCopies(expectedCount, null);
        } else if (values.size() == 1) {
            return Collections.nCopies(expectedCount, values.get(0));
        } else if (values.size() == expectedCount) {
            return values;
        } else {
            throw new ValidatorException("validator.label.exception.inputCountMismatch", valueInputName, values.size(), contentInputName, expectedCount);
        }
    }

    /**
     * Validate and store the provided (one or more) content items to validate.
     * <p>
     * The provided content syntaxes and embedding methods may each be omitted, provided once (applied to every
     * content item) or provided once per content item (applied positionally).
     *
     * @param contents The content(s) to validate (string as-is, URL or base64 content).
     * @param syntaxes The content syntax(es) (mime types) to consider.
     * @param embeddingMethods The embedding method(s) to consider.
     * @param parentFolder The folder within which to create the resulting file(s).
     * @param httpVersion The HTTP version to use.
     * @return The information on each stored file to validate (in the same order as the provided content).
     */
    public List<FileInfo> validateContentsToValidate(List<String> contents, List<String> syntaxes, List<String> embeddingMethods, File parentFolder, HttpClient.Version httpVersion) {
        if (contents == null || contents.isEmpty()) {
            throw new ValidatorException("validator.label.exception.noContentProvided", ValidationConstants.INPUT_CONTENT);
        }
        List<String> alignedSyntaxes = alignInputList(contents.size(), syntaxes, ValidationConstants.INPUT_SYNTAX, ValidationConstants.INPUT_CONTENT);
        List<String> alignedEmbeddingMethods = alignInputList(contents.size(), embeddingMethods, ValidationConstants.INPUT_EMBEDDING_METHOD, ValidationConstants.INPUT_CONTENT);
        List<FileInfo> result = new ArrayList<>(contents.size());
        for (int i = 0; i < contents.size(); i++) {
            ValueEmbeddingEnumeration embeddingMethod = getEmbeddingMethod(alignedEmbeddingMethods.get(i));
            result.add(validateContentToValidate(contents.get(i), embeddingMethod, alignedSyntaxes.get(i), parentFolder, httpVersion));
        }
        return result;
    }

    /**
     * Flatten the SOAP inputs matching the provided name, expanding any multi-value ("item") inputs to their
     * individual leaf values.
     *
     * @param validateRequest The SOAP request.
     * @param inputName The name of the input to look for.
     * @return The flattened list of leaf inputs (never null).
     */
    private List<AnyContent> flattenInputs(ValidateRequest validateRequest, String inputName) {
        List<AnyContent> result = new ArrayList<>();
        for (AnyContent content: Utils.getInputFor(validateRequest, inputName)) {
            if (content.getItem() != null && !content.getItem().isEmpty()) {
                result.addAll(content.getItem());
            } else {
                result.add(content);
            }
        }
        return result;
    }

    /**
     * Extract the content syntax value(s) provided for the given SOAP input.
     *
     * @param validateRequest The SOAP request.
     * @param inputName The name of the input to look for.
     * @return The provided values (never null, possibly empty).
     */
    public List<String> validateContentSyntaxes(ValidateRequest validateRequest, String inputName) {
        return flattenInputs(validateRequest, inputName).stream().map(AnyContent::getValue).toList();
    }

    /**
     * Extract the embedding method value(s) explicitly provided for the given SOAP input.
     *
     * @param validateRequest The SOAP request.
     * @param inputName The name of the input to look for.
     * @return The provided values (never null, possibly empty).
     */
    public List<ValueEmbeddingEnumeration> validateContentEmbeddingMethods(ValidateRequest validateRequest, String inputName) {
        return flattenInputs(validateRequest, inputName).stream().map(content -> getEmbeddingMethod(content.getValue())).toList();
    }

    /**
     * Validate and store the provided (one or more) content items to validate, as received through the SOAP API.
     *
     * @param validateRequest The SOAP request.
     * @param inputName The name of the input for the content to validate.
     * @param explicitEmbeddingMethods The embedding method(s) provided as an explicit choice (may be empty).
     * @param contentSyntaxes The content syntax(es) provided as an explicit choice (may be empty).
     * @param parentFolder The folder within which to create the resulting file(s).
     * @param httpVersion The HTTP version to use.
     * @return The information on each stored file to validate (in the same order as the provided content).
     */
    public List<FileInfo> validateContentsToValidate(ValidateRequest validateRequest, String inputName,
            List<ValueEmbeddingEnumeration> explicitEmbeddingMethods, List<String> contentSyntaxes,
            File parentFolder, HttpClient.Version httpVersion) {
        List<AnyContent> leaves = flattenInputs(validateRequest, inputName);
        if (leaves.isEmpty()) {
            throw new ValidatorException("validator.label.exception.noContentProvided", inputName);
        }
        List<String> alignedSyntaxes = alignInputList(leaves.size(), contentSyntaxes, ValidationConstants.INPUT_SYNTAX, inputName);
        List<ValueEmbeddingEnumeration> alignedEmbeddingMethods = alignInputList(leaves.size(), explicitEmbeddingMethods, ValidationConstants.INPUT_EMBEDDING_METHOD, inputName);
        List<FileInfo> result = new ArrayList<>(leaves.size());
        for (int i = 0; i < leaves.size(); i++) {
            AnyContent leaf = leaves.get(i);
            ValueEmbeddingEnumeration explicitEmbeddingMethod = alignedEmbeddingMethods.get(i);
            if (explicitEmbeddingMethod == null) {
                explicitEmbeddingMethod = leaf.getEmbeddingMethod();
            }
            if (explicitEmbeddingMethod == null) {
                // Embedding method not provided as input nor as attribute.
                throw new ValidatorException("validator.label.exception.embeddingMethodEitherAsInputOrAttribute", inputName);
            }
            String valueToProcess = leaf.getValue();
            if (leaf.getEmbeddingMethod() == ValueEmbeddingEnumeration.BASE_64 && explicitEmbeddingMethod != ValueEmbeddingEnumeration.BASE_64) {
                // This is a URI or a plain text string encoded as BASE64.
                valueToProcess = new String(Utils.decodeBase64String(valueToProcess, true));
            }
            result.add(validateContentToValidate(valueToProcess, explicitEmbeddingMethod, alignedSyntaxes.get(i), parentFolder, httpVersion));
        }
        return result;
    }
}
