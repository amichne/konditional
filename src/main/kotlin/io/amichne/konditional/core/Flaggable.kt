package io.amichne.konditional.core

/**
 * Interface for types that can be used as feature flag values.
 * Implementations must provide a way to parse from String representation.
 */
interface Flaggable<T : Any> {
    val value: T
    /**
     * Parse a string representation into an instance of this type.
     */
    fun parse(value: String): T
}
