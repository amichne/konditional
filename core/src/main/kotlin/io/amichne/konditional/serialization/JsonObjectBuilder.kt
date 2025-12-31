package io.amichne.konditional.serialization

import io.amichne.kontracts.value.JsonArray
import io.amichne.kontracts.value.JsonBoolean
import io.amichne.kontracts.value.JsonNull
import io.amichne.kontracts.value.JsonNumber
import io.amichne.kontracts.value.JsonObject
import io.amichne.kontracts.value.JsonString
import io.amichne.kontracts.value.JsonValue

/**
 * DSL builder for constructing JsonObject instances without reflection.
 *
 * Provides a clean, type-safe way to build JSON objects for custom serializers.
 *
 * ## Usage
 *
 * ```kotlin
 * val json = jsonObject {
 *     "name" to "Alice"
 *     "age" to 30
 *     "active" to true
 *     "score" to 95.5
 *     "settings" to jsonObject {
 *         "theme" to "dark"
 *         "notifications" to false
 *     }
 *     "tags" to jsonArray("kotlin", "feature-flags", "type-safety")
 * }
 * ```
 *
 * ## Type Conversions
 *
 * - `String` → `JsonString`
 * - `Int` → `JsonNumber`
 * - `Double` → `JsonNumber`
 * - `Boolean` → `JsonBoolean`
 * - `null` → `JsonNull`
 * - `JsonValue` → passed through
 * - Lists → `JsonArray`
 */
class JsonObjectBuilder {
    private val fields = mutableMapOf<String, JsonValue>()

    /**
     * Adds a field to the JSON object.
     *
     * Supports automatic conversion from Kotlin types to JsonValue.
     */
    infix fun String.to(value: Any?) {
        fields[this] = value.toJsonValue()
    }

    /**
     * Builds the JsonObject.
     */
    internal fun build(): JsonObject = JsonObject(fields, schema = null)
}

/**
 * Constructs a JsonObject using DSL syntax.
 *
 * ```kotlin
 * val obj = jsonObject {
 *     "key" to "value"
 *     "count" to 42
 * }
 * ```
 */
fun jsonObject(builder: JsonObjectBuilder.() -> Unit): JsonObject {
    return JsonObjectBuilder().apply(builder).build()
}

/**
 * Constructs a JsonArray from a list of values.
 *
 * ```kotlin
 * val arr = jsonArray("a", "b", "c")
 * val arr2 = jsonArray(1, 2, 3)
 * ```
 */
fun jsonArray(vararg values: Any?): JsonArray {
    return JsonArray(values.map { it.toJsonValue() }, elementSchema = null)
}

/**
 * Converts a Kotlin value to a JsonValue.
 *
 * Handles primitives, nested objects, and arrays.
 */
private fun Any?.toJsonValue(): JsonValue = when (this) {
    null -> JsonNull
    is Boolean -> JsonBoolean(this)
    is String -> JsonString(this)
    is Int -> JsonNumber(this.toDouble())
    is Double -> JsonNumber(this)
    is JsonValue -> this
    is List<*> -> JsonArray(map { it.toJsonValue() }, elementSchema = null)
    is Map<*, *> -> {
        JsonObject(
            fields = entries.associate { (k, v) ->
                (k as? String ?: error("Map keys must be strings")) to v.toJsonValue()
            },
            schema = null
        )
    }
    else -> error("Unsupported type for JSON conversion: ${this::class.simpleName}")
}

/**
 * Extension functions for extracting typed values from JsonValue.
 *
 * These provide safe access to JSON values with proper error handling.
 */

/**
 * Extracts an Int from a JsonValue.
 * Returns null if the value is not a JsonNumber or cannot be converted to Int.
 */
fun JsonValue.asInt(): Int? = when (this) {
    is JsonNumber -> toInt()
    else -> null
}

/**
 * Extracts a Double from a JsonValue.
 * Returns null if the value is not a JsonNumber.
 */
fun JsonValue.asDouble(): Double? = when (this) {
    is JsonNumber -> toDouble()
    else -> null
}

/**
 * Extracts a String from a JsonValue.
 * Returns null if the value is not a JsonString.
 */
fun JsonValue.asString(): String? = when (this) {
    is JsonString -> value
    else -> null
}

/**
 * Extracts a Boolean from a JsonValue.
 * Returns null if the value is not a JsonBoolean.
 */
fun JsonValue.asBoolean(): Boolean? = when (this) {
    is JsonBoolean -> value
    else -> null
}

/**
 * Extracts a JsonObject from a JsonValue.
 * Returns null if the value is not a JsonObject.
 */
fun JsonValue.asObject(): JsonObject? = this as? JsonObject

/**
 * Extracts a JsonArray from a JsonValue.
 * Returns null if the value is not a JsonArray.
 */
fun JsonValue.asArray(): JsonArray? = this as? JsonArray
