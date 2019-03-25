package eu.europa.ec.itb.shacl.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entry point to bootstrap the application.
 */
@SpringBootApplication
@ComponentScan(basePackages = "eu.europa.ec.itb.shacl") 
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
