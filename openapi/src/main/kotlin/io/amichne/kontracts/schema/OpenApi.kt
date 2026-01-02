package io.amichne.kontracts.schema

/**
 * Base interface for OpenAPI-esque properties.
 */
interface OpenApi<out T : Any> {
    val type: Type
    val title: String?
    val description: String?
    val default: T?
    val nullable: Boolean
    val example: T?
    val deprecated: Boolean

    /**
     * OpenAPI/JSON Schema type identifiers.
     */
    enum class Type(val serialized: String) {
        STRING("string"),
        INTEGER("integer"),
        NUMBER("number"),
        BOOLEAN("boolean"),
        ARRAY("array"),
        OBJECT("object"),
        NULL("null")
    }
}
