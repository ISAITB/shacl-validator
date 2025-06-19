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

import eu.europa.ec.itb.validation.commons.web.KeyWithLabel;

import java.util.List;

/**
 * Class used to record the values for the result of a validation (through the UI). This is serialised to JSON as part of
 * the response.
 */
public class UploadResult extends eu.europa.ec.itb.validation.commons.web.dto.UploadResult<Translations> {

    private List<KeyWithLabel> contentSyntax;

    /**
     * @param contentSyntax The supported RDF content syntax values.
     */
    public void setContentSyntax(List<KeyWithLabel> contentSyntax) {
        this.contentSyntax = contentSyntax;
    }

    /**
     * @return The supported RDF content syntax values.
     */
    public List<KeyWithLabel> getContentSyntax() {
        return contentSyntax;
    }

}
