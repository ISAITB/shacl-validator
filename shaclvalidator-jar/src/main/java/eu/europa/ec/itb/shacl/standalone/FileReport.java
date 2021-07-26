package eu.europa.ec.itb.shacl.standalone;

import com.gitb.tr.TAR;

/**
 * Class used to summarise a TAR validation report.
 */
public class FileReport {

    private final String fileName;
    private final TAR report;
    private final boolean requireType;
    private final String validationType;

    /**
     * Constructor.
     *
     * @param fileName The report file name.
     * @param report The report contents.
     */
    public FileReport(String fileName, TAR report) {
        this(fileName, report, false, null);
    }

    /**
     * Constructor.
     *
     * @param fileName The report file name.
     * @param report The report contents.
     * @param requireType True to include the validation type in messages.
     * @param type The validation type.
     */
    public FileReport(String fileName, TAR report, boolean requireType, String type) {
        this.fileName = fileName;
        this.report = report;
        this.requireType = requireType;
        this.validationType = type;
    }

    /**
     * Convert the provided report to a command-line message.
     *
     * @return The text.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Validation report summary [").append(this.fileName).append("]:");
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
