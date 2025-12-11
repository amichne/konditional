package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.JsonSchema

/**
 * Builder for creating field schemas within a JSON object.
 */
@JsonSchemaDsl
class JsonFieldSchemaBuilder {

    /**
     * Creates a boolean schema.
     */
    fun boolean(): JsonSchema.BooleanSchema = JsonSchema.BooleanSchema()

    /**
     * Creates a string schema.
     */
    fun string(
        block: JsonSchema.StringSchema.() -> Unit = {},
    ): JsonSchema.StringSchema = JsonSchema.StringSchema()

    /**
     * Creates an integer schema.
     */
    fun int(): JsonSchema.IntSchema = JsonSchema.IntSchema()

    /**
     * Creates a double schema.
     */
    fun double(): JsonSchema.DoubleSchema = JsonSchema.DoubleSchema()

    /**
     * Creates an enum schema.
     */
    inline fun <reified E : Enum<E>> enum(): JsonSchema.EnumSchema<E> =
        JsonSchema.EnumSchema(enumClass = E::class, values = enumValues<E>().toList())

    /**
     * Creates a null schema.
     */
    fun nullSchema(): JsonSchema.NullSchema = JsonSchema.NullSchema()

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
