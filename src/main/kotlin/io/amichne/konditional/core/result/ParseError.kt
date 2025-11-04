package io.amichne.konditional.core.result

/**
 * Domain-specific parse errors that make failure reasons explicit and type-safe.
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
     * Failed to parse a hexadecimal identifier.
     */
    @ConsistentCopyVisibility
    data class InvalidHexId internal constructor(val input: String, override val message: String) : ParseError

    /**
     * Invalid rollout percentage (must be 0.0-100.0).
     */
    @ConsistentCopyVisibility
    data class InvalidRollout internal constructor(val value: Double, override val message: String) : ParseError

    /**
     * Failed to parse a semantic version string.
     */
    @ConsistentCopyVisibility
    data class InvalidVersion internal constructor(val input: String, override val message: String) : ParseError

    /**
     * Conditional key not found in registry.
     */
    @ConsistentCopyVisibility
    data class ConditionalNotFound internal constructor(val key: String) : ParseError {
        override val message: String get() = "Conditional not found: $key"
    }

    /**
     * Feature flag not found in registry.
     */
    @ConsistentCopyVisibility
    data class FlagNotFound internal constructor(val key: String) : ParseError {
        override val message: String get() = "Flag not found: $key"
    }

    /**
     * Invalid salt value for bucketing.
     */
    @ConsistentCopyVisibility
    data class InvalidSalt internal constructor(val input: String, override val message: String) : ParseError

    /**
     * Failed to deserialize JSON into a snapshot.
     */
    @ConsistentCopyVisibility
    data class InvalidSnapshot internal constructor(val reason: String) : ParseError {
        override val message: String get() = "Invalid snapshot: $reason"
    }

    /**
     * Type mismatch during deserialization.
     */
    @ConsistentCopyVisibility
    data class TypeMismatch internal constructor(val expected: String, val actual: String, val context: String) :
        ParseError {
        override val message: String get() = "Type mismatch in $context: expected $expected but got $actual"
    }

    /**
     * Invalid JSON data that cannot be parsed.
     */
    @ConsistentCopyVisibility
    data class InvalidJson internal constructor(val reason: String) : ParseError {
        override val message: String get() = "Invalid JSON: $reason"
    }
}
