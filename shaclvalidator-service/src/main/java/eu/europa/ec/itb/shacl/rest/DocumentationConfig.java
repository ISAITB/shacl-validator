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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * Configuration for the validator's REST API documentation (based on Swagger and Hydra).
 */
@Configuration
public class DocumentationConfig {

    private static final Logger logger = LoggerFactory.getLogger(DocumentationConfig.class);

    @Value("${validator.docs.licence.description}")
    private String licenceDescription;
    @Value("${validator.docs.licence.url}")
    private String licenceUrl;
    @Value("${validator.docs.version}")
    private String restApiVersion;
    @Value("${validator.docs.title}")
    private String restApiTitle;
    @Value("${validator.docs.description}")
    private String restApiDescription;
    @Value("${validator.docs.host:\"localhost:8080\"}")
    private String restApiHost;
    @Value("${validator.docs.schemes:http}")
    private Set<String> schemes;
    @Value("${validator.hydraRootPath:null}")
    private String hydraRootPath;
    @Value("${server.servlet.context-path:null}")
    private String contextPath;

    @Autowired
    private DomainConfigCache domainConfigCache;
    @Autowired
    private FileManager fileManager;

    /**
     * @return The API documentation entry point bean.
     */
    @Bean
    public Docket api() {
        Docket docket =  new Docket(DocumentationType.SWAGGER_2)
                .select()
                    .apis(RequestHandlerSelectors
                    .basePackage("eu.europa.ec.itb.shacl.rest"))
                    .paths(PathSelectors.regex("/.*"))
                .build()
                .apiInfo(apiEndPointsInfo())
                .useDefaultResponseMessages(false)
                .host(restApiHost)
                .protocols(schemes);
        return docket;
    }

    /**
     * @return The service's API metadata.
     */
    private ApiInfo apiEndPointsInfo() {
        return new ApiInfoBuilder().title(restApiTitle)
            .description(restApiDescription)
            .license(licenceDescription)
            .licenseUrl(licenceUrl)
            .version(restApiVersion)
            .build();
    }

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
            logger.info("Generated hydra documentation files in ["+docsRoot.getAbsolutePath()+"]");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialise hydra documentation", e);
        }
    }
}
