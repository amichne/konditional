package io.amichne.konditional.core.result

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
