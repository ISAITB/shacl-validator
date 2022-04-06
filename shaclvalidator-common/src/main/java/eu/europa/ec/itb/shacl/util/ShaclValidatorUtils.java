package eu.europa.ec.itb.shacl.util;

import eu.europa.ec.itb.shacl.validation.ReportSpecs;
import eu.europa.ec.itb.shacl.validation.SHACLReportHandler;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.ReportPair;

/**
 * Class with utility methods.
 */
public class ShaclValidatorUtils {

    /**
     * Constructor to prevent instantiation.
     */
    private ShaclValidatorUtils() { throw new IllegalStateException("Utility class"); }

    /**
     * Create a TAR validation report using the provided specifications.
     *
     * @param reportSpecs The report generation specifications.
     * @return The TAR validation report pair (detailed and aggregate).
     */
    public static ReportPair getTAR(ReportSpecs reportSpecs) {
        SHACLReportHandler reportHandler = new SHACLReportHandler(reportSpecs);
        return reportHandler.createReport();
    }

    /**
     * Get the labels to use for the SHACL validation report's fixed texts.
     *
     * @param localisationHelper The localisation helper.
     * @return The labels.
     */
    public static SHACLReportHandler.ReportLabels getReportLabels(LocalisationHelper localisationHelper) {
        var labels = new SHACLReportHandler.ReportLabels();
        labels.setFocusNode(localisationHelper.localise("validator.label.reportItemFocusNode"));
        labels.setResultPath(localisationHelper.localise("validator.label.reportItemResultPath"));
        labels.setShape(localisationHelper.localise("validator.label.reportItemShape"));
        labels.setValue(localisationHelper.localise("validator.label.reportItemValue"));
        return labels;
    }

}
