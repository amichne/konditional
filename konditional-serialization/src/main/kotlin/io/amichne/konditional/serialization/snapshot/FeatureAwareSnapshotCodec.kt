package io.amichne.konditional.serialization.snapshot

import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.serialization.options.SnapshotLoadOptions
import io.amichne.konditional.values.FeatureId

/**
 * Snapshot codec that can decode against a trusted, namespace-scoped feature index.
 *
 * This allows callers to avoid global feature lookup during deserialization by providing
 * the exact feature universe that should be considered valid for the decode operation.
 */
interface FeatureAwareSnapshotCodec<T> : SnapshotCodec<T> {
    fun decode(
        json: String,
        featuresById: Map<FeatureId, Feature<*, *, *>>,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): ParseResult<T>
}

