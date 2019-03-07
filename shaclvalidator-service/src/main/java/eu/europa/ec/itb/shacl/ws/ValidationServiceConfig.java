package eu.europa.ec.itb.shacl.ws;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.ValidatorChannel;
import eu.europa.ec.itb.shacl.web.ShaclController;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

/**
 * Configuration class responsible for creating the Spring beans required by the service.
 */
@Configuration
public class ValidationServiceConfig {

    @Autowired
    private Bus cxfBus = null;
    
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DomainConfigCache domainConfigCache;

    /**
     * The CXF endpoint(s) that will serve service calls.
     */
    @PostConstruct
    public void publishValidationServices() {
    	for (DomainConfig domainConfig: domainConfigCache.getAllDomainConfigurations()) {
            if (domainConfig.getChannels().contains(ValidatorChannel.WEB_SERVICE)) {
            	EndpointImpl endpoint = new EndpointImpl(cxfBus, applicationContext.getBean(ShaclController.class, domainConfig));
            	endpoint.setEndpointName(new QName("http://www.gitb.com/vs/v1/", "ValidationServicePort"));
                endpoint.setServiceName(new QName("http://www.gitb.com/vs/v1/", "ValidationService"));
                endpoint.publish("/"+domainConfig.getDomain()+"/validation");
            }
    	}
    }

}
