/**
 * A builder class for defining and configuring feature flags using a DSL (Domain-Specific Language).
 *
 * This class allows you to define feature flags and their associated rules in a declarative manner.
 * The flags are stored in a registry that can be loaded into the application.
 *
 * Example usage:
 * ```
 * ConfigBuilder.config {
 *     Conditional.ENABLE_COMPACT_CARDS with {
 *         default(value = BooleanConditional.TRUE)
 *         boundary { ... }
 *     }
 * }
 * ```
 *
 * @constructor Private constructor to enforce usage of the DSL entry point via the `config` function.
 */
package io.amichne.konditional.builders

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.FeatureFlag
import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.core.FlagRegistry
import io.amichne.konditional.core.instance.Konfig

@FeatureFlagDsl
class ConfigBuilder private constructor() {
    private val flags = LinkedHashMap<Conditional<*, *>, FeatureFlag<*, *>>()

    /**
     * Define a flag using infix syntax:
     * ```
     * Conditional.ENABLE_COMPACT_CARDS with {
     *     default(value = BooleanConditional.TRUE)
     *     boundary { ... }
     * }
     * ```
     */
    infix fun <S : Any, C : Context> Conditional<S, C>.with(build: FlagBuilder<S, C>.() -> Unit) {
        require(this !in this@ConfigBuilder.flags) { "Duplicate flag $this" }
        this@ConfigBuilder.flags[this] = FlagBuilder(this).apply<FlagBuilder<S, C>>(build).build()
    }

    fun build(): Konfig = Konfig(flags.toMap())

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
