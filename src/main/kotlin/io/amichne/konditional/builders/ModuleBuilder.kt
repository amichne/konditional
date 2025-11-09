package io.amichne.konditional.builders

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.FeatureFlag
import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.core.Module
import io.amichne.konditional.core.instance.ModuleConfig
import io.amichne.konditional.core.types.EncodableValue

/**
 * A builder class for defining and configuring modules with their feature flags.
 *
 * This builder provides a DSL for associating feature flag definitions with a module.
 * All flags declared in the module must be defined within the builder.
 *
 * Example usage:
 * ```
 * module(MyModules.USER_FEATURES) {
 *     UserFlags.ENABLE_PROFILE with {
 *         default(true)
 *         rule { ... } implies false
 *     }
 *     UserFlags.ENABLE_NOTIFICATIONS with {
 *         default(false)
 *     }
 * }
 * ```
 *
 * @param C The type of context used for flag evaluation
 * @property module The module instance being configured
 */
@ConsistentCopyVisibility
@FeatureFlagDsl
data class ModuleBuilder<C : Context> internal constructor(
    private val module: Module<C>,
) {
    private val flags = LinkedHashMap<Conditional<*, *, C>, FeatureFlag<*, *, C>>()

    /**
     * Define a flag within this module using infix syntax.
     *
     * Example:
     * ```
     * MyFlag.EXAMPLE with {
     *     default(true)
     *     rule { ... } implies false
     * }
     * ```
     *
     * @param S The EncodableValue type wrapper
     * @param T The actual value type
     * @param build The flag configuration lambda
     */
    infix fun <S : EncodableValue<T>, T : Any> Conditional<S, T, C>.with(
        build: FlagBuilder<S, T, C>.() -> Unit
    ) {
        require(this !in this@ModuleBuilder.flags) { "Duplicate flag $this in module '${module.moduleName}'" }
        require(this in module.flags()) {
            "Flag $this is not declared in module '${module.moduleName}'. " +
                "Only flags declared in the module's flags() method can be configured."
        }
        this@ModuleBuilder.flags[this] = FlagBuilder(this).apply(build).build()
    }

    /**
     * Builds the module configuration.
     *
     * @throws IllegalStateException if not all module flags have been defined
     */
    internal fun build(): ModuleConfig<C> {
        val moduleFlags = module.flags()
        val definedFlags = flags.keys

        val missingFlags = moduleFlags - definedFlags
        require(missingFlags.isEmpty()) {
            "Module '${module.moduleName}' is missing definitions for flags: ${missingFlags.joinToString { it.key }}"
        }

        val extraFlags = definedFlags - moduleFlags
        require(extraFlags.isEmpty()) {
            "Module '${module.moduleName}' has extra flags not declared in the module: ${extraFlags.joinToString { it.key }}"
        }

        return ModuleConfig(
            module = module,
            flags = flags.toMap()
        )
    }
}
