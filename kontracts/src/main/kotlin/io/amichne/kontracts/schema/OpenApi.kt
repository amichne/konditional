package io.amichne.kontracts.schema

/**
 * Base interface for OpenAPI schema representation.
 *
 * @param T
 * @constructor Create empty Open api
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
     * JSON Schema data types, as per OpenAPI Specification.
     *
     * @property serialized The serialized string representation of the type.
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
