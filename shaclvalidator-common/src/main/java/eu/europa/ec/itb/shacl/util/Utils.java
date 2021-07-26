package eu.europa.ec.itb.shacl.util;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.validation.SHACLReportHandler;
import org.apache.jena.rdf.model.Model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Class with utility methods.
 */
public class Utils extends eu.europa.ec.itb.validation.commons.Utils {

    /**
     * Create a TAR validation report from the provided SHACL validation report.
     *
     * @param report The SHACL validation report.
     * @param domainConfig The domain configuration to consider.
     * @return The TAR validation report.
     */
    public static TAR getTAR(Model report, DomainConfig domainConfig) {
        SHACLReportHandler reportHandler = new SHACLReportHandler(report, domainConfig);
        return reportHandler.createReport();
    }

    /**
     * Create a TAR validation report from the provided SHACL validation report.
     *
     * @param report The SHACL validation report.
     * @param reportContentToInclude The SHACL validation report's content to add to the TAR report as context.
     * @param aggregatedShapes The SHACL shapes to add to the TAR report as context.
     * @param domainConfig The domain configuration to consider.
     * @return The TAR validation report.
     */
    public static TAR getTAR(Model report, String reportContentToInclude, Path inputFilePath, Model aggregatedShapes, DomainConfig domainConfig) {
        //SHACL report: from Model to TAR
        try {
            String contentToValidateString = null;
            if (inputFilePath != null) {
                contentToValidateString = new String(Files.readAllBytes(inputFilePath));
            }
            SHACLReportHandler reportHandler = new SHACLReportHandler(contentToValidateString, aggregatedShapes, report, reportContentToInclude, domainConfig);
            return reportHandler.createReport();
        } catch (IOException e) {
            throw new IllegalStateException("Error during the transformation of the report to TAR");
        }
    }

}
