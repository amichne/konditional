package io.amichne.konditional.core.dsl.json

import io.amichne.konditional.core.types.json.JsonSchema

/**
 * Builder for creating field schemas within a JSON object.
 */
@JsonSchemaDsl
class JsonFieldSchemaBuilder {

    /**
     * Creates a boolean definition.
     */
    fun boolean(): JsonSchema.BooleanSchema = JsonSchema.BooleanSchema

    /**
     * Creates a string definition.
     */
    fun string(): JsonSchema.StringSchema = JsonSchema.StringSchema

    /**
     * Creates an integer definition.
     */
    fun int(): JsonSchema.IntSchema = JsonSchema.IntSchema

    /**
     * Creates a double definition.
     */
    fun double(): JsonSchema.DoubleSchema = JsonSchema.DoubleSchema

    /**
     * Creates an enum definition.
     */
    inline fun <reified E : Enum<E>> enum(): JsonSchema.EnumSchema<E> =
        JsonSchema.EnumSchema(E::class)

    /**
     * Creates a null definition.
     */
    fun nullSchema(): JsonSchema.NullSchema = JsonSchema.NullSchema

    /**
     * Creates a nested object definition.
     */
    fun jsonObject(builder: JsonObjectSchemaBuilder.() -> Unit): JsonSchema.ObjectSchema {
        return JsonObjectSchemaBuilder().apply(builder).build()
    }

    /**
     * Creates an array definition.
     */
    fun <S : JsonSchema> array(elementBuilder: JsonFieldSchemaBuilder.() -> S): JsonSchema.ArraySchema<S> {
        val elementSchema = JsonFieldSchemaBuilder().elementBuilder()
        return JsonSchema.ArraySchema(elementSchema)
    }
}
