package eu.europa.ec.itb.plugin;

import java.nio.file.Path;
import java.util.List;

/**
 * The information needed to load a plugin (JAR file and list of class names).
 */
public class PluginInfo {

    private Path jarPath;
    private List<String> pluginClasses;

    Path getJarPath() {
        return jarPath;
    }

    public void setJarPath(Path jarPath) {
        this.jarPath = jarPath;
    }

    List<String> getPluginClasses() {
        return pluginClasses;
    }

    public void setPluginClasses(List<String> pluginClasses) {
        this.pluginClasses = pluginClasses;
    }

}
