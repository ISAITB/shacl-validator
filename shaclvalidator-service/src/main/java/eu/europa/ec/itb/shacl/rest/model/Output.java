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
