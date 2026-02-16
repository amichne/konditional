package io.amichne.konditional.core.registry

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.instance.ConfigurationView

/**
 * Opt-in, cross-module runtime contract for mutation and lifecycle.
 */
@KonditionalInternalApi
interface NamespaceRegistryRuntime : NamespaceRegistry {
    fun load(config: ConfigurationView)

    val history: List<ConfigurationView>

    fun rollback(steps: Int = 1): Boolean

    fun updateDefinition(definition: FlagDefinition<*, *, *>)

    fun <T : Any, C : Context, M : Namespace> setOverride(
        feature: Feature<T, C, M>,
        value: T,
    )

    fun <T : Any, C : Context, M : Namespace> clearOverride(
        feature: Feature<T, C, M>,
    )
}
