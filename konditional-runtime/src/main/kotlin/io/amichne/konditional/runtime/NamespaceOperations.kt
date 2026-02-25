@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.runtime

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.instance.ConfigurationMetadataView
import io.amichne.konditional.core.instance.ConfigurationView
import io.amichne.konditional.core.registry.NamespaceRegistryRuntime
import io.amichne.konditional.serialization.instance.MaterializedConfiguration

/**
 * Runtime-only namespace operations (mutation/lifecycle).
 *
 * These are intentionally not part of the `:konditional-core` API surface.
 */
/**
 * Atomically loads [configuration] into this namespace's registry.
 *
 * Callers that need to decode JSON first should use [NamespaceSnapshotLoader] from
 * `:konditional-runtime`, which combines decode and update in one call.
 */
fun Namespace.update(configuration: MaterializedConfiguration) {
    runtimeRegistry().load(configuration.configuration)
}

fun Namespace.rollback(steps: Int = 1): Boolean =
    runtimeRegistry().rollback(steps)

val Namespace.history: List<ConfigurationView>
    get() = runtimeRegistry().history

val Namespace.historyMetadata: List<ConfigurationMetadataView>
    get() = history.map { it.metadata }

private fun Namespace.runtimeRegistry(): NamespaceRegistryRuntime =
    registry as? NamespaceRegistryRuntime
        ?: error("NamespaceRegistryRuntime is required. Add :konditional-runtime to your dependencies.")
