package io.amichne.konditional.core.result

import io.amichne.konditional.rules.predicate.PredicateRef
import io.amichne.konditional.values.FeatureId

/**
 * Domain-specific parseUnsafe errors that make failure reasons explicit and type-safe.
 *
 * Each error type contains structured information about what went wrong,
 * allowing consumers to handle errors precisely.
 */
sealed interface ParseError {
    /**
     * Human-readable error message.
     */
    val message: String

    /**
     * Failed to parseUnsafe a hexadecimal identifier.
     */
    @ConsistentCopyVisibility
    data class InvalidHexId internal constructor(
        val input: String,
        override val message: String,
    ) : ParseError

    /**
     * Invalid rampUp percentage (must be 0.0-100.0).
     */
    @ConsistentCopyVisibility
    data class InvalidRollout internal constructor(
        val value: Double,
        override val message: String,
    ) : ParseError

    /**
     * Failed to parseUnsafe a semantic version string.
     */
    @ConsistentCopyVisibility
    data class InvalidVersion internal constructor(
        val input: String,
        override val message: String,
    ) : ParseError

    /**
     * Feature key not found in registry.
     */
    @ConsistentCopyVisibility
    data class FeatureNotFound internal constructor(val key: FeatureId) : ParseError {
        override val message: String get() = "Feature not found: $key"
    }

    /**
     * An external snapshot ref is missing a required version pin.
     *
     * Emitted when a consumer attempts to register or load an [io.amichne.konditional.core.external.ExternalSnapshotRef]
     * with a blank [version][io.amichne.konditional.core.external.ExternalSnapshotRef.version].
     *
     * Unversioned refs are rejected at registration time — they never reach the evaluation path.
     */
    data class UnversionedExternalRef(
        val id: String,
        val reason: String = "version must not be blank",
        override val message: String = "External ref '$id' rejected: $reason",
    ) : ParseError

    /**
     * Predicate ref not found in the namespace-scoped [io.amichne.konditional.rules.predicate.PredicateRegistry].
     */
    data class UnknownPredicate(val ref: PredicateRef) : ParseError {
        override val message: String get() = "Unknown predicate: $ref"
    }

    companion object {
        fun featureNotFound(key: FeatureId): ParseError = FeatureNotFound(key)

        fun invalidJson(reason: String): ParseError = InvalidJson(reason)

        fun invalidSnapshot(reason: String): ParseError = InvalidSnapshot(reason)

        fun unversionedExternalRef(id: String, reason: String = "version must not be blank"): ParseError =
            UnversionedExternalRef(id = id, reason = reason)
            
        fun unknownPredicate(ref: PredicateRef): ParseError = UnknownPredicate(ref)
    }

    /**
     * Feature flag not found in registry.
     */
    @ConsistentCopyVisibility
    data class FlagNotFound internal constructor(val key: FeatureId) : ParseError {
        override val message: String get() = "Flag not found: $key"
    }

    /**
     * Failed to deserialize JSON into a snapshot.
     */
    data class InvalidSnapshot(val reason: String) : ParseError {
        override val message: String get() = "Invalid snapshot: $reason"
    }

    /**
     * Invalid JSON data that cannot be parsed.
     */
    @ConsistentCopyVisibility
    data class InvalidJson internal constructor(val reason: String) : ParseError {
        override val message: String get() = "Invalid JSON: $reason"
    }
}
