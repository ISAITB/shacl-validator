package eu.europa.ec.itb.shacl.standalone;

import eu.europa.ec.itb.validation.commons.jar.CommandLineValidator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;

/**
 * Application entry point when running the validator as a command-line tool.
 */
@SpringBootApplication
@ComponentScan("eu.europa.ec.itb")
public class Application {

    /**
     * Main method.
     *
     * @param args The command line arguments.
     * @throws IOException If an error occurs reading inputs or writing reports.
     */
    public static void main(String[] args) throws IOException {
        new CommandLineValidator().start(Application.class, args, "shaclvalidator");
    }

}
