/**
 * A builder class for defining and configuring feature flags using a DSL (Domain-Specific Language).
 *
 * This class allows you to define modules containing feature flags and their associated rules
 * in a declarative manner. Flags must be organized into modules and cannot be registered directly.
 *
 * Example usage:
 * ```
 * ConfigBuilder.config {
 *     module(MyModules.USER_FEATURES) {
 *         UserFlags.ENABLE_PROFILE with {
 *             default(true)
 *             rule { ... } implies false
 *         }
 *     }
 * }
 * ```
 *
 * @constructor Private constructor to enforce usage of the DSL entry point via the `config` function.
 */
package io.amichne.konditional.builders

import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.core.FlagRegistry
import io.amichne.konditional.core.Module
import io.amichne.konditional.core.instance.Konfig
import io.amichne.konditional.core.instance.ModuleConfig

@FeatureFlagDsl
class ConfigBuilder private constructor() {
    private val modules = LinkedHashMap<String, ModuleConfig>()

    /**
     * Define a module with its feature flags.
     *
     * All flags declared in the module must be defined within the builder block.
     *
     * Example:
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
     * @param module The module instance to configure
     * @param build The module configuration lambda
     */
    fun module(
        module: Module,
        build: ModuleBuilder.() -> Unit,
    ) {
        val moduleName = module.moduleName
        require(moduleName !in modules) {
            "Duplicate module '$moduleName'"
        }
        modules[moduleName] = ModuleBuilder(module).apply(build).build()
    }

    fun build(): Konfig = Konfig(modules.toMap())

    @FeatureFlagDsl
    companion object {
        fun config(registry: FlagRegistry = FlagRegistry, fn: ConfigBuilder.() -> Unit): Unit =
            ConfigBuilder().apply(fn).build().let { registry.load(it) }

        /**
         * Builds a Konfig without loading it into SingletonFlagRegistry.
         * Useful for testing and external snapshot management.
         */
        fun buildSnapshot(fn: ConfigBuilder.() -> Unit): Konfig =

            ConfigBuilder().apply(fn).build()
    }
}
