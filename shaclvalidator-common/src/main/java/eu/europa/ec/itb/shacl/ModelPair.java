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

/**
 * Wrapper class to hold the pair of a validation's RDF models (input and report).
 */
public class ModelPair {

    private final Model inputModel;
    private final Model reportModel;

    /**
     * Constructor.
     *
     * @param inputModel The RDF model that was validated.
     * @param reportModel The model for the SHACL validation report.
     */
    public ModelPair(Model inputModel, Model reportModel) {
        this.inputModel = inputModel;
        this.reportModel = reportModel;
    }

    /**
     * @return The input model.
     */
    public Model getInputModel() {
        return inputModel;
    }

    /**
     * @return The report model.
     */
    public Model getReportModel() {
        return reportModel;
    }
}
