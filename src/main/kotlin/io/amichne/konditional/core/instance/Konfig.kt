package io.amichne.konditional.core.instance

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.FeatureFlag

/**
 * Represents a complete feature flag configuration organized by modules.
 *
 * Instead of storing flags in a flat map, Konfig now organizes flags into modules.
 * Each module has a name and contains a fixed set of feature flags.
 *
 * @param modules Map of module names to their configurations
 */
@ConsistentCopyVisibility
data class Konfig internal constructor(
    val modules: Map<String, ModuleConfig<*>>
) {
    /**
     * Returns all flags across all modules in a flat map.
     *
     * This is provided for backward compatibility and flag evaluation.
     * Prefer working with modules directly when possible.
     */
    val flags: Map<Conditional<*, *, *>, FeatureFlag<*, *, *>> by lazy {
        modules.values.flatMap { moduleConfig ->
            moduleConfig.flags.entries
        }.associate { it.key to it.value }
    }

    /**
     * Gets a specific module configuration by name.
     *
     * @param moduleName The name of the module
     * @return The module configuration if found, null otherwise
     */
    fun getModule(moduleName: String): ModuleConfig<*>? = modules[moduleName]

    /**
     * Gets all module names in this configuration.
     */
    val moduleNames: Set<String> get() = modules.keys
}
