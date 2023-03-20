package eu.europa.ec.itb.shacl.validation;

import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.util.ShaclValidatorUtils;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import org.apache.jena.rdf.model.Model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Specifications to build TAR validation reports from the SHACL validation output.
 */
public class ReportSpecs {

    private Model inputModel;
    private Model reportModel;
    private String inputContentToInclude;
    private String reportContentToInclude;
    private Model shapesModel;
    private SHACLReportHandler.ReportLabels reportLabels;
    private LocalisationHelper localisationHelper;
    private boolean produceAggregateReport;
    private DomainConfig domainConfig;
    private String validationType;

    /**
     * Private as this is supposed to be constructed via its builder.
     */
    private ReportSpecs() {}

    /**
     * @return Whether the report items should be ordered.
     */
    public boolean isReportItemsOrdered() {
        return domainConfig.isReportsOrdered();
    }

    /**
     * @return The RDF model for the input that was validated.
     */
    public Model getInputModel() {
        return inputModel;
    }

    /**
     * @return The RDF model of the SHACL validation report.
     */
    public Model getReportModel() {
        return reportModel;
    }

    /**
     * @return The (optional) input content to include as context in the report.
     */
    public String getInputContentToInclude() {
        return inputContentToInclude;
    }

    /**
     * @return The (optional) SHACL validation report content to include as context in the report.
     */
    public String getReportContentToInclude() {
        return reportContentToInclude;
    }

    /**
     * @return The (optional) shapes RDF model to include.
     */
    public Model getShapesModel() {
        return shapesModel;
    }

    /**
     * @return The report labels to use.
     */
    public SHACLReportHandler.ReportLabels getReportLabels() {
        return reportLabels;
    }

    /**
     * @return The localisation helper to assist with translations.
     */
    public LocalisationHelper getLocalisationHelper() {
        return localisationHelper;
    }

    /**
     * @return Whether an aggregate report should be produced as well.
     */
    public boolean isProduceAggregateReport() {
        return produceAggregateReport;
    }

    /**
     * @return The relevant domain configuration.
     */
    public DomainConfig getDomainConfig() {
        return domainConfig;
    }

    /**
     * @return The requested validation type.
     */
    public String getValidationType() {
        return validationType;
    }

    /**
     * Builder to construct the report specification.
     *
     * @param inputModel The RDF model for the input that was validated.
     * @param reportModel The RDF model of the SHACL validation report.
     * @param localisationHelper The localisation helper to assist with translations.
     * @param domainConfig The domain configuration.
     * @param validationType The requested validation type.
     *
     * @return The builder to use.
     */
    public static Builder builder(Model inputModel, Model reportModel, LocalisationHelper localisationHelper, DomainConfig domainConfig, String validationType) {
        return new Builder(inputModel, reportModel, localisationHelper, domainConfig, validationType);
    }

    /**
     * Builder class to construct report specification objects.
     */
    public static class Builder {

        private final ReportSpecs instance;

        /**
         * Constructor.
         *
         * @param inputModel The RDF model for the input that was validated.
         * @param reportModel The RDF model of the SHACL validation report.
         * @param localisationHelper The localisation helper to assist with translations.
         * @param domainConfig The domain configuration.
         * @param validationType The requested validation type.
         */
        Builder(Model inputModel, Model reportModel, LocalisationHelper localisationHelper, DomainConfig domainConfig, String validationType) {
            instance = new ReportSpecs();
            instance.inputModel = inputModel;
            instance.reportModel = reportModel;
            instance.localisationHelper = localisationHelper;
            instance.reportLabels = ShaclValidatorUtils.getReportLabels(localisationHelper);
            instance.domainConfig = domainConfig;
            instance.validationType = validationType;
        }

        /**
         * Construct the report specifications from the collected preferences.
         *
         * @return The report specifications.
         */
        public ReportSpecs build() {
            return instance;
        }

        /**
         * Add the input content to add to the report.
         *
         * @param inputContentToInclude The input model content.
         * @return The builder.
         */
        public Builder withInputContentToInclude(Path inputContentToInclude) {
            try {
                return withInputContentToInclude(Files.readString(inputContentToInclude));
            } catch (IOException e) {
                throw new IllegalStateException("Error during the transformation of the report to TAR");
            }
        }

        /**
         * Add the input content to add to the report.
         *
         * @param inputContentToInclude The input model content.
         * @return The builder.
         */
        public Builder withInputContentToInclude(String inputContentToInclude) {
            instance.inputContentToInclude = inputContentToInclude;
            return this;
        }

        /**
         * Add the SHACL validation report content to add to the report.
         *
         * @param reportContentToInclude The report model content.
         * @return The builder.
         */
        public Builder withReportContentToInclude(String reportContentToInclude) {
            instance.reportContentToInclude = reportContentToInclude;
            return this;
        }

        /**
         * Add the shapes model to include as context in the report.
         *
         * @param shapesModel The shapes model to include.
         * @return The builder.
         */
        public Builder withShapesToInclude(Model shapesModel) {
            instance.shapesModel = shapesModel;
            return this;
        }

        /**
         * Generate also the aggregate report.
         *
         * @return The builder.
         */
        public Builder produceAggregateReport() {
            instance.produceAggregateReport = true;
            return this;
        }

    }

}
