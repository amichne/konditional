package io.amichne.konditional.core.dsl.json

import io.amichne.konditional.core.types.json.JsonSchema

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
    fun <S : JsonSchema> array(elementBuilder: JsonFieldSchemaBuilder.() -> S): JsonSchema.ArraySchema<S> {
        val elementSchema = JsonFieldSchemaBuilder().elementBuilder()
        return JsonSchema.ArraySchema(elementSchema)
    }
}
