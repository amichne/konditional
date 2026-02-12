@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization.snapshot

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.registry.NamespaceRegistryRuntime
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.options.SnapshotLoadOptions

/**
 * Namespace-scoped snapshot loader.
 *
 * Decodes JSON via a [SnapshotCodec] and loads successful results into the namespace registry.
 */
class NamespaceSnapshotLoader<M : Namespace>(
    private val namespace: M,
    private val codec: SnapshotCodec<Configuration> = ConfigurationSnapshotCodec,
) : SnapshotLoader<Configuration> {
    override fun load(
        json: String,
        options: SnapshotLoadOptions,
    ): ParseResult<Configuration> =
        when (val decoded = decodeSnapshot(json, options)) {
            is ParseResult.Success ->
                ParseResult.success(
                    decoded.value.also { config ->
                        namespace.runtimeRegistry().load(config)
                    },
                )
            is ParseResult.Failure -> ParseResult.failure(decoded.error.withNamespaceContext(namespace.id))
        }

    @Suppress("UNCHECKED_CAST")
    private fun decodeSnapshot(
        json: String,
        options: SnapshotLoadOptions,
    ): ParseResult<Configuration> =
        (codec as? FeatureAwareSnapshotCodec<Configuration>)
            ?.decode(
                json = json,
                featuresById = namespace.featureIndexById(),
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

private fun Namespace.featureIndexById() =
    allFeatures()
        .sortedBy { feature -> feature.id.toString() }
        .associateBy { feature -> feature.id }
