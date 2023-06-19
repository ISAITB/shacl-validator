package eu.europa.ec.itb.shacl.config;

import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.validation.CustomReadFailureHandler;
import eu.europa.ec.itb.validation.commons.config.DomainPluginConfigProvider;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.riot.adapters.AdapterFileManager;
import org.apache.jena.riot.system.stream.LocatorFTP;
import org.apache.jena.riot.system.stream.LocatorFile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;


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
        var fileManager = AdapterFileManager.get();
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
