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

package eu.europa.ec.itb.shacl.gitb;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

import eu.europa.ec.itb.shacl.ApplicationConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;

/**
 * Configuration class responsible for creating the Spring beans required by the SOAP API.
 */
@Configuration
public class ValidationServiceConfig {

    public static final String CXF_ROOT = "soap";

    @Autowired
    private Bus cxfBus;

    @Autowired
    private ApplicationConfig config;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DomainConfigCache domainConfigCache;

    /**
     * Create the CXF registration bean.
     *
     * @param context The application context.
     * @return The bean.
     */
    @Bean
    public ServletRegistrationBean<CXFServlet> servletRegistrationBean(ApplicationContext context) {
        ServletRegistrationBean<CXFServlet> srb = new ServletRegistrationBean<>(new CXFServlet(), "/"+ CXF_ROOT +"/*");
        srb.addInitParameter("hide-service-list-page", "true");
        return srb;
    }

    /**
     * Initialisation method to create a separate web service endpoint per configured domain.
     */
    @PostConstruct
    public void publishValidationServices() {
    	for (DomainConfig domainConfig: domainConfigCache.getAllDomainConfigurations()) {
            if (domainConfig.getChannels().contains(ValidatorChannel.SOAP_API)) {
                EndpointImpl endpoint = new EndpointImpl(cxfBus, applicationContext.getBean(ValidationServiceImpl.class, domainConfig));
                endpoint.setEndpointName(new QName("http://www.gitb.com/vs/v1/", "ValidationServicePort"));
                endpoint.setServiceName(new QName("http://www.gitb.com/vs/v1/", "ValidationService"));
                if (StringUtils.isNotBlank(config.getBaseSoapEndpointUrl())) {
                    var url = StringUtils.appendIfMissing(config.getBaseSoapEndpointUrl(), "/");
                    endpoint.setPublishedEndpointUrl(url+domainConfig.getDomainName()+"/validation");
                }
                endpoint.publish("/"+domainConfig.getDomainName()+"/validation");
            }
    	}
    }

}
