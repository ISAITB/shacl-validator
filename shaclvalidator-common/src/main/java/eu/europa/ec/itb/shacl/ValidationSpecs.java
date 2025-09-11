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

import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import org.apache.jena.rdf.model.Model;

import java.io.File;
import java.util.List;

/**
 * Record used to wrap the specifications with which to carry out a validation.
 *
 */
public class ValidationSpecs {

    private File inputFileToValidate;
    private String validationType;
    private String contentSyntax;
    private List<FileInfo> externalShaclFiles;
    private boolean loadImports;
    private boolean mergeModelsBeforeValidation;
    private DomainConfig domainConfig;
    private LocalisationHelper localiser;
    private boolean logProgress;
    private boolean usePlugins;
    private ModelManager modelManager;

    /**
     * Private constructor to prevent direct initialisation.
     */
    private ValidationSpecs() {}

    /**
     * @return The input RDF (or other) content to validate.
     */
    public File getInputFileToValidate() {
        return inputFileToValidate;
    }

    /**
     * @return The type of validation to perform.
     */
    public String getValidationType() {
        return validationType;
    }

    /**
     * @return The mime type of the provided RDF content.
     */
    public String getContentSyntax() {
        return contentSyntax;
    }

    /**
     * @return Any shapes to consider that are externally provided.
     */
    public List<FileInfo> getExternalShaclFiles() {
        return externalShaclFiles;
    }

    /**
     * @return True if OWL imports in the content should be loaded before validation.
     */
    public boolean isLoadImports() {
        return loadImports;
    }

    /**
     * @return True if the shape and input modems should be merged before validation.
     */
    public boolean isMergeModelsBeforeValidation() {
        return mergeModelsBeforeValidation;
    }

    /**
     * @return The domain in question.
     */
    public DomainConfig getDomainConfig() {
        return domainConfig;
    }

    /**
     * @return Helper class for localisations.
     */
    public LocalisationHelper getLocaliser() {
        return localiser;
    }

    /**
     * @return Whether validation progress should be logged.
     */
    public boolean isLogProgress() {
        return logProgress;
    }

    /**
     * @return Whether custom plugins should be considered.
     */
    public boolean isUsePlugins() {
        return usePlugins;
    }

    /**
     * Track the provided model.
     *
     * @param model The model to track.
     */
    public void track(Model model) {
        if (modelManager != null) modelManager.track(model);
    }

    /**
     * Build the validation specifications.
     *
     * @param inputFileToValidate The input RDF (or other) content to validate.
     * @param validationType The type of validation to perform.
     * @param contentSyntax The mime type of the provided RDF content.
     * @param externalShaclFiles Any shapes to consider that are externally provided
     * @param loadImports True if OWL imports in the content should be loaded before validation.
     * @param mergeModelsBeforeValidation True if the shape and input modems should be merged before validation.
     * @param domainConfig The domain in question.
     * @param localiser Helper class for localisations.
     * @param modelManager The model manager instance to use.
     * @return The specification builder.
     */
    public static Builder builder(File inputFileToValidate, String validationType, String contentSyntax, List<FileInfo> externalShaclFiles, boolean loadImports, boolean mergeModelsBeforeValidation, DomainConfig domainConfig, LocalisationHelper localiser, ModelManager modelManager) {
        if (validationType == null) {
            validationType = domainConfig.getType().get(0);
        }
        return new Builder(inputFileToValidate, validationType, contentSyntax, externalShaclFiles, loadImports, mergeModelsBeforeValidation, domainConfig, localiser, modelManager);
    }

    /**
     * Builder class used to incrementally create a specification instance.
     */
    public static class Builder {

        private final ValidationSpecs instance;

        /**
         * Constructor.
         *
         * @param inputFileToValidate The input RDF (or other) content to validate.
         * @param validationType      The type of validation to perform.
         * @param contentSyntax       The mime type of the provided RDF content.
         * @param externalShaclFiles  Any shapes to consider that are externally provided
         * @param loadImports         True if OWL imports in the content should be loaded before validation.
         * @param mergeModelsBeforeValidation True if the shape and input modems should be merged before validation.
         * @param domainConfig        The domain in question.
         * @param localiser           Helper class for localisations.
         * @param modelManager        The model manager instance to use.
         */
        Builder(File inputFileToValidate, String validationType, String contentSyntax, List<FileInfo> externalShaclFiles, boolean loadImports, boolean mergeModelsBeforeValidation, DomainConfig domainConfig, LocalisationHelper localiser, ModelManager modelManager) {
            instance = new ValidationSpecs();
            this.instance.contentSyntax = contentSyntax;
            this.instance.inputFileToValidate = inputFileToValidate;
            this.instance.domainConfig = domainConfig;
            this.instance.externalShaclFiles = externalShaclFiles;
            this.instance.loadImports = loadImports;
            this.instance.mergeModelsBeforeValidation = mergeModelsBeforeValidation;
            this.instance.localiser = localiser;
            this.instance.validationType = validationType;
            this.instance.logProgress = true;
            this.instance.usePlugins = true;
            this.instance.modelManager = modelManager;
        }

        /**
         * Skip progress logging.
         *
         * @return The builder instance.
         */
        public Builder withoutProgressLogging() {
            this.instance.logProgress = false;
            return this;
        }

        /**
         * Skip custom plugins.
         *
         * @return The builder instance.
         */
        public Builder withoutPlugins() {
            this.instance.usePlugins = false;
            return this;
        }

        /**
         * @return The specification instance to use.
         */
        public ValidationSpecs build() {
            return instance;
        }

    }

}
