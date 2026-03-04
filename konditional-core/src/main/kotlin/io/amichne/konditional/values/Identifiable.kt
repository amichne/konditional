package io.amichne.konditional.values

import io.amichne.konditional.values.IdentifierEncoding.SEPARATOR

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
interface Identifiable : Validateable {
    val value: String

    interface NonBlank : Identifiable {
        override fun validate() = apply {
            require(value.isNotBlank()) { "${this::class.simpleName} must not be blank" }
        }
    }

    interface Composable : NonBlank {
        override fun validate() = apply {
            super<NonBlank>.validate()
            require(!value.contains(SEPARATOR)) { "${this::class.simpleName} must not contain '$SEPARATOR': '$value'" }
        }
    }
}
