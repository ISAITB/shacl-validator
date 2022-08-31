package eu.europa.ec.itb.shacl.config;

import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.DomainPluginConfigProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;


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
//        OntDocumentManager.getInstance().setReadFailureHandler(new CustomReadFailureHandler());
    }
}
