package eu.europa.ec.itb.shacl.util;

import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.ReportSpecs;
import eu.europa.ec.itb.shacl.validation.SHACLReportHandler;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.ReportPair;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;

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

    /**
     * Get the RDF validation report content to include in the resulting TAR report's context.
     *
     * @param reportModel The model of the RDF validation report.
     * @param rdfReportMimeType The mime type for the RDF report.
     * @param reportProcessingQuery The SPARQL query to use for post-processing of the SHACL validation report.
     * @param fileManager The file manager instance to use.
     * @return The content to inject in the TAR report.
     */
    public static String getRdfReportToIncludeInTAR(Model reportModel, String rdfReportMimeType, String reportProcessingQuery, FileManager fileManager) {
        Model reportToInclude = reportModel;
        if (reportProcessingQuery != null && !reportProcessingQuery.isBlank()) {
            Query query = QueryFactory.create(reportProcessingQuery);
            try (QueryExecution queryExecution = QueryExecutionFactory.create(query, reportToInclude)) {
                reportToInclude = queryExecution.execConstruct();
            }
        }
        return fileManager.writeRdfModelToString(reportToInclude, rdfReportMimeType);
    }

}
