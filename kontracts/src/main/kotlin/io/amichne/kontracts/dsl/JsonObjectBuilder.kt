package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.value.JsonValue

/**
 * Builder for creating JSON object values using a type-safe DSL.
 *
 * Example:
 * ```kotlin
 * val user = buildJsonObject {
 *     "id" to 123
 *     "name" to "John Doe"
 *     "email" to "john@example.com"
 *     "settings" to buildJsonObject {
 *         "theme" to "dark"
 *         "notifications" to true
 *     }
 *     "tags" to buildJsonArray("kotlin", "programming", "feature-flags")
 * }
 * ```
 */
@JsonSchemaDsl
class JsonObjectBuilder(private val schema: JsonSchema.ObjectSchema? = null) {
    private val fields = mutableMapOf<String, JsonValue>()

    /**
     * Adds a boolean field.
     */
    infix fun String.to(value: Boolean) {
        fields[this] = JsonValue.JsonBoolean(value)
    }

    /**
     * Adds a string field.
     */
    infix fun String.to(value: String) {
        fields[this] = JsonValue.JsonString(value)
    }

    /**
     * Adds an integer field.
     */
    infix fun String.to(value: Int) {
        fields[this] = JsonValue.JsonNumber(value.toDouble())
    }

    /**
     * Adds a double field.
     */
    infix fun String.to(value: Double) {
        fields[this] = JsonValue.JsonNumber(value)
    }

    /**
     * Adds an enum field.
     */
    infix fun <E : Enum<E>> String.to(value: E) {
        fields[this] = JsonValue.JsonString(value.name)
    }

    /**
     * Adds a JsonValue field.
     */
    infix fun String.to(value: JsonValue) {
        fields[this] = value
    }

    /**
     * Adds a null field.
     */
    infix fun String.toNull(@Suppress("UNUSED_PARAMETER") unit: Unit) {
        fields[this] = JsonValue.JsonNull
    }

    /**
     * Builds the final JsonObject.
     */
    fun build(): JsonValue.JsonObject = JsonValue.JsonObject(fields.toMap(), schema)
}
