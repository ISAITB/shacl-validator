package eu.europa.ec.itb.shacl.standalone;

import org.apache.commons.io.FileUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import eu.europa.ec.itb.shacl.ApplicationConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by mfontsan on 16/07/2019.
 */
@SpringBootApplication
@ComponentScan("eu.europa.ec.itb")
public class Application {

    public static void main(String[] args) throws IOException {
        System.out.print("Starting validator ...");
        File tempFolder = Files.createTempDirectory("shaclvalidator").toFile();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(tempFolder)));
        // Setup folders - start
        File resourceFolder = new File(tempFolder, "resources");
        File logFolder = new File(tempFolder, "logs");
        File workFolder = new File(tempFolder, "work");
        if (!resourceFolder.mkdirs() || !logFolder.mkdirs() || !workFolder.mkdirs()) {
            throw new IllegalStateException("Unable to create work directories under ["+tempFolder.getAbsolutePath()+"]");
        }
        // Set the resource root so that it can be used. This is done before app startup to avoid PostConstruct issues.
        String resourceRoot = resourceFolder.getAbsolutePath();
        if (!resourceRoot.endsWith(File.separator)) {
            resourceRoot += File.separator;
        }
        System.setProperty("LOG_PATH", logFolder.getAbsolutePath());
        System.setProperty("validator.tmpFolder", workFolder.getAbsolutePath());
        System.setProperty("validator.resourceRoot", resourceRoot);
        // Setup folders - end
        prepareConfigForStandalone(resourceFolder);
        // Start the application.
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
        // Post process config.
        ApplicationConfig config = ctx.getBean(ApplicationConfig.class);
        System.out.println(" Done.");
        try {
	        ValidationRunner runner = ctx.getBean(ValidationRunner.class);
	        runner.bootstrap(args, new File(config.getTmpFolder(), UUID.randomUUID().toString()));
        } catch(Exception e) {}
    }

    /**
     * Store in temporary folder resource files.
     * @throws IOException
     */
    private static void prepareConfigForStandalone(File tempFolder) throws IOException {
        // Explode invoice resources to temp folder
        File tempJarFile = new File(tempFolder, "validator-resources.jar");
        FileUtils.copyInputStreamToFile(Thread.currentThread().getContextClassLoader().getResourceAsStream("validator-resources.jar"), tempJarFile);
        JarFile resourcesJar = new JarFile(tempJarFile);
        Enumeration<JarEntry> entries = resourcesJar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = (JarEntry)entries.nextElement();
            File f = new File(tempFolder, entry.getName());
            if (entry.isDirectory()) { // if its a directory, create it
                f.mkdir();
                continue;
            }
            FileUtils.copyInputStreamToFile(resourcesJar.getInputStream(entry), f);
        }
    }

}
