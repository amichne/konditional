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
    private val fields = mutableMapOf<String, JsonFieldSchemaBuilder.() -> JsonSchema.FieldSchema>()

    /**
     * Adds a field to the object schema.
     *
     * @param name The field name
     * @param required Whether the field is required (default: false)
     * @param default Default value for the field
     * @param schema Lambda to build the field's schema
     */
    fun field(
        name: String,
        schema: JsonFieldSchemaBuilder.() -> JsonSchema.FieldSchema,
    ) {
        fields[name] = schema
    }

    /**
     * Builds the final ObjectSchema.
     */
    fun build(): JsonSchema.ObjectSchema =
        JsonSchema.ObjectSchema(fields.mapValues { it.value(JsonFieldSchemaBuilder()) }.toMap())
}
