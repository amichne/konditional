package io.amichne.konditional.builders

import io.amichne.konditional.core.FeatureFlag
import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.core.Flag
import io.amichne.konditional.core.Flaggable
import io.amichne.konditional.core.Flags

@FeatureFlagDsl
class ConfigBuilder private constructor(){
    private val flags = LinkedHashMap<FeatureFlag<*, *>, Flag<*, *>>()

    /**
     * Define a flag using infix syntax:
     * ```
     * FeatureFlag.ENABLE_COMPACT_CARDS withRules {
     *     default(value = BooleanFlaggable.TRUE)
     *     rule { ... }
     * }
     * ```
     */
    infix fun <T : Flaggable<S>, S : Any> FeatureFlag<T, S>.withRules(build: FlagBuilder<S, T>.() -> Unit) {
        require(this !in flags) { "Duplicate flag $this" }
        flags[this] = FlagBuilder(this).apply(build).build()
    }

    fun build(): Flags.Registry = Flags.Registry(flags.toMap())

    @FeatureFlagDsl
    companion object {
        fun config(fn: ConfigBuilder.() -> Unit): Unit =
            ConfigBuilder().apply(fn).build().let {
                Flags.load(it)
            }
    }
}
