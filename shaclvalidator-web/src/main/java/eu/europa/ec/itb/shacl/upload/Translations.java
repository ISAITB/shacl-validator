/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
    @Override
    public String getDownloadReportButton() {
        return downloadReportButton;
    }

    /**
     * @param downloadReportButton The value to use for the label.
     */
    @Override
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
