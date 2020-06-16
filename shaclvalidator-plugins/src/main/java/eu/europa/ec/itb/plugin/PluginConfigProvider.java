package eu.europa.ec.itb.plugin;

import java.util.List;
import java.util.Map;

/**
 * Provider of the configuration needed to load plugins.
 *
 * The configuration currently provided in a map of unique classifiers that point to
 * an array of PluginInfo objects. These represent the plugin JAR and classes to load implementations
 * from.
 */
public interface PluginConfigProvider {

    /**
     * Get the map of plugin classifiers to plugin information (JARs and classes per JAR).
     *
     * Classifiers are unique keys to determine sets of plugins (determined by the caller).
     *
     * @return The plugin configuration map.
     */
    Map<String, List<PluginInfo>> getPluginInfoPerType();

}
