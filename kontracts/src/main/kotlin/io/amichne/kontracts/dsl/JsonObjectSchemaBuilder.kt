package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.JsonSchema

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
    @PublishedApi
    internal val fields = mutableMapOf<String, JsonSchema.FieldSchema>()

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
        schemaBuilder: JsonFieldSchemaBuilder.() -> JsonSchema,
    ) {
        val schema = JsonFieldSchemaBuilder().schemaBuilder()
        fields[name] = JsonSchema.FieldSchema(schema, required, schema.default)
    }

    /**
     * Builds the final ObjectSchema.
     */
    fun build(): JsonSchema.ObjectSchema = JsonSchema.ObjectSchema(fields.toMap())
}
