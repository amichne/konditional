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
import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.core.Flags

@FeatureFlagDsl
class ConfigBuilder private constructor() {
    private val flags = LinkedHashMap<Conditional<*, *>, Flags.FlagEntry<*, *>>()

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
        require(this !in flags) { "Duplicate flag $this" }
        flags[this] = Flags.FlagEntry(FlagBuilder(this).apply<FlagBuilder<S, C>>(build).build())
    }

    fun build(): Flags.Snapshot = Flags.Snapshot(flags.toMap())

    @FeatureFlagDsl
    companion object {
        fun config(fn: ConfigBuilder.() -> Unit): Unit =
            ConfigBuilder().apply(fn).build().let {
                Flags.load(it)
            }

        /**
         * Builds a Snapshot without loading it into Flags.
         * Useful for testing and external snapshot management.
         */
        fun buildSnapshot(fn: ConfigBuilder.() -> Unit): Flags.Snapshot =
            ConfigBuilder().apply(fn).build()
    }
}
