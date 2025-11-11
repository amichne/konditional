/**
 * Internal implementation of [io.amichne.konditional.core.ConfigScope].
 *
 * This class is the internal implementation of the configuration DSL scope.
 * Users interact with the public [io.amichne.konditional.core.ConfigScope] interface,
 * not this implementation directly.
 *
 * @constructor Internal constructor - users cannot instantiate this class directly.
 */
package io.amichne.konditional.internal.builders

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.ConfigScope
import io.amichne.konditional.core.Feature
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.FlagScope
import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.core.instance.Konfig

@FeatureFlagDsl
@PublishedApi
internal class ConfigBuilder : ConfigScope {
    private val flags = LinkedHashMap<Feature<*, *>, FlagDefinition<*, *>>()

    /**
     * Define a flag using infix syntax.
     *
     * Implementation of [ConfigScope.with] that delegates to [FlagBuilder].
     */
    override infix fun <S : Any, C : Context> Feature<S, C>.with(build: FlagScope<S, C>.() -> Unit) {
        require(this !in this@ConfigBuilder.flags) { "Duplicate flag $this" }
        this@ConfigBuilder.flags[this] = FlagBuilder(this).apply(build).build()
    }

    @PublishedApi
    internal fun build(): Konfig = Konfig(flags.toMap())
}
