package eu.europa.ec.itb.plugin;

import com.gitb.vs.GetModuleDefinitionResponse;
import com.gitb.vs.ValidationService;

/**
 * Plugin extension of the ValidationService interface.
 */
public interface ValidationPlugin extends ValidationService {

    /**
     * Get the name of the current plugin.
     *
     * @return The plugin name.
     */
    default String getName() {
        GetModuleDefinitionResponse pluginDefinition = getModuleDefinition(null);
        if (pluginDefinition != null && pluginDefinition.getModule() != null) {
            if (pluginDefinition.getModule().getId() != null) {
                return pluginDefinition.getModule().getId();
            } else if (pluginDefinition.getModule().getMetadata() != null && pluginDefinition.getModule().getMetadata().getName() != null) {
                return pluginDefinition.getModule().getMetadata().getName();
            }
        }
        return "";
    }
}
