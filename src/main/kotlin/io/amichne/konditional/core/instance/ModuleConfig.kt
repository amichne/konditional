package io.amichne.konditional.core.instance

import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.FeatureFlag
import io.amichne.konditional.core.Module

/**
 * Represents a module configuration containing a fixed set of feature flags.
 *
 * This class holds the association between a module and its flag definitions.
 * All flags defined within a module are stored together in a single field.
 *
 * @param module The module instance
 * @param flags Map of conditionals to their feature flag definitions for this module
 * @param C The type of context used for flag evaluation
 */
@ConsistentCopyVisibility
data class ModuleConfig internal constructor(
    val module: Module,
    val flags: Map<Conditional<*, *, *>, FeatureFlag<*, *, *>>
) {
    /**
     * The name of the module.
     */
    val moduleName: String get() = module.moduleName
}
