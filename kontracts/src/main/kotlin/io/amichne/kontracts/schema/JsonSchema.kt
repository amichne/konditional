package io.amichne.kontracts.schema

/**
 * Sealed class representing compile-time schema definitions for JSON values, with OpenAPI-esque properties.
 */
sealed class JsonSchema<out T : Any> : OpenApi<T> {
    abstract override val type: OpenApi.Type
    override val title: String? = null
    override val description: String? = null
    override val default: T? = null
    override val nullable: Boolean = false
    override val example: T? = null
    override val deprecated: Boolean = false
}
