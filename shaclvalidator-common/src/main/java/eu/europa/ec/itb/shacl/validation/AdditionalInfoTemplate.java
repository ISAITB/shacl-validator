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
    private static final String ADDITIONAL_INFO_SOURCE_PROPERTY_PREFIX = "validator.reportItemAdditionalInfoTemplateSource";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(\\$\\{[\\S]+})");

    private final TemplateDefinition defaultDefinition;
    private final Map<String, TemplateDefinition> typeToTemplateMap;
    private final Map<String, Property> propertyCache = new HashMap<>();
    private final Model inputModel;
    private final Model shapesModel;
    private final boolean enabled;

    /**
     * Constructor.
     *
     * @param localiser The localiser to look up template expressions.
     * @param inputModel The input model to make property evaluations on (for input-sourced values).
     * @param shapesModel The shapes model to make property evaluations on (for input-sourced values).
     */
    AdditionalInfoTemplate(LocalisationHelper localiser, Model inputModel, Model shapesModel) {
        this.inputModel = inputModel;
        this.shapesModel = shapesModel;
        // Parse the default template (if any)
        AdditionalInfoSource defaultSource;
        if (localiser.propertyExists(ADDITIONAL_INFO_SOURCE_PROPERTY_PREFIX)) {
            defaultSource = AdditionalInfoSource.byName(localiser.localise(ADDITIONAL_INFO_SOURCE_PROPERTY_PREFIX));
        } else {
            defaultSource = AdditionalInfoSource.INPUT;
        }
        if (localiser.propertyExists(ADDITIONAL_INFO_TEMPLATE_PROPERTY_PREFIX)) {
            defaultDefinition = parseTemplateDefinition(localiser.localise(ADDITIONAL_INFO_TEMPLATE_PROPERTY_PREFIX), defaultSource);
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
                var sourceProperty = String.format("%s.%s.source", ADDITIONAL_INFO_TEMPLATE_PROPERTY_PREFIX, templateCounter);
                AdditionalInfoSource sourceForType;
                if (localiser.propertyExists(sourceProperty)) {
                    sourceForType = AdditionalInfoSource.byName(localiser.localise(sourceProperty));
                } else {
                    sourceForType = defaultSource;
                }
                typeToTemplateMap.put(localiser.localise(uriProperty), parseTemplateDefinition(localiser.localise(templateProperty), sourceForType));
                templateCounter += 1;
            }
        } while (continueCheck);
        // Determine if additional info is supported overall.
        enabled = defaultDefinition != null || !typeToTemplateMap.isEmpty();
    }

    /**
     * Parse a template definition form a property value.
     *
     * @param templateWithPlaceholders The property value to parse.
     * @param source type The source to retrieve values from.
     * @return The definition.
     */
    private TemplateDefinition parseTemplateDefinition(String templateWithPlaceholders, AdditionalInfoSource source) {
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
        return new TemplateDefinition(localisedTemplate, templateProperties, source);
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
     * @param shapeURI The URI of the fired shape.
     * @return The text to use or null if it should be skipped.
     */
    String apply(String focusNodeURI, String shapeURI) {
        if (enabled) {
            var focusNode = inputModel.getResource(focusNodeURI);
            if (focusNode != null) {
                String focusNodeTypeValue = null;
                var focusNodeType = focusNode.getPropertyResourceValue(RDF.type);
                if (focusNodeType != null) {
                    focusNodeTypeValue = focusNodeType.toString();
                }
                TemplateDefinition definitionToUse = null;
                if (focusNodeTypeValue != null && typeToTemplateMap.containsKey(focusNodeTypeValue)) {
                    definitionToUse = typeToTemplateMap.get(focusNodeTypeValue);
                } else if (defaultDefinition != null) {
                    definitionToUse = defaultDefinition;
                }
                if (definitionToUse != null) {
                    Resource sourceToUse;
                    Model sourceModel;
                    if (definitionToUse.source() == AdditionalInfoSource.INPUT) {
                        sourceToUse = focusNode;
                        sourceModel = inputModel;
                    } else {
                        sourceToUse = shapesModel.getResource(shapeURI);
                        sourceModel = shapesModel;
                    }
                    if (sourceToUse != null) {
                        return applyTemplateDefinition(sourceToUse, definitionToUse, sourceModel);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Apply the provided definition to the given resource.
     *
     * @param sourceResource The resource to use as the additional information source.
     * @param definition The template definition.
     * @param sourceModel The source model.
     * @return The text to use.
     */
    private String applyTemplateDefinition(Resource sourceResource, TemplateDefinition definition, Model sourceModel) {
        String[] templateProperties = definition.templateProperties();
        Object[] propertyValues = new String[templateProperties.length];
        for (int i=0; i < templateProperties.length; i++) {
            String propertyValue = null;
            var propertyURI = templateProperties[i];
            var property = propertyCache.computeIfAbsent(propertyURI, key -> sourceModel.createProperty(propertyURI));
            if (property != null) {
                var nodeProperty = sourceResource.getProperty(property);
                if (nodeProperty != null) {
                    var nodePropertyObject = nodeProperty.getObject();
                    if (nodePropertyObject != null) {
                        propertyValue = nodePropertyObject.toString();
                    }
                }
            }
            propertyValues[i] = ((propertyValue == null)?"":propertyValue);
        }
        return String.format(definition.localisedTemplate(), propertyValues);
    }

    /**
         * Class to hold a template and its properties.
         */
        private record TemplateDefinition(String localisedTemplate, String[] templateProperties, AdditionalInfoSource source) {

        /**
         * Constructor.
         *
         * @param localisedTemplate  The localised template string.
         * @param templateProperties The properties to include in the template.
         */
        private TemplateDefinition {
        }

            /**
             * @return The properties.
             */
            @Override
            public String[] templateProperties() {
                return templateProperties;
            }

            /**
             * @return The template.
             */
            @Override
            public String localisedTemplate() {
                return localisedTemplate;
            }
        }

}
