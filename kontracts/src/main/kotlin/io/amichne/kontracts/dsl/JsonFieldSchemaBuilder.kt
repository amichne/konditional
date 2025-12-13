package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.ArraySchema
import io.amichne.kontracts.schema.BooleanSchema
import io.amichne.kontracts.schema.DoubleSchema
import io.amichne.kontracts.schema.EnumSchema
import io.amichne.kontracts.schema.IntSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.NullSchema
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.schema.StringSchema

/**
 * Builder for creating field schemas within a JSON object.
 */
@JsonSchemaDsl
class JsonFieldSchemaBuilder {

    /**
     * Creates a boolean schema.
     */
    fun boolean(): BooleanSchema = BooleanSchema()

    /**
     * Creates a string schema.
     */
    fun string(
        block: StringSchema.() -> Unit = {},
    ): StringSchema = StringSchema()

    /**
     * Creates an integer schema.
     */
    fun int(): IntSchema = IntSchema()

    /**
     * Creates a double schema.
     */
    fun double(): DoubleSchema = DoubleSchema()

    /**
     * Creates an enum schema.
     */
    inline fun <reified E : Enum<E>> enum(): EnumSchema<E> =
        EnumSchema(enumClass = E::class, values = enumValues<E>().toList())

    /**
     * Creates a null schema.
     */
    fun nullSchema(): NullSchema = NullSchema()

    /**
     * Creates a nested object schema.
     */
    fun jsonObject(builder: JsonObjectSchemaBuilder.() -> Unit): ObjectSchema {
        return JsonObjectSchemaBuilder().apply(builder).build()
    }

    /**
     * Creates an array schema.
     */
    fun array(elementBuilder: JsonFieldSchemaBuilder.() -> JsonSchema): ArraySchema {
        val elementSchema = JsonFieldSchemaBuilder().elementBuilder()
        return ArraySchema(elementSchema)
    }
}
