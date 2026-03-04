package io.amichne.konditional.core.external

import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseError

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
 * ## Construction
 *
 * Prefer [ExternalSnapshotRef.parse] at boundary sites (where you receive raw strings)
 * to get a typed [ParseError.UnversionedExternalRef] instead of an exception when the version
 * is blank or missing.
 *
 * Use [ExternalSnapshotRef.Versioned] directly when both [id] and [version] are known at compile time.
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

        /**
         * Parses raw [id] and [version] strings into a typed [ExternalSnapshotRef].
         *
         * Returns [Result.failure] with [ParseError.UnversionedExternalRef] when [version] is blank
         * or [ParseError.InvalidValue] when [id] is blank.
         *
         * Use this at serialization boundaries where raw string input must produce typed errors.
         */
        fun parse(id: String, version: String): Result<ExternalSnapshotRef> = when {
            id.isBlank() -> Result.failure(
                KonditionalBoundaryFailure(
                    ParseError.UnversionedExternalRef(id = "(blank)", reason = "id must not be blank"),
                )
            )

            version.isBlank() -> Result.failure(
                KonditionalBoundaryFailure(
                    ParseError.UnversionedExternalRef(id = id, reason = "version must not be blank"),
                )
            )

            else -> Result.success(Versioned(id = id, version = version))
        }
    }
}
