package io.amichne.konditional.serialization.snapshot

import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.serialization.options.SnapshotLoadOptions

/**
 * Side-effecting JSON snapshot loader.
 *
 * Implementations may update process state (e.g., load into a namespace registry).
 */
interface SnapshotLoader<T> {
    fun load(
        json: String,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): ParseResult<T>
}

