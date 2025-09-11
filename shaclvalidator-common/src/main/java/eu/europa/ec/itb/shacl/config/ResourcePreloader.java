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

package eu.europa.ec.itb.shacl.config;

import eu.europa.ec.itb.shacl.*;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.plugin.PluginManager;
import jakarta.annotation.PostConstruct;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.UUID;

import static eu.europa.ec.itb.shacl.DomainConfig.FILE_NAME_SHAPES;

/**
 * Configuration class to trigger the preloading of OWL import resources for the domains
 * where this is enabled.
 */
@Configuration
public class ResourcePreloader {

    private static final Logger LOG = LoggerFactory.getLogger(ResourcePreloader.class);

    @Autowired
    private ApplicationConfig appConfig = null;
    @Autowired
    private ApplicationContext ctx = null;
    @Autowired
    private DomainConfigCache domainConfigs = null;
    @Autowired
    private FileManager fileManager = null;
    @Autowired
    private PluginManager pluginManager = null;

    @PostConstruct
    public void initialise() {
        // Initialise plugins.
        if (pluginManager.hasPlugins()) {
            LOG.info("Initialised plugins");
        }
        // Preload OWL imports and shape graphs.
        preloadImportsAndShapeGraphs();
    }

    /**
     * Preload any imports and shape graphs that are configured as such.
     */
    private void preloadImportsAndShapeGraphs() {
        // All cases where we are preloading shape graphs will also have preloading of imports enabled (or forced).
        domainConfigs.getAllDomainConfigurations().stream().filter(DomainConfig::isPreloadingImportsForAnyType).forEach(domainConfig -> {
            LOG.info("Preloading owl:import references for domain [{}]", domainConfig.getDomainName());
            var localiser = new LocalisationHelper(domainConfig, Utils.getSupportedLocale(null, domainConfig));
            // Simulate a validation for each validation type with an empty model.
            String contentType = RDFLanguages.TURTLE.getContentType().getContentTypeStr();
            // Iterate over validation types.
            var modelManager = new ModelManager(fileManager);
            domainConfig.getType().stream().filter(domainConfig::preloadImportsForType).forEach(validationType -> {
                Path parentFolder = Path.of(appConfig.getTmpFolder(), UUID.randomUUID().toString());
                try {
                    Files.createDirectories(parentFolder);
                    Path inputFile = Files.createFile(parentFolder.resolve("emptyModel.ttl"));
                    Model emptyModel = ModelFactory.createDefaultModel();
                    try (var out = Files.newOutputStream(inputFile)) {
                        emptyModel.write(out, RDFLanguages.TURTLE.getName());
                        out.flush();
                    }
                    try {
                        LOG.info("Preloading owl:import references for validation type [{}]", validationType);
                        ValidationSpecs specs = ValidationSpecs.builder(inputFile.toFile(), validationType, contentType, Collections.emptyList(), false, false, domainConfig, localiser, modelManager)
                                .withoutPlugins()
                                .withoutProgressLogging()
                                .build();
                        SHACLValidator validator = ctx.getBean(SHACLValidator.class, specs);
                        validator.validateAll();
                        if (domainConfig.preloadShapeGraphForType(validationType)) {
                            LOG.info("Caching aggregated SHACL shapes for validation type [{}]", validationType);
                            String extension = fileManager.getFileExtension(domainConfig.getDefaultReportSyntax());
                            fileManager.writeShaclShapes(fileManager.createFile(parentFolder.toFile(), extension, FILE_NAME_SHAPES), validator.getAggregatedShapes(), validationType, domainConfig.getDefaultReportSyntax(), domainConfig);
                        }
                    } catch (Exception e) {
                        LOG.warn("Failure while preloading owl:import references for validation type [{}]", validationType, e);
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to preload OWL imports for domain [{}]", domainConfig.getDomainName(), e);
                } finally {
                    modelManager.close();
                    FileUtils.deleteQuietly(parentFolder.toFile());
                }
            });
        });
    }

}
