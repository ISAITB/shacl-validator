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

import eu.europa.ec.itb.shacl.CustomJenaFileManager;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.validation.CustomReadFailureHandler;
import eu.europa.ec.itb.validation.commons.config.DomainPluginConfigProvider;
import jakarta.annotation.PostConstruct;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.riot.system.stream.LocatorFTP;
import org.apache.jena.riot.system.stream.LocatorFile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Configure common spring beans.
 */
@Configuration
public class BeanConfiguration {

    /**
     * Support the definition of plugins.
     *
     * @return The default plugin provider.
     */
    @Bean
    public DomainPluginConfigProvider<DomainConfig> pluginConfigProvider() {
        return new DomainPluginConfigProvider<>();
    }

    @PostConstruct
    public void initialise() {
        // Setup FileManager.
        var fileManager = CustomJenaFileManager.get();
        fileManager.getStreamManager().clearLocators();
        fileManager.getStreamManager().addLocator(new LocatorFile()) ;
        fileManager.getStreamManager().addLocator(new CustomLocatorHTTP()) ;
        fileManager.getStreamManager().addLocator(new LocatorFTP()) ;
        fileManager.getStreamManager().addLocator(new CustomClassLoaderLocator(fileManager.getStreamManager().getClass().getClassLoader())) ;
        fileManager.setModelCaching(true);
        // Setup OntDocumentManager.
        var ontDocumentManager = OntDocumentManager.getInstance();
        ontDocumentManager.setFileManager(fileManager);
        ontDocumentManager.setReadFailureHandler(new CustomReadFailureHandler());
    }

}
