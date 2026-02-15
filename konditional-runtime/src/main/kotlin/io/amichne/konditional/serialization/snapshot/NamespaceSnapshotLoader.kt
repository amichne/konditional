@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization.snapshot

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.registry.NamespaceRegistryRuntime
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.parseErrorOrNull
import io.amichne.konditional.core.result.parseFailure
import io.amichne.konditional.serialization.instance.MaterializedConfiguration
import io.amichne.konditional.serialization.options.SnapshotLoadOptions

/**
 * Namespace-scoped snapshot loader.
 *
 * Decodes JSON via a [SnapshotCodec] and loads successful results into the namespace registry.
 */
class NamespaceSnapshotLoader<M : Namespace>(
    private val namespace: M,
    private val codec: SnapshotCodec<MaterializedConfiguration> = ConfigurationSnapshotCodec,
) : SnapshotLoader<MaterializedConfiguration> {
    override fun load(
        json: String,
        options: SnapshotLoadOptions,
    ): Result<MaterializedConfiguration> {
        val decoded = decodeSnapshot(json, options)
        if (decoded.isFailure) {
            val contextualError = decoded.parseErrorOrNull()?.withNamespaceContext(namespace.id)
            return if (contextualError != null) {
                parseFailure(contextualError)
            } else {
                Result.failure(decoded.exceptionOrNull() ?: IllegalStateException("Unknown load failure"))
            }
        }

        val materialized = decoded.getOrThrow()
        namespace.runtimeRegistry().load(materialized.configuration)
        return Result.success(materialized)
    }

    private fun decodeSnapshot(
        json: String,
        options: SnapshotLoadOptions,
    ): Result<MaterializedConfiguration> =
        (codec as? FeatureAwareSnapshotCodec<MaterializedConfiguration>)
            ?.decode(
                json = json,
                schema = namespace.compiledSchema(),
                options = options,
            )
            ?: codec.decode(json = json, options = options)

    private fun Namespace.runtimeRegistry(): NamespaceRegistryRuntime =
        registry as? NamespaceRegistryRuntime
            ?: error(
                "NamespaceRegistryRuntime is required. " +
                    "Add :konditional-runtime to your dependencies to enable runtime operations.",
            )

    private fun ParseError.withNamespaceContext(namespaceId: String): ParseError =
        when (this) {
            is ParseError.InvalidJson -> ParseError.invalidJson("namespace='$namespaceId': $reason")
            is ParseError.InvalidSnapshot -> ParseError.invalidSnapshot("namespace='$namespaceId': $reason")
            else -> this
        }

    companion object {
        fun <M : Namespace> forNamespace(namespace: M): NamespaceSnapshotLoader<M> = NamespaceSnapshotLoader(namespace)
    }
}
