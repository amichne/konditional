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
    data class InvalidHexId(val input: String, override val message: String) : ParseError

    /**
     * Invalid rollout percentage (must be 0.0-100.0).
     */
    data class InvalidRollout(val value: Double, override val message: String) : ParseError

    /**
     * Failed to parse a semantic version string.
     */
    data class InvalidVersion(val input: String, override val message: String) : ParseError

    /**
     * Conditional key not found in registry.
     */
    data class ConditionalNotFound(val key: String) : ParseError {
        override val message: String get() = "Conditional not found: $key"
    }

    /**
     * Feature flag not found in registry.
     */
    data class FlagNotFound(val key: String) : ParseError {
        override val message: String get() = "Flag not found: $key"
    }

    /**
     * Invalid salt value for bucketing.
     */
    data class InvalidSalt(val input: String, override val message: String) : ParseError
}
