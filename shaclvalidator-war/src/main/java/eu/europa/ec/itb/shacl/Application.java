package eu.europa.ec.itb.shacl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point to bootstrap the application.
 */
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class, FreeMarkerAutoConfiguration.class, GsonAutoConfiguration.class})
@EnableScheduling
@ComponentScan("eu.europa.ec.itb")
@EnableAspectJAutoProxy
public class Application {

    /**
     * Main method.
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
