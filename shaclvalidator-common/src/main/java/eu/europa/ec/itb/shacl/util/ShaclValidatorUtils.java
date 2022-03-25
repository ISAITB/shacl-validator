package eu.europa.ec.itb.shacl.util;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.ModelPair;
import eu.europa.ec.itb.shacl.validation.SHACLReportHandler;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import org.apache.jena.rdf.model.Model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Class with utility methods.
 */
public class ShaclValidatorUtils {

    /**
     * Constructor to prevent instantiation.
     */
    private ShaclValidatorUtils() { throw new IllegalStateException("Utility class"); }

    /**
     * Create a TAR validation report from the provided SHACL validation report.
     *
     * @param models The RDF models for the input and report.
     * @param domainConfig The domain configuration to consider.
     * @param labels The labels to use for fixed texts.
     * @param localiser HElper class to lookup translations.
     * @return The TAR validation report.
     */
    public static TAR getTAR(ModelPair models, DomainConfig domainConfig, SHACLReportHandler.ReportLabels labels, LocalisationHelper localiser) {
        SHACLReportHandler reportHandler = new SHACLReportHandler(models, domainConfig, labels, localiser);
        return reportHandler.createReport();
    }

    /**
     * Create a TAR validation report from the provided SHACL validation report.
     *
     * @param models The models for the input and SHACL validation report.
     * @param reportContentToInclude The SHACL validation report's content to add to the TAR report as context.
     * @param inputFilePath The path to the input file.
     * @param aggregatedShapes The SHACL shapes to add to the TAR report as context.
     * @param domainConfig The domain configuration to consider.
     * @param labels The labels for fixed texts.
     * @param localiser Helper class to lookup translations.
     * @return The TAR validation report.
     */
    public static TAR getTAR(ModelPair models, String reportContentToInclude, Path inputFilePath, Model aggregatedShapes, DomainConfig domainConfig, SHACLReportHandler.ReportLabels labels, LocalisationHelper localiser) {
        //SHACL report: from Model to TAR
        try {
            String contentToValidateString = null;
            if (inputFilePath != null) {
                contentToValidateString = new String(Files.readAllBytes(inputFilePath));
            }
            SHACLReportHandler reportHandler = new SHACLReportHandler(contentToValidateString, aggregatedShapes, models, reportContentToInclude, domainConfig, labels, localiser);
            return reportHandler.createReport();
        } catch (IOException e) {
            throw new IllegalStateException("Error during the transformation of the report to TAR");
        }
    }

    /**
     * Get the default labels to use for SHACL validation reports when we do not support different locales.
     *
     * @param config The domain configuration.
     * @return The labels to consider.
     */
    public static SHACLReportHandler.ReportLabels getDefaultReportLabels(DomainConfig config) {
        return getReportLabels(new LocalisationHelper(config, Locale.ENGLISH));
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
