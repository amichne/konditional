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
 * Abstraction for managing feature flag configurations and evaluations.
 *
 * `:konditional-core` depends only on this consumer-safe surface.
 * Mutation/lifecycle operations live behind [NamespaceRegistryRuntime].
 */
interface NamespaceRegistry {
    val namespaceId: String

    /**
     * Read-only view of the currently loaded configuration.
     */
    val configuration: ConfigurationView

    /**
     * Operational hooks for logging/metrics.
     *
     * Hooks are evaluated on the hot path; keep implementations lightweight.
     */
    val hooks: RegistryHooks

    fun setHooks(hooks: RegistryHooks)

    /**
     * Emergency kill switch: when enabled, all flag evaluations return their declared defaults.
     */
    val isAllDisabled: Boolean

    fun disableAll()

    fun enableAll()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any, C : Context, M : Namespace> flag(
        key: Feature<T, C, M>,
    ): FlagDefinition<T, C, M> =
        configuration.flags[key] as FlagDefinition<T, C, M>

    fun allFlags(): Map<Feature<*, *, *>, FlagDefinition<*, *, *>> =
        configuration.flags

    /**
     * Captures a deterministic, immutable snapshot for evaluation.
     */
    fun snapshot(): EvaluationSnapshot {
        val namespaceIdSnapshot = namespaceId
        val configurationSnapshot = configuration
        val hooksSnapshot = hooks
        val allDisabledSnapshot = isAllDisabled
        val registry = this

        return object : EvaluationSnapshot {
            override val namespaceId: String = namespaceIdSnapshot
            override val configuration: ConfigurationView = configurationSnapshot
            override val hooks: RegistryHooks = hooksSnapshot
            override val isAllDisabled: Boolean = allDisabledSnapshot

            override fun <T : Any, C : Context, M : Namespace> flag(
                feature: Feature<T, C, M>,
            ): FlagDefinition<T, C, M> = registry.flag(feature)
        }
    }
}

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
