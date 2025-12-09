package io.amichne.konditional.core.dsl

import io.amichne.konditional.core.types.json.JsonSchema
import io.amichne.konditional.core.types.json.JsonValue

/**
 * Top-level function to create a JSON object schema using DSL.
 */
fun jsonObject(builder: JsonObjectSchemaBuilder.() -> Unit): JsonSchema.ObjectSchema {
    return JsonObjectSchemaBuilder().apply(builder).build()
}

// ========== JsonValue Builder DSL ==========

/**
 * Top-level function to build a JSON object value.
 */
fun buildJsonObject(
    schema: JsonSchema.ObjectSchema? = null,
    builder: JsonObjectBuilder.() -> Unit
): JsonValue.JsonObject {
    return JsonObjectBuilder(schema).apply(builder).build()
}

/**
 * Builds an empty JSON array with a specified element schema.
 *
 * Example:
 * ```kotlin
 * val emptyStringArray = buildJsonArray { string() }
 * val emptyIntArray = buildJsonArray { int() }
 * ```
 */
fun buildJsonArray(
    elementSchemaBuilder: JsonFieldSchemaBuilder.() -> JsonSchema
): JsonValue.JsonArray {
    val elementSchema = JsonFieldSchemaBuilder().elementSchemaBuilder()
    return JsonValue.JsonArray(emptyList(), elementSchema)
}

/**
 * Builds a JSON array from varargs.
 */
fun buildJsonArray(
    vararg elements: JsonValue,
    elementSchema: JsonSchema? = null
): JsonValue.JsonArray {
    return JsonValue.JsonArray(elements.toList(), elementSchema)
}

/**
 * Builds a JSON array from a list.
 */
fun buildJsonArray(
    elements: List<JsonValue>,
    elementSchema: JsonSchema? = null
): JsonValue.JsonArray {
    return JsonValue.JsonArray(elements, elementSchema)
}

/**
 * Builds a JSON array of strings.
 */
fun buildJsonArray(vararg strings: String): JsonValue.JsonArray {
    return JsonValue.JsonArray(strings.map { JsonValue.JsonString(it) }, JsonSchema.StringSchema)
}

/**
 * Builds a JSON array of integers.
 */
fun buildJsonArray(vararg ints: Int): JsonValue.JsonArray {
    return JsonValue.JsonArray(ints.map { JsonValue.JsonNumber(it.toDouble()) }, JsonSchema.IntSchema)
}

/**
 * Builds a JSON array of booleans.
 */
fun buildJsonArray(vararg booleans: Boolean): JsonValue.JsonArray {
    return JsonValue.JsonArray(booleans.map { JsonValue.JsonBoolean(it) }, JsonSchema.BooleanSchema)
}

/**
 * Builds a JSON array of doubles.
 */
fun buildJsonArray(vararg doubles: Double): JsonValue.JsonArray {
    return JsonValue.JsonArray(doubles.map { JsonValue.JsonNumber(it) }, JsonSchema.DoubleSchema)
}

/**
 * Builds a JSON array of objects with a shared schema.
 *
 * Example:
 * ```kotlin
 * val userSchema = jsonObject {
 *     field("id") { int() }
 *     field("name") { string() }
 * }
 *
 * val users = buildJsonObjectArray(schema = userSchema) {
 *     add {
 *         "id" to 1
 *         "name" to "Alice"
 *     }
 *     add {
 *         "id" to 2
 *         "name" to "Bob"
 *     }
 * }
 * ```
 */
fun buildJsonObjectArray(
    schema: JsonSchema.ObjectSchema,
    builder: JsonObjectArrayBuilder.() -> Unit
): JsonValue.JsonArray {
    return JsonObjectArrayBuilder(schema).apply(builder).build()
}
