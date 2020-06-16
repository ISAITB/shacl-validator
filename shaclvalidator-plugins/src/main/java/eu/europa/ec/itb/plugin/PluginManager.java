package eu.europa.ec.itb.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class used to provide plugin support for validators.
 *
 * The job of this class is to load the plugin implementations from the configuration and to provide them to
 * the validator when requested.
 */
@Component
@ConditionalOnBean(PluginConfigProvider.class)
public class PluginManager {

    private static final Logger LOG = LoggerFactory.getLogger(PluginManager.class);

    private ConcurrentHashMap<String, ValidationPlugin[]> pluginCache = new ConcurrentHashMap<>();

    @Autowired
    private PluginConfigProvider configProvider = null;

    @PostConstruct
    private void loadPlugins() {
        boolean pluginsFound = false;
        try {
            Map<String, List<PluginInfo>> pluginPathConfigs = configProvider.getPluginInfoPerType();
            for (Map.Entry<String, List<PluginInfo>> pluginConfig: pluginPathConfigs.entrySet()) {
                try {
                    List<ValidationPlugin> plugins = new ArrayList<>();
                    for (PluginInfo pluginInfo: pluginConfig.getValue()) {
                        plugins.addAll(getValidatorsFromJar(pluginInfo.getJarPath(), pluginInfo.getPluginClasses().toArray(new String[0])));
                    }
                    if (!plugins.isEmpty()) {
                        pluginsFound = true;
                        LOG.info("Loaded {} plugin(s) for {}", plugins.size(), pluginConfig.getKey());
                        pluginCache.put(pluginConfig.getKey(), plugins.toArray(new ValidationPlugin[0]));
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to initialise plugins for classifier ["+pluginConfig.getKey()+"]. Considering no plugins for this case.", e);
                    pluginCache.put(pluginConfig.getKey(), new ValidationPlugin[0]);
                }
            }
            if (!pluginsFound) {
                LOG.info("No plugins found");
            }
        } catch (Exception e) {
            LOG.warn("Failed to load plugin configuration. Continuing with no plugins.", e);
        }
    }

    /**
     * Get the plugins to consider for the given classifier.
     *
     * @param classifier The classifier to determine which plugins to return.
     * @return The array of plugins (never null).
     */
    public ValidationPlugin[] getPlugins(String classifier) {
        ValidationPlugin[] plugins = pluginCache.get(classifier);
        if (plugins == null) {
            plugins = new ValidationPlugin[0];
        }
        return plugins;
    }

    private List<ValidationPlugin> getValidatorsFromJar(Path jarFile, String[] classes) {
        try {
            List<ValidationPlugin> instances = new ArrayList<>();
            URLClassLoader loader = new URLClassLoader(new URL[] {jarFile.toUri().toURL()}, null);
            for (String clazz: classes) {
                Class pluginClass = loader.loadClass(clazz);
                instances.add(new PluginAdapter(pluginClass.getConstructor().newInstance(), loader));
            }
            return instances;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to instantiate plugin classes from JAR ["+jarFile.toFile().getAbsolutePath()+"]", e);
        }
    }

}
