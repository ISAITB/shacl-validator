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

package eu.europa.ec.itb.shacl.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The validator's output providing the produced SHACL validation report.
 */
@Schema(description = "The content and metadata linked to the validation report that corresponds to a provided RDF input.")
public class Output {

    @Schema(description = "The RDF validation report, provided as a BASE64 encoded String.")
    private String report;
    @Schema(description = "The mime type for the validation report as defined by the corresponding Input.reportSyntax property (or the applied default if missing).")
    private String reportSyntax;

    /**
     * @return The RDF validation report, provided as a BASE64 encoded String.
     */
    public String getReport() {
        return report;
    }

    /**
     * @param report The RDF validation report, provided as a BASE64 encoded String.
     */
    public void setReport(String report) {
        this.report = report;
    }

    /**
     * @return The mime type for the validation report as defined by the corresponding Input.reportSyntax property (or
     * the applied default if missing).
     */
    public String getReportSyntax() {
        return reportSyntax;
    }

    /**
     * @param reportSyntax The mime type for the validation report as defined by the corresponding Input.reportSyntax
     *                     property (or the applied default if missing).
     */
    public void setReportSyntax(String reportSyntax) {
        this.reportSyntax = reportSyntax;
    }
}
