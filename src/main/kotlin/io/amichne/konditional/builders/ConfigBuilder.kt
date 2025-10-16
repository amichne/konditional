/**
 * A builder class for defining and configuring feature flags using a DSL (Domain-Specific Language).
 *
 * This class allows you to define feature flags and their associated rules in a declarative manner.
 * The flags are stored in a registry that can be loaded into the application.
 *
 * Example usage:
 * ```
 * ConfigBuilder.config {
 *     FeatureFlag.ENABLE_COMPACT_CARDS withRules {
 *         default(value = BooleanConditional.TRUE)
 *         rule { ... }
 *     }
 * }
 * ```
 *
 * @constructor Private constructor to enforce usage of the DSL entry point via the `config` function.
 */
package io.amichne.konditional.builders

import io.amichne.konditional.core.FeatureFlag
import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.core.Flag
import io.amichne.konditional.core.Flags

@FeatureFlagDsl
class ConfigBuilder private constructor(){
    private val flags = LinkedHashMap<FeatureFlag<*>, Flag<*>>()

    /**
     * Define a flag using infix syntax:
     * ```
     * FeatureFlag.ENABLE_COMPACT_CARDS withRules {
     *     default(value = BooleanConditional.TRUE)
     *     rule { ... }
     * }
     * ```
     */
    infix fun <S : Any> FeatureFlag<S>.withRules(build: FlagBuilder<S>.() -> Unit) {
        require(this !in flags) { "Duplicate flag $this" }
        flags[this] = FlagBuilder(this).apply(build).build()
    }

    fun build(): Flags.Snapshot = Flags.Snapshot(flags.toMap())

    @FeatureFlagDsl
    companion object {
        fun config(fn: ConfigBuilder.() -> Unit): Unit =
            ConfigBuilder().apply(fn).build().let {
                Flags.load(it)
            }
    }
}
