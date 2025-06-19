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

package eu.europa.ec.itb.shacl.rest;

import org.apache.jena.rdf.model.Model;

import java.util.Optional;

/**
 * Class used to wrap all resources that drive and result from a validation.
 */
public class ValidationResources {

    private final String inputContent;
    private final Model input;
    private final Model report;
    private final Model shapes;
    private final String validationType;

    /**
     * Constructor.
     *
     * @param inputContent The provided input content (if needed).
     * @param input The final input model that was validated.
     * @param report The produced SHACL validation report.
     * @param shapes The SHACL shapes that were used.
     * @param validationType The applied validation type.
     */
    public ValidationResources(String inputContent, Model input, Model report, Model shapes, String validationType) {
        this.inputContent = inputContent;
        this.input = input;
        this.report = report;
        this.shapes = shapes;
        this.validationType = validationType;
    }

    /**
     * @return The provided input content (if needed).
     */
    public Optional<String> getInputContent() {
        return Optional.ofNullable(inputContent);
    }

    /**
     * @return The final input model that was validated.
     */
    public Model getInput() {
        return input;
    }

    /**
     * @return The produced SHACL validation report.
     */
    public Model getReport() {
        return report;
    }

    /**
     * @return The SHACL shapes that were used.
     */
    public Model getShapes() {
        return shapes;
    }

    /**
     * @return The applied validation type.
     */
    public String getValidationType() {
        return validationType;
    }
}
