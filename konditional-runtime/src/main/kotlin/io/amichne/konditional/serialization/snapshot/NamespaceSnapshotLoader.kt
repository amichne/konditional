@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization.snapshot

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.registry.NamespaceRegistryRuntime
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.parseErrorOrNull
import io.amichne.konditional.core.result.parseFailure
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.options.SnapshotLoadOptions
import io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader.Companion.forNamespace

/**
 * Side-effecting snapshot loader scoped to a [Namespace].
 *
 * Decodes a JSON snapshot via [ConfigurationCodec] against the namespace's
 * compile-time schema, then atomically loads the result into the namespace registry.
 * The registry is only mutated on success — a parse failure leaves the current state intact.
 *
 * Construct via [forNamespace]; the constructor is private to prevent codec injection,
 * which previously allowed bypassing the schema-aware decode path.
 */
class NamespaceSnapshotLoader<M : Namespace> private constructor(
    private val namespace: M,
) {
    /**
     * Decodes [json] and loads it into the namespace registry if decoding succeeds.
     *
     * @return [Result.success] with the [Configuration] on success,
     *         or [Result.failure] with a typed [ParseError] on failure. Never throws.
     */
    fun load(
        json: String,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<Configuration> =
        ConfigurationCodec.decode(
            json = json,
            namespace = namespace,
            options = options,
        ).fold(
            onSuccess = { configuration ->
                namespace.runtimeRegistry().load(configuration)
                Result.success(configuration)
            },
            onFailure = { throwable ->
                val parseError = throwable.parseErrorOrNull()
                if (parseError != null) {
                    parseFailure(parseError.withNamespaceContext(namespace.id.value))
                } else {
                    Result.failure(throwable)
                }
            },
        )

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
        /**
         * Creates a [NamespaceSnapshotLoader] scoped to [namespace].
         */
        fun <M : Namespace> forNamespace(namespace: M): NamespaceSnapshotLoader<M> = NamespaceSnapshotLoader(namespace)
    }
}
