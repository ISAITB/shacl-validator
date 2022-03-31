package eu.europa.ec.itb.shacl;

import com.gitb.tr.TAR;

/**
 * Wrapper class to hold the pair of a validation's TAR reports.
 */
public class ReportPair {

    private final TAR detailedReport;
    private final TAR aggregateReport;

    /**
     * Constructor.
     *
     * @param detailedReport The detailed TAR report.
     * @param aggregateReport The aggregate TAR report.
     */
    public ReportPair(TAR detailedReport, TAR aggregateReport) {
        this.detailedReport = detailedReport;
        this.aggregateReport = aggregateReport;
    }

    /**
     * @return The detailed TAR report.
     */
    public TAR getDetailedReport() {
        return detailedReport;
    }

    /**
     * @return The aggregate TAR report.
     */
    public TAR getAggregateReport() {
        return aggregateReport;
    }
}
