package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.FieldSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ObjectSchema

@JsonSchemaBuilderDsl
class RootObjectSchemaBuilder {
    @PublishedApi
    internal val fields: MutableMap<String, FieldSchema> = mutableMapOf()

    @PublishedApi
    internal var schema: JsonSchema<*>? = null

    fun build(): ObjectSchema = when (val builtSchema = schema) {
        is ObjectSchema -> builtSchema
        null -> ObjectSchema(fields.toMap())
        else -> throw IllegalStateException("Top-level schema must be an ObjectSchema")
    }
}
