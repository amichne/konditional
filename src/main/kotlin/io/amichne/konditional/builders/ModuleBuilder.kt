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
data class ModuleBuilder internal constructor(
    private val module: Module,
) {
    private val flags = LinkedHashMap<Conditional<*, *, *>, FeatureFlag<*, *, *>>()

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
    infix fun <S : EncodableValue<T>, T : Any, C : Context> Conditional<S, T, C>.with(
        build: FlagBuilder<S, T, C>.() -> Unit
    ) {
        require(this !in this@ModuleBuilder.flags) { "Duplicate flag $this in module '${module.moduleName}'" }
        this@ModuleBuilder.flags[this] = FlagBuilder(this).apply(build).build()
    }

    /**
     * Builds the module configuration.
     *
     * @throws IllegalStateException if not all module flags have been defined
     */
    internal fun build(): ModuleConfig = ModuleConfig(
        module = module,
        flags = flags.toMap()
    )
}
