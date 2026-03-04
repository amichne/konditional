package io.amichne.konditional.values

/**
 * Base interface for strongly-typed identifier value classes.
 *
 * Implement this interface on `@JvmInline value class` types to produce typed,
 * zero-overhead identifier wrappers with a uniform access surface throughout the codebase.
 *
 * Example:
 * ```kotlin
 * @JvmInline
 * value class NamespaceId(override val value: String) : Identifiable
 * ```
 */
interface Identifiable {
    val value: String
}
