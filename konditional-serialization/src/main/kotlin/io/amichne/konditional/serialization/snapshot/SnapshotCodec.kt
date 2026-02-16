package io.amichne.konditional.serialization.snapshot

import io.amichne.konditional.serialization.options.SnapshotLoadOptions

/**
 * Pure JSON snapshot codec.
 *
 * This interface is intentionally side-effect free:
 * - Encoding does not mutate runtime state
 * - Decoding does not load into any namespace registry
 */
interface SnapshotCodec<T> {
    fun encode(value: T): String

    fun decode(
        json: String,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<T>
}
