package io.amichne.kontracts.schema

/**
 * Base interface for OpenAPI-esque properties.
 */
interface OpenApiProps {
    val title: String?
    val description: String?
    val default: Any?
    val nullable: Boolean
    val example: Any?
    val deprecated: Boolean
}
