package io.amichne.konditional.runtime

/**
 * Source of raw JSON snapshot strings for a single namespace endpoint.
 *
 * Implementations must:
 * - Return [Result.success] with a non-empty JSON string on success.
 * - Return [Result.failure] on any transport-level error; never throw.
 *
 * The result is passed directly to [NamespaceSnapshotLoader]; parse errors
 * are handled there and never clobber the active registry.
 */
fun interface SnapshotFetcher {
    suspend fun fetch(): Result<String>
}
