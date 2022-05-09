package eu.europa.ec.itb.shacl.upload;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;

/**
 * User interface translations specific to the RDF validator.
 */
public class Translations extends eu.europa.ec.itb.validation.commons.web.dto.Translations {

    private String downloadReportButton;
    private String downloadShapesButton;
    private String downloadInputButton;

    /**
     * Constructor.
     *
     * @param helper The localisation helper to use in looking up translations.
     * @param report The detailed TAR report.
     * @param domainConfig The domain configuration.
     */
    public Translations(LocalisationHelper helper, TAR report, DomainConfig domainConfig) {
        super(helper, report, domainConfig);
    }

    /**
     * @return The label value.
     */
    public String getDownloadInputButton() {
        return downloadInputButton;
    }

    /**
     * @param downloadInputButton The value to use for the label.
     */
    public void setDownloadInputButton(String downloadInputButton) {
        this.downloadInputButton = downloadInputButton;
    }

    /**
     * @return The label value.
     */
    public String getDownloadReportButton() {
        return downloadReportButton;
    }

    /**
     * @param downloadReportButton The value to use for the label.
     */
    public void setDownloadReportButton(String downloadReportButton) {
        this.downloadReportButton = downloadReportButton;
    }

    /**
     * @return The label value.
     */
    public String getDownloadShapesButton() {
        return downloadShapesButton;
    }

    /**
     * @param downloadShapesButton The value to use for the label.
     */
    public void setDownloadShapesButton(String downloadShapesButton) {
        this.downloadShapesButton = downloadShapesButton;
    }

}
