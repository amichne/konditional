package io.amichne.konditional.core.external

/**
 * A typed, versioned reference to a named external configuration source.
 *
 * [ExternalSnapshotRef] is an opt-in escape hatch for namespaces that need to
 * incorporate external data into evaluation. All uses are subject to strict invariants:
 *
 * - The [version] field must be non-blank and immutable (e.g., a content hash or monotonic token).
 * - Resolving the same `(id, version)` pair must always return the same content (determinism).
 * - Each ref is scoped to the namespace that registers it — cross-namespace lookup is not permitted.
 *
 * Refs without a valid [version] are rejected at load time with a typed
 * `ParseError.UnversionedExternalRef` — they never reach the evaluation path.
 *
 * ## Usage
 *
 * ```kotlin
 * val ref = ExternalSnapshotRef.Versioned(
 *     id = "remote-prices",
 *     version = "sha256:abc123",
 * )
 * ```
 */
sealed interface ExternalSnapshotRef {
    /** Stable identifier for the external source. Must be non-blank. */
    val id: String

    /**
     * Immutable version token that pins this ref to a specific snapshot of the external source.
     *
     * Must be non-blank. Typical values: content hash, monotonic sequence number, or
     * an opaque version string provided by the external system.
     */
    val version: String

    /**
     * A fully-versioned, immutable reference to a named external configuration snapshot.
     *
     * Both [id] and [version] are validated to be non-blank at construction time.
     *
     * @property id Stable identifier for the external source.
     * @property version Immutable version token pinning this ref to a specific snapshot.
     */
    data class Versioned(
        override val id: String,
        override val version: String,
    ) : ExternalSnapshotRef {
        init {
            require(id.isNotBlank()) { "ExternalSnapshotRef.id must not be blank" }
            require(version.isNotBlank()) { "ExternalSnapshotRef.version must not be blank" }
        }
    }

    companion object {
        /**
         * Creates a [Versioned] ref, validating both [id] and [version] at construction time.
         */
        fun versioned(id: String, version: String): ExternalSnapshotRef = Versioned(id = id, version = version)
    }
}
