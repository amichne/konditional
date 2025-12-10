package io.amichne.konditional.core.dsl.json

import io.amichne.konditional.core.types.json.JsonSchema

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
    private val fields = mutableMapOf<String, JsonFieldSchemaBuilder.() -> JsonSchema.FieldSchema<*>>()

    /**
     * Adds a field to the object definition.
     *
     * @param name The field name
     * @param schema Lambda to build the field's definition
     */
    internal inline fun <reified S : JsonSchema> field(
        name: String,
        crossinline schema: JsonFieldSchemaBuilder.() -> JsonSchema.FieldSchema<S>,
    ) {
        updateFieldMap(name,) { schema() }
    }

    /**
     * Adds a field to the object definition (convenience overload for raw schemas).
     *
     * @param name The field name
     * @param required Whether the field is required (default: false)
     * @param default Default value for the field
     * @param schema Lambda to build the field's definition
     */
    @JvmName("fieldWithDefaults")
    inline fun <reified S : JsonSchema> fieldRaw(
        name: String,
        required: Boolean = false,
        default: Any? = null,
        crossinline schema: JsonFieldSchemaBuilder.() -> S,
    ) {
        updateFieldMap(name) {
            JsonSchema.FieldSchema(schema(), required, default)
        }
    }

    @PublishedApi
    internal fun updateFieldMap(
        name: String,
        schemaBuilder: JsonFieldSchemaBuilder.() -> JsonSchema.FieldSchema<*>
    ) {
        fields[name] = schemaBuilder
    }



    /**
     * Builds the final ObjectSchema.
     */
    fun build(): JsonSchema.ObjectSchema =
        JsonSchema.ObjectSchema(fields.mapValues { it.value(JsonFieldSchemaBuilder()) }.toMap())
}
