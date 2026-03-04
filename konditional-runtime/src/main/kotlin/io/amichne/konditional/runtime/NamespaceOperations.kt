@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.runtime

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.instance.ConfigurationView
import io.amichne.konditional.core.registry.NamespaceRegistryRuntime
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.snapshot.ConfigurationCodec

/**
 * Runtime-only namespace operations (mutation/lifecycle).
 *
 * These are intentionally not part of the `:konditional-core` API surface.
 */
/**
 * Atomically loads [configuration] into this namespace's registry.
 *
 * Callers that need to decode JSON first should use
 * [io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader] from
 * `:konditional-runtime`, which combines decode and update in one call.
 */
fun Namespace.update(configuration: Configuration) {
    runtimeRegistry().load(configuration)
}

/**
 * Serializes the namespace's current immutable snapshot into canonical JSON.
 *
 * This is the preferred operational entry point for snapshot export because it is
 * namespace-scoped and deterministic for a fixed configuration state.
 */
fun Namespace.dump(): String = ConfigurationCodec.encode(this)

/**
 * Convenience accessor for [dump].
 */
val Namespace.json: String
    get() = dump()

fun Namespace.rollback(steps: Int = 1): Boolean =
    runtimeRegistry().rollback(steps)

val Namespace.history: List<ConfigurationView>
    get() = runtimeRegistry().history

private fun Namespace.runtimeRegistry(): NamespaceRegistryRuntime =
    registry as? NamespaceRegistryRuntime
        ?: error("NamespaceRegistryRuntime is required. Add :konditional-runtime to your dependencies.")
