package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.value.JsonBoolean
import io.amichne.kontracts.value.JsonNull
import io.amichne.kontracts.value.JsonNumber
import io.amichne.kontracts.value.JsonObject
import io.amichne.kontracts.value.JsonString
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
class JsonObjectBuilder(private val schema: ObjectSchema? = null) {
    private val fields = mutableMapOf<String, JsonValue>()

    /**
     * Adds a boolean field.
     */
    infix fun String.to(value: Boolean) {
        fields[this] = JsonBoolean(value)
    }

    /**
     * Adds a string field.
     */
    infix fun String.to(value: String) {
        fields[this] = JsonString(value)
    }

    /**
     * Adds an integer field.
     */
    infix fun String.to(value: Int) {
        fields[this] = JsonNumber(value.toDouble())
    }

    /**
     * Adds a double field.
     */
    infix fun String.to(value: Double) {
        fields[this] = JsonNumber(value)
    }

    /**
     * Adds an enum field.
     */
    infix fun <E : Enum<E>> String.to(value: E) {
        fields[this] = JsonString(value.name)
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
        fields[this] = JsonNull
    }

    /**
     * Builds the final JsonObject.
     */
    fun build(): JsonObject = JsonObject(fields.toMap(), schema)
}
