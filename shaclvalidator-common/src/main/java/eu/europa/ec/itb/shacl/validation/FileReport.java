package eu.europa.ec.itb.shacl.validation;

import com.gitb.tr.TAR;

/**
 * Created by simatosc on 12/08/2016.
 */
public class FileReport {

    private final String fileName;
    private final TAR report;
    private final boolean requireType;
    private final String validationType;

    public FileReport(String fileName, TAR report) {
        this(fileName, report, false, null);
    }

    public FileReport(String fileName, TAR report, boolean requireType, String type) {
        this.fileName = fileName;
        this.report = report;
        this.requireType = requireType;
        this.validationType = type;
    }

    public String getFileName() {
        return fileName;
    }

    public TAR getReport() {
        return report;
    }

    public String getReportXmlFileName() {
        return fileName.substring(0, fileName.lastIndexOf('.'))+".report.xml";
    }

    public String getReportPdfFileName() {
        return fileName.substring(0, fileName.lastIndexOf('.'))+".report.pdf";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Validation report summary:");
        if(requireType) {
        	sb.append("\n- Validation type: ").append(this.validationType);
        }
        sb.append("\n- Date: ").append(report.getDate());
        sb.append("\n- Result: ").append(report.getResult());
        sb.append("\n- Errors: ").append(report.getCounters().getNrOfErrors());
        sb.append("\n- Warnings: ").append(report.getCounters().getNrOfWarnings());
        sb.append("\n- Messages: ").append(report.getCounters().getNrOfAssertions());
        
        return sb.toString();
    }
}
