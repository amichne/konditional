@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core.registry

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.instance.ConfigurationView
import io.amichne.konditional.core.ops.RegistryHooks

/**
 * Immutable view over a namespace evaluation state.
 *
 * Use this to keep multi-flag evaluations deterministic by pinning evaluation to the same
 * configuration snapshot and override set.
 */
interface EvaluationSnapshot {
    val namespaceId: String
    val configuration: ConfigurationView
    val hooks: RegistryHooks
    val isAllDisabled: Boolean

    @Suppress("UNCHECKED_CAST")
    fun <T : Any, C : Context, M : Namespace> flag(
        feature: Feature<T, C, M>,
    ): FlagDefinition<T, C, M> =
        configuration.flags[feature] as? FlagDefinition<T, C, M>
            ?: error("Flag not found in configuration: ${feature.key}")
}

internal data class DefaultEvaluationSnapshot(
    override val namespaceId: String,
    override val configuration: ConfigurationView,
    override val hooks: RegistryHooks,
    override val isAllDisabled: Boolean,
) : EvaluationSnapshot
