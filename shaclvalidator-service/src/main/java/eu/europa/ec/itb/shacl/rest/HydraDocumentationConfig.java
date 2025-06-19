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

package eu.europa.ec.itb.shacl.rest;

import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.validation.FileManager;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Template;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the validator's REST API documentation (based on Hydra).
 */
@Configuration
public class HydraDocumentationConfig {

    private static final Logger logger = LoggerFactory.getLogger(HydraDocumentationConfig.class);

    @Value("${validator.hydraRootPath:null}")
    private String hydraRootPath;
    @Value("${server.servlet.context-path:null}")
    private String contextPath;

    @Autowired
    private DomainConfigCache domainConfigCache;
    @Autowired
    private FileManager fileManager;

    /**
     * Prepare the service's Hydra documentation on initialisation.
     */
    @PostConstruct
    public void generateHydraDocumentation() {
        try {
            File docsRoot = fileManager.getHydraDocsRootFolder();
            FileUtils.deleteQuietly(docsRoot);
            freemarker.template.Configuration templateConfig = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_28);
            templateConfig.setTemplateLoader(new ClassTemplateLoader(getClass(), "/hydra"));
            Template apiTemplate = templateConfig.getTemplate("api.jsonld");
            Template entryPointTemplate = templateConfig.getTemplate("EntryPoint.jsonld");
            Template vocabTemplate = templateConfig.getTemplate("vocab.jsonld");
            String hydraRootPathToUse;
            if (hydraRootPath != null && !hydraRootPath.equals("/")) {
                hydraRootPathToUse = hydraRootPath;
            } else if (contextPath != null && !contextPath.equals("/")) {
                hydraRootPathToUse = contextPath;
            } else {
                hydraRootPathToUse = "";
            }
            Map<String, Object> model = new HashMap<>();
            for (DomainConfig domainConfig: domainConfigCache.getAllDomainConfigurations()) {
                File domainRoot = fileManager.getHydraDocsFolder(domainConfig.getDomainName());
                domainRoot.mkdirs();
                model.put("domainName", domainConfig.getDomainName());
                model.put("rootPath", hydraRootPathToUse);
                apiTemplate.process(model, Files.newBufferedWriter(new File(domainRoot, apiTemplate.getName()).toPath()));
                entryPointTemplate.process(model, Files.newBufferedWriter(new File(domainRoot, entryPointTemplate.getName()).toPath()));
                vocabTemplate.process(model, Files.newBufferedWriter(new File(domainRoot, vocabTemplate.getName()).toPath()));
            }
            logger.info("Generated hydra documentation files in [{}]", docsRoot.getAbsolutePath());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialise hydra documentation", e);
        }
    }
}
