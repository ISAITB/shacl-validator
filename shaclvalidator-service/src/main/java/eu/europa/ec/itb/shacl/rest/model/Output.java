package eu.europa.ec.itb.shacl.rest.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "The content and metadata linked to the validation report that corresponds to a provided RDF input.")
public class Output {

    @ApiModelProperty(notes = "The RDF validation report, provided as a BASE64 encoded String.")
    private String report;
    @ApiModelProperty(notes = "The mime type for the validation report as defined by the corresponding Input.reportSyntax property (or the applied default if missing).")
    private String reportSyntax;

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public String getReportSyntax() {
        return reportSyntax;
    }

    public void setReportSyntax(String reportSyntax) {
        this.reportSyntax = reportSyntax;
    }
}
