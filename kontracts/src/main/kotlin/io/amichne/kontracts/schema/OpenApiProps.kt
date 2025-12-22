package io.amichne.kontracts.schema

/**
 * Base interface for OpenAPI-esque properties.
 */
interface OpenApiProps<out T : Any> {
    val title: String?
    val description: String?
    val default: T?
    val nullable: Boolean
    val example: T?
    val deprecated: Boolean
}
