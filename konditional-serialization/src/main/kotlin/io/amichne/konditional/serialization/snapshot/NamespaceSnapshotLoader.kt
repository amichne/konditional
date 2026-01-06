package io.amichne.konditional.serialization.snapshot

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.serialization.options.SnapshotLoadOptions

/**
 * Namespace-scoped snapshot loader.
 *
 * Decodes JSON via a [SnapshotCodec] and loads successful results into the namespace registry.
 *
 * ## Side Effects
 *
 * On success, the decoded [Configuration] is loaded into the namespace (replacing its current configuration).
 *
 * ## Usage Example
 *
 * ```kotlin
 * object Payments : Namespace("payments")
 *
 * val loader = NamespaceSnapshotLoader(Payments)
 * val loadedJson = File("configs/payments.json").readText()
 * val result = loader.load(loadedJson)
 * ```
 */
class NamespaceSnapshotLoader<M : Namespace>(
    private val namespace: M,
    private val codec: SnapshotCodec<Configuration> = ConfigurationSnapshotCodec,
) : SnapshotLoader<Configuration> {
    override fun load(
        json: String,
        options: SnapshotLoadOptions,
    ): ParseResult<Configuration> =
        when (val decoded = codec.decode(json, options)) {
            is ParseResult.Success -> ParseResult.success(decoded.value.also { namespace.load(it) })
            is ParseResult.Failure -> ParseResult.failure(decoded.error.withNamespaceContext(namespace.id))
        }

    private fun ParseError.withNamespaceContext(namespaceId: String): ParseError =
        when (this) {
            is ParseError.InvalidJson -> ParseError.InvalidJson("namespace='$namespaceId': $reason")
            is ParseError.InvalidSnapshot -> ParseError.InvalidSnapshot("namespace='$namespaceId': $reason")
            else -> this
        }

    companion object {
        fun <M : Namespace> forNamespace(namespace: M): NamespaceSnapshotLoader<M> = NamespaceSnapshotLoader(namespace)
    }
}
