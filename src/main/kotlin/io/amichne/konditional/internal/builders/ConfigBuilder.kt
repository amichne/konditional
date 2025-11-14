package io.amichne.konditional.internal.builders

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.dsl.ConfigScope
import io.amichne.konditional.core.Feature
import io.amichne.konditional.core.dsl.FeatureFlagDsl
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.instance.Konfig
import io.amichne.konditional.core.types.EncodableValue

/**
 * Internal implementation of [ConfigScope].
 *
 * This class is the internal implementation of the configuration DSL scope.
 * Users interact with the public [ConfigScope] interface,
 * not this implementation directly.
 *
 * @constructor Internal constructor - users cannot instantiate this class directly.
 */
@FeatureFlagDsl
@PublishedApi
internal class ConfigBuilder : ConfigScope {
    private val flags = LinkedHashMap<Feature<*, *, *, *>, FlagDefinition<*, *, *, *>>()

    /**
     * Define a flag using infix syntax.
     *
     * Implementation of [ConfigScope.with] that delegates to [FlagBuilder].
     */
    override infix fun <S : EncodableValue<T>, T : Any, C : Context, M : Taxonomy> Feature<S, T, C, M>.with(build: FlagScope<S, T, C, M>.() -> Unit) {
        require(this !in this@ConfigBuilder.flags) { "Duplicate flag $this" }
        this@ConfigBuilder.flags[this] = FlagBuilder(this).apply(build).build()
    }

    @PublishedApi
    internal fun build(): Konfig = Konfig(flags.toMap())
}
