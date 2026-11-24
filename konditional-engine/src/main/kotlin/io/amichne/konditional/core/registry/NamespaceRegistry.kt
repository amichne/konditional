@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core.registry

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.instance.definitionFor
import io.amichne.konditional.core.ops.RegistryHooks

/**
 * Abstraction for managing feature flag configurations and evaluation state.
 */
interface NamespaceRegistry {
    val namespaceId: String

    /**
     * The installed configuration and kill-switch state observed in one read.
     * Namespace evaluation falls back to declared definitions for missing entries.
     * Custom registries must implement this getter by capturing both fields under the
     * same lock or by reading one immutable snapshot from atomic storage.
     */
    val currentSnapshot: NamespaceSnapshot

    val configuration: Configuration

    val hooks: RegistryHooks

    fun setHooks(hooks: RegistryHooks)

    val isAllDisabled: Boolean

    fun load(config: Configuration)

    val history: List<NamespaceSnapshot>

    fun rollback(steps: Int = 1): Boolean

    fun disableAll()

    fun enableAll()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any, C : Context, M : Namespace> flag(
        key: Feature<T, C, M>,
    ): FlagDefinition<T, C, M> =
        configuration.flags[key] as FlagDefinition<T, C, M>

    /**
     * Safe lookup variant for callers that prefer typed absence handling over exceptions.
     */
    fun <T : Any, C : Context, M : Namespace> findFlag(
        key: Feature<T, C, M>,
    ): FlagDefinition<T, C, M>? =
        configuration.flags.definitionFor(key)

    fun allFlags(): Map<Feature<*, *, *>, FlagDefinition<*, *, *>> =
        configuration.flags
}
