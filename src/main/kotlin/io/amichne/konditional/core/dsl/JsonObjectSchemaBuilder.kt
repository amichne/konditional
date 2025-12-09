package io.amichne.konditional.core.dsl

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
     * Adds a required field to the object schema.
     * Convenience method for field(name, required = true, schemaBuilder).
     *
     * @param name The field name
     * @param schemaBuilder Lambda to build the field's schema
     */
    fun requiredField(
        name: String,
        schemaBuilder: JsonFieldSchemaBuilder.() -> JsonSchema
    ) {
        field(name, required = true, schemaBuilder = schemaBuilder)
    }

    /**
     * Adds an optional field to the object schema.
     * Convenience method for field(name, required = false, schemaBuilder).
     *
     * @param name The field name
     * @param schemaBuilder Lambda to build the field's schema
     */
    fun optionalField(
        name: String,
        schemaBuilder: JsonFieldSchemaBuilder.() -> JsonSchema
    ) {
        field(name, required = false, schemaBuilder = schemaBuilder)
    }

    /**
     * Builds the final ObjectSchema.
     */
    fun build(): JsonSchema.ObjectSchema = JsonSchema.ObjectSchema(fields.toMap())
}
