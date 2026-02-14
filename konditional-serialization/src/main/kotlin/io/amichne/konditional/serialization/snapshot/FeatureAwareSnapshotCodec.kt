@file:OptIn(io.amichne.konditional.api.KonditionalInternalApi::class)

package io.amichne.konditional.serialization.snapshot

import io.amichne.konditional.core.schema.CompiledNamespaceSchema
import io.amichne.konditional.serialization.options.SnapshotLoadOptions

/**
 * Snapshot codec that can decode against a trusted, namespace-scoped feature index.
 *
 * This allows callers to avoid global feature lookup during deserialization by providing
 * the exact feature universe that should be considered valid for the decode operation.
 */
interface FeatureAwareSnapshotCodec<T> : SnapshotCodec<T> {
    fun decode(
        json: String,
        schema: CompiledNamespaceSchema,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<T>
}
