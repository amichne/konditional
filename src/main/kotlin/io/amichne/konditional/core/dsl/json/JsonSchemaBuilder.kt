package io.amichne.konditional.core.dsl.json

import io.amichne.konditional.core.types.json.JsonSchema
import io.amichne.konditional.core.types.json.JsonValue

/**
 * Top-level function to create a JSON object schema using DSL.
 */
fun jsonObject(builder: JsonObjectSchemaBuilder.() -> Unit): JsonSchema.ObjectSchema {
    return JsonObjectSchemaBuilder().apply(builder).build()
}

/**
 * Top-level function to build a JSON object value.
 */
internal fun buildJsonObject(
    schema: JsonSchema.ObjectSchema? = null,
    builder: JsonObjectBuilder.() -> Unit
): JsonValue.JsonObject {
    return JsonObjectBuilder(schema).apply(builder).build()
}

internal fun buildJsonArray(
    builder: JsonFieldSchemaBuilder.() -> JsonSchema
): JsonValue.JsonArray {
    return JsonValue.JsonArray(
        emptyList(),
        JsonSchema.ArraySchema(JsonFieldSchemaBuilder().array { builder() })
    )
}

/**
 * Builds a JSON array from varargs.
 */
internal fun buildJsonArray(
    vararg elements: JsonValue,
    elementSchema: JsonSchema? = null
): JsonValue.JsonArray {
    return JsonValue.JsonArray(elements.toList(), elementSchema)
}

/**
 * Builds a JSON array from a list.
 */
internal fun buildJsonArray(
    elements: List<JsonValue>,
    elementSchema: JsonSchema? = null
): JsonValue.JsonArray {
    return JsonValue.JsonArray(elements, elementSchema)
}

/**
 * Builds a JSON array of strings.
 */
internal fun buildJsonArray(vararg strings: String): JsonValue.JsonArray {
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
