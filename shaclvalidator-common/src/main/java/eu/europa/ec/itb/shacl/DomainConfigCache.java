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

package eu.europa.ec.itb.shacl;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.config.ErrorResponseTypeEnum;
import eu.europa.ec.itb.validation.commons.config.ParseUtils;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import jakarta.annotation.PostConstruct;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static eu.europa.ec.itb.validation.commons.config.ParseUtils.*;

/**
 * Component to load, record and share the domain configurations.
 */
@Component
public class DomainConfigCache extends WebDomainConfigCache<DomainConfig> {

    private static final Logger LOG = LoggerFactory.getLogger(DomainConfigCache.class);

    @Autowired
    private ApplicationConfig appConfig = null;

    @Override
    @PostConstruct
    public void init() {
        super.init();
    }

    /**
     * Create a new and empty domain configuration object.
     *
     * @return The object.
     */
    @Override
    protected DomainConfig newDomainConfig() {
        return new DomainConfig();
    }

    /**
     * @see eu.europa.ec.itb.validation.commons.config.DomainConfigCache#getSupportedChannels()
     *
     * @return Form, SOAP and REST API.
     */
    @Override
    protected ValidatorChannel[] getSupportedChannels() {
        return new ValidatorChannel[] {ValidatorChannel.FORM, ValidatorChannel.SOAP_API, ValidatorChannel.REST_API};
    }

    /**
     * Extend the domain configuration loading with JSON-specific information.
     *
     * @param domainConfig The domain configuration to enrich.
     * @param config The configuration properties to consider.
     */
    @Override
    protected void addDomainConfiguration(DomainConfig domainConfig, Configuration config) {
        super.addDomainConfiguration(domainConfig, config);
        addValidationArtifactInfo("validator.shaclFile", "validator.externalShapes", null, domainConfig, config);
        domainConfig.setDefaultReportSyntax(config.getString("validator.defaultReportSyntax", appConfig.getDefaultReportSyntax()));
        domainConfig.setWebContentSyntax(Arrays.stream(StringUtils.split(config.getString("validator.contentSyntax", ""), ',')).map(String::trim).toList());
        domainConfig.setMergeModelsBeforeValidation(config.getBoolean("validator.mergeModelsBeforeValidation", true));
        // SPARQL query configuration - start
        domainConfig.setQueryEndpoint(config.getString("validator.queryEndpoint"));
        domainConfig.setQueryUsername(config.getString("validator.queryUsername"));
        domainConfig.setQueryPassword(config.getString("validator.queryPassword"));
        ExternalArtifactSupport queryAuthenticationInput;
        if (domainConfig.getQueryUsername() == null && domainConfig.getQueryPassword() == null) {
            // No predefined credentials
            queryAuthenticationInput = ExternalArtifactSupport.byName(config.getString("validator.queryAuthenticationInput", ExternalArtifactSupport.OPTIONAL.getName()));
        } else {
            // Predefined credentials.
            queryAuthenticationInput = ExternalArtifactSupport.NONE;
        }
        domainConfig.setQueryAuthentication(queryAuthenticationInput);
        domainConfig.setQueryContentType(config.getString("validator.queryPreferredContentType", appConfig.getQueryPreferredContentType()));
        boolean hasQueryConfiguration = (domainConfig.getQueryEndpoint() != null || domainConfig.getQueryUsername() != null || domainConfig.getQueryPassword() != null);
        // If not explicitly set, we allow queries if there are query-related configuration properties.
        domainConfig.setSupportsQueries(config.getBoolean("validator.supportsQueries", hasQueryConfiguration));
        // SPARQL query configuration - end
        domainConfig.setDefaultLoadImportsType(parseBooleanMap("validator.loadImports", config, domainConfig.getType(), config.getBoolean("validator.loadImports", false)));
        domainConfig.setUserInputForLoadImportsType(parseEnumMap("validator.input.loadImports", ExternalArtifactSupport.byName(config.getString("validator.input.loadImports", ExternalArtifactSupport.NONE.getName())), config, domainConfig.getType(), ExternalArtifactSupport::byName));
        domainConfig.setReturnMessagesForAllLocales(config.getBoolean("validator.returnMessagesForAllLocales", Boolean.FALSE));
        // Check how to react to owl:import failures - start
        var defaultResponseType = ErrorResponseTypeEnum.fromValue(config.getString("validator.owlImportErrors", "log"));
        domainConfig.setImportedShapeErrorResponse(ParseUtils.parseEnumMap("validator.owlImportErrors", defaultResponseType, config, domainConfig.getType(), ErrorResponseTypeEnum::fromValue));
        domainConfig.setUrisToIgnoreForImportErrors(new HashSet<>(Arrays.asList(StringUtils.split(config.getString("validator.owlImportErrorsIgnoredUris", ""), ","))));
        domainConfig.setUrisToSkipWhenImporting(new HashSet<>(Arrays.asList(StringUtils.split(config.getString("validator.owlImportSkippedUris", ""), ","))));
        // Check how to react to owl:import failures - end
        addMissingDefaultValues(domainConfig.getWebServiceDescription(), appConfig.getDefaultLabels());
        // Local mapping files for URIs used in owl:imports - start
        List<Pair<String, Path>> mappingList = ParseUtils.parseValueList("validator.owlImportMapping", config, (entry) -> {
            String uri = entry.get("uri");
            String file = entry.get("file");
            if (StringUtils.isNotBlank(uri) && StringUtils.isNotBlank(file)) {
                uri = uri.trim();
                if (!uri.toLowerCase().startsWith("http://") && !uri.toLowerCase().startsWith("https://")) {
                    throw new IllegalStateException("OWL import mapping for URI [%s] does not start with 'http://' or 'https://'".formatted(uri));
                }
                file = file.trim();
                Path resourcePath = Path.of(appConfig.getResourceRoot(), domainConfig.getDomain(), file);
                if (!Files.exists(resourcePath)) {
                    throw new IllegalStateException("OWL import mapping for URI [%s] points to a non-existent file [%s]".formatted(uri, file));
                }
                return Pair.of(uri, resourcePath);
            } else {
                throw new IllegalStateException("Invalid mappings for owl:import. Each element must include [uri] and [file] properties");
            }
        });
        Map<String, Path> mappingMap = new HashMap<>();
        mappingList.forEach(entry -> {
            if (mappingMap.containsKey(entry.getKey())) {
                throw new IllegalStateException("Invalid mappings for owl:import. URI [%s] defined multiple times".formatted(entry.getKey()));
            }
            mappingMap.put(entry.getKey(), entry.getValue());
        });
        domainConfig.setOwlImportMappings(mappingMap);
        // Local mapping files for URIs used in owl:imports - end
        // Preload imports and SHACL shape graphs - start
        Boolean defaultPreloadOwlImports = config.getBoolean("validator.preloadOwlImports", null);
        Map<String, Boolean> preloadImportsMap = parseBooleanMap("validator.preloadOwlImports", config, domainConfig.getType(), Objects.requireNonNullElse(defaultPreloadOwlImports, false));
        Boolean defaultShapePreloading = config.getBoolean("validator.preloadShapeGraph", null);
        Map<String, Boolean> preloadShapeGraphMap = parseBooleanMap("validator.preloadShapeGraph", config, domainConfig.getType(), (type) -> {
            if (domainConfig.canCacheShapes(type)) {
                // If we are preloading imports and have not explicitly disabled preloading of shapes, set based on preloading of imports.
                return Objects.requireNonNullElseGet(defaultShapePreloading, () -> preloadImportsMap.getOrDefault(type, false));
            } else {
                // It's not possible to preload shapes.
                if (Boolean.TRUE.equals(defaultShapePreloading)) {
                    LOG.info("Preloading of the shapes graph will be disabled for validation type [{}] because it uses user-provided or remote shapes", type);
                }
                return false;
            }
        });
        // Post-process the validation types for which shape preloading is activated.
        preloadShapeGraphMap.entrySet().stream().filter(Map.Entry::getValue).forEach(entry -> {
            if (domainConfig.canCacheShapes(entry.getKey())) {
                // Shape preloading is enabled for the validation type. Force that imports are also preloaded.
                if (!preloadImportsMap.get(entry.getKey())) {
                    if (Boolean.FALSE.equals(defaultPreloadOwlImports)) {
                        // Preloading of imports was disabled as a default setting.
                        LOG.info("Validation type [{}] is set to preload the shapes graph. The preloading of owl:imports will be forced.", entry.getKey());
                    } else if (Boolean.FALSE.equals(config.getBoolean("validator.preloadOwlImports." + entry.getKey(), null))) {
                        // Warn because preloading of imports was explicitly disabled for the specific validation type.
                        LOG.warn("Validation type [{}] is set to preload the shapes graph. The preloading of owl:imports will be forced.", entry.getKey());
                    }
                    preloadImportsMap.put(entry.getKey(), true);
                }
            } else {
                // If shapes cannot be preloaded due to external or user-provided shapes force this (warning as at this step this was a specific setting for the validation type)..
                LOG.warn("Preloading of the shapes graph will be disabled for validation type [{}] because it uses user-provided or remote shapes", entry.getKey());
                entry.setValue(false);
            }
        });
        domainConfig.setPreloadImports(preloadImportsMap);
        domainConfig.setPreloadShapeGraph(preloadShapeGraphMap);
        // Preload imports and SHACL shape graphs - end
    }

}
