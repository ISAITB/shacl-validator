/*
 * Copyright (C) 2026 European Union
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

package eu.europa.ec.itb.shacl.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum AdditionalInfoSource {

    INPUT("input"), SHAPES("shapes");

    private static final Logger LOG = LoggerFactory.getLogger(AdditionalInfoSource.class);
    private final String name;

    /**
     * @param name The enum instance's text value.
     */
    AdditionalInfoSource(String name) {
        this.name = name;
    }

    /**
     * @return The enum's textual representation.
     */
    public String getName() {
        return name;
    }

    /**
     * Determine the enum instance corresponding to the provided text.
     *
     * @param name The enum text.
     * @return The enum instance.
     * @throws IllegalArgumentException if the provided name was invalid.
     */
    public static AdditionalInfoSource byName(String name) {
        if (INPUT.name.equalsIgnoreCase(name)) {
            return INPUT;
        } else if (SHAPES.name.equalsIgnoreCase(name)) {
            return SHAPES;
        } else {
            LOG.warn("Unexpected source model type [{}] for additional report item information. Considering [{}] instead.", name, INPUT.name);
            return INPUT;
        }
    }
}
