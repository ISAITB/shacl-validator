package eu.europa.ec.itb.shacl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point to bootstrap the application.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan("eu.europa.ec.itb")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
