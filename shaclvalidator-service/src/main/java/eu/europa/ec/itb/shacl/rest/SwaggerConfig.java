package eu.europa.ec.itb.shacl.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

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
    @Value("${validator.docs.host:null}")
    private String restApiHost;

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
                .protocols(new HashSet<>(Arrays.asList("http", "https")));
        return docket;
    }

    private ApiInfo apiEndPointsInfo() {
        return new ApiInfoBuilder().title(restApiTitle)
            .description(restApiDescription)
            .license(licenceDescription)
            .licenseUrl(licenceUrl)
            .version(restApiVersion)
            .build();
    }
}
