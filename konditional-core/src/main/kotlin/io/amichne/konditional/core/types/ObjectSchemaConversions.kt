package io.amichne.konditional.core.types

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.kontracts.dsl.objectSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.schema.RootObjectSchema

@KonditionalInternalApi
fun JsonSchema<*>.asObjectSchema(): ObjectSchema =
    when (this) {
        is ObjectSchema -> this
        is RootObjectSchema ->
            objectSchema {
                fields = this@asObjectSchema.fields
                title = this@asObjectSchema.title
                description = this@asObjectSchema.description
                default = this@asObjectSchema.default
                nullable = this@asObjectSchema.nullable
                example = this@asObjectSchema.example
                deprecated = this@asObjectSchema.deprecated
                required = this@asObjectSchema.required
            }
        else -> throw IllegalArgumentException("Expected an object schema, got ${this::class.qualifiedName}")
    }
