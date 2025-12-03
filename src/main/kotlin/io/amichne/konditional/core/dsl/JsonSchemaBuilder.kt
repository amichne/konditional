package io.amichne.konditional.core.dsl

import io.amichne.konditional.core.types.JsonSchema
import io.amichne.konditional.core.types.JsonValue

/**
 * DSL marker for JSON schema builders.
 */
@DslMarker
annotation class JsonSchemaDsl

/**
 * Builder for creating JSON object schemas using a type-safe DSL.
 *
 * Example:
 * ```kotlin
 * val userSchema = jsonObject {
 *     field("id", required = true) { int() }
 *     field("name", required = true) { string() }
 *     field("email") { string() }
 *     field("age") { int() }
 *     field("settings") {
 *         jsonObject {
 *             field("theme") { string() }
 *             field("notifications") { boolean() }
 *         }
 *     }
 * }
 * ```
 */
@JsonSchemaDsl
class JsonObjectSchemaBuilder {
    private val fields = mutableMapOf<String, JsonSchema.FieldSchema>()

    /**
     * Adds a field to the object schema.
     *
     * @param name The field name
     * @param required Whether the field is required (default: false)
     * @param default Default value for the field
     * @param schemaBuilder Lambda to build the field's schema
     */
    fun field(
        name: String,
        required: Boolean = false,
        default: Any? = null,
        schemaBuilder: JsonFieldSchemaBuilder.() -> JsonSchema
    ) {
        val schema = JsonFieldSchemaBuilder().schemaBuilder()
        fields[name] = JsonSchema.FieldSchema(schema, required, default)
    }

    /**
     * Builds the final ObjectSchema.
     */
    fun build(): JsonSchema.ObjectSchema = JsonSchema.ObjectSchema(fields.toMap())
}

/**
 * Builder for creating field schemas within a JSON object.
 */
@JsonSchemaDsl
class JsonFieldSchemaBuilder {

    /**
     * Creates a boolean schema.
     */
    fun boolean(): JsonSchema.BooleanSchema = JsonSchema.BooleanSchema

    /**
     * Creates a string schema.
     */
    fun string(): JsonSchema.StringSchema = JsonSchema.StringSchema

    /**
     * Creates an integer schema.
     */
    fun int(): JsonSchema.IntSchema = JsonSchema.IntSchema

    /**
     * Creates a double schema.
     */
    fun double(): JsonSchema.DoubleSchema = JsonSchema.DoubleSchema

    /**
     * Creates an enum schema.
     */
    inline fun <reified E : Enum<E>> enum(): JsonSchema.EnumSchema<E> =
        JsonSchema.EnumSchema(E::class)

    /**
     * Creates a null schema.
     */
    fun nullSchema(): JsonSchema.NullSchema = JsonSchema.NullSchema

    /**
     * Creates a nested object schema.
     */
    fun jsonObject(builder: JsonObjectSchemaBuilder.() -> Unit): JsonSchema.ObjectSchema {
        return JsonObjectSchemaBuilder().apply(builder).build()
    }

    /**
     * Creates an array schema.
     */
    fun array(elementBuilder: JsonFieldSchemaBuilder.() -> JsonSchema): JsonSchema.ArraySchema {
        val elementSchema = JsonFieldSchemaBuilder().elementBuilder()
        return JsonSchema.ArraySchema(elementSchema)
    }
}

/**
 * Top-level function to create a JSON object schema using DSL.
 */
fun jsonObject(builder: JsonObjectSchemaBuilder.() -> Unit): JsonSchema.ObjectSchema {
    return JsonObjectSchemaBuilder().apply(builder).build()
}

// ========== JsonValue Builder DSL ==========

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

/**
 * Top-level function to build a JSON object value.
 */
fun buildJsonObject(
    schema: JsonSchema.ObjectSchema? = null,
    builder: JsonObjectBuilder.() -> Unit
): JsonValue.JsonObject {
    return JsonObjectBuilder(schema).apply(builder).build()
}

fun buildJsonArray(
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
