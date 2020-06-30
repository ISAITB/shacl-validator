package eu.europa.ec.itb.shacl;

import eu.europa.ec.itb.validation.plugin.PluginConfigProvider;
import eu.europa.ec.itb.validation.plugin.PluginInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used to load the configuration needed to support plugins.
 *
 * The implementation of this class would differ based on how plugins are loaded from
 * a configuration file.
 */
@Component
public class DomainPluginConfigProvider implements PluginConfigProvider {

    @Autowired
    private DomainConfigCache domainConfigCache = null;

    /**
     * The plugin classifier is used to split plugin implementations in sets.
     *
     * In this case the classifier is constructed from the domain and validation type
     * combination.
     *
     * @param domainConfig The current domain.
     * @param validationType The current validation type.
     * @return The classifier to consider.
     */
    public String getPluginClassifier(DomainConfig domainConfig, String validationType) {
        return domainConfig.getDomainName()+"|"+validationType;
    }

    @Override
    public Map<String, List<PluginInfo>> getPluginInfoPerType() {
        Map<String, List<PluginInfo>> pluginInfoPerDomain = new HashMap<>();
        for (DomainConfig domainConfig: domainConfigCache.getAllDomainConfigurations()) {
            if (domainConfig.isDefined()) {
                for (String validationType: domainConfig.getType()) {
                    List<PluginInfo> pluginInfoList = new ArrayList<>();
                    // Default plugins for all validation types in the domain.
                    if (domainConfig.getPluginDefaultConfig() != null) {
                        pluginInfoList.addAll(domainConfig.getPluginDefaultConfig());
                    }
                    // Type-specific plugins.
                    if (domainConfig.getPluginPerTypeConfig() != null && domainConfig.getPluginPerTypeConfig().containsKey(validationType)) {
                        List<PluginInfo> typeSpecificPluginInfo = domainConfig.getPluginPerTypeConfig().get(validationType);
                        if (typeSpecificPluginInfo != null) {
                            pluginInfoList.addAll(domainConfig.getPluginPerTypeConfig().get(validationType));
                        }
                    }
                    if (!pluginInfoList.isEmpty()) {
                        // Record in map. Define plugin type key as combination of domain name and validation type.
                        pluginInfoPerDomain.put(getPluginClassifier(domainConfig, validationType), pluginInfoList);
                    }
                }
            }
        }
        return pluginInfoPerDomain;
    }
}
