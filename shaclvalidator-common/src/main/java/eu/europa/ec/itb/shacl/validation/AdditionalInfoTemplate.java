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

package eu.europa.ec.itb.shacl.validation;

import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Class to hold a parsed additional info template.
 */
class AdditionalInfoTemplate {

    private static final String ADDITIONAL_INFO_TEMPLATE_PROPERTY_PREFIX = "validator.reportItemAdditionalInfoTemplate";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(\\$\\{[\\S]+})");

    private final TemplateDefinition defaultDefinition;
    private final Map<String, TemplateDefinition> typeToTemplateMap;
    private final Map<String, Property> propertyCache = new HashMap<>();
    private final Model inputModel;
    private final boolean enabled;

    /**
     * Constructor.
     *
     * @param localiser The localiser to look up template expressions.
     * @param inputModel The model to make property evaluations on.
     */
    AdditionalInfoTemplate(LocalisationHelper localiser, Model inputModel) {
        this.inputModel = inputModel;
        // Parse the default template (if any)
        if (localiser.propertyExists(ADDITIONAL_INFO_TEMPLATE_PROPERTY_PREFIX)) {
            defaultDefinition = parseTemplateDefinition(localiser.localise(ADDITIONAL_INFO_TEMPLATE_PROPERTY_PREFIX));
        } else {
            defaultDefinition = null;
        }
        // Parse URI-specific templates (if any).
        typeToTemplateMap = new HashMap<>();
        int templateCounter = 0;
        var continueCheck = true;
        do {
            var uriProperty = String.format("%s.%s.type", ADDITIONAL_INFO_TEMPLATE_PROPERTY_PREFIX, templateCounter);
            var templateProperty = String.format("%s.%s.template", ADDITIONAL_INFO_TEMPLATE_PROPERTY_PREFIX, templateCounter);
            continueCheck = localiser.propertyExists(uriProperty) && localiser.propertyExists(templateProperty);
            if (continueCheck) {
                templateCounter += 1;
                typeToTemplateMap.put(localiser.localise(uriProperty), parseTemplateDefinition(localiser.localise(templateProperty)));
            }
        } while (continueCheck);
        // Determine if additional info is supported overall.
        enabled = defaultDefinition != null || !typeToTemplateMap.isEmpty();
    }

    /**
     * Parse a template definition form a property value.
     *
     * @param templateWithPlaceholders The property value to parse.
     * @return The definition.
     */
    private TemplateDefinition parseTemplateDefinition(String templateWithPlaceholders) {
        var matcher = PLACEHOLDER_PATTERN.matcher(templateWithPlaceholders);
        var templateProperties = matcher.results().map(match -> {
            var propertyExpression = match.group();
            return propertyExpression.substring(propertyExpression.indexOf("${") + 2, propertyExpression.lastIndexOf('}'));
        }).toArray(String[]::new);
        String localisedTemplate;
        if (templateProperties.length > 0) {
            localisedTemplate = matcher.replaceAll("%s");
        } else {
            localisedTemplate = templateWithPlaceholders;
        }
        return new TemplateDefinition(localisedTemplate, templateProperties);
    }

    /**
     * @return Whether additional information is overall enabled.
     */
    boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the additional info text for the given focus node.
     *
     * @param focusNodeURI The URI of the focus node.
     * @return The text to use or null if it should be skipped.
     */
    String apply(String focusNodeURI) {
        if (enabled) {
            var focusNode = inputModel.getResource(focusNodeURI);
            if (focusNode != null) {
                String focusNodeTypeValue = null;
                var focusNodeType = focusNode.getPropertyResourceValue(RDF.type);
                if (focusNodeType != null) {
                    focusNodeTypeValue = focusNodeType.toString();
                }
                if (focusNodeTypeValue != null && typeToTemplateMap.containsKey(focusNodeTypeValue)) {
                    return applyTemplateDefinition(focusNode, typeToTemplateMap.get(focusNodeTypeValue));
                } else if (defaultDefinition != null) {
                    return applyTemplateDefinition(focusNode, defaultDefinition);
                }
            }
        }
        return null;
    }

    /**
     * Apply the provided definition to the given focus node.
     *
     * @param focusNode The focus node.
     * @param definition The template definition.
     * @return The text to use.
     */
    private String applyTemplateDefinition(Resource focusNode, TemplateDefinition definition) {
        String[] templateProperties = definition.getTemplateProperties();
        Object[] propertyValues = new String[templateProperties.length];
        for (int i=0; i < templateProperties.length; i++) {
            String propertyValue = null;
            var propertyURI = templateProperties[i];
            var property = propertyCache.computeIfAbsent(propertyURI, key -> inputModel.createProperty(propertyURI));
            if (property != null) {
                var nodeProperty = focusNode.getProperty(property);
                if (nodeProperty != null) {
                    var nodePropertyObject = nodeProperty.getObject();
                    if (nodePropertyObject != null) {
                        propertyValue = nodePropertyObject.toString();
                    }
                }
            }
            propertyValues[i] = ((propertyValue == null)?"":propertyValue);
        }
        return String.format(definition.getLocalisedTemplate(), propertyValues);
    }

    /**
     * Class to hold a template and its properties.
     */
    private static class TemplateDefinition {

        private final String[] templateProperties;
        private final String localisedTemplate;

        /**
         * Constructor.
         *
         * @param localisedTemplate The localised template string.
         * @param templateProperties The properties to include in the template.
         */
        private TemplateDefinition(String localisedTemplate, String[] templateProperties) {
            this.localisedTemplate = localisedTemplate;
            this.templateProperties = templateProperties;
        }

        /**
         * @return The properties.
         */
        public String[] getTemplateProperties() {
            return templateProperties;
        }

        /**
         * @return The template.
         */
        public String getLocalisedTemplate() {
            return localisedTemplate;
        }
    }

}
