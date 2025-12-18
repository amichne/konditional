package io.amichne.konditional.core.types

import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.schema.RootObjectSchema

@PublishedApi
internal fun JsonSchema.asObjectSchema(): ObjectSchema =
    when (this) {
        is ObjectSchema -> this
        is RootObjectSchema ->
            ObjectSchema(
                fields = fields,
                title = title,
                description = description,
                default = default,
                nullable = nullable,
                example = example,
                deprecated = deprecated,
                required = required,
            )
        else -> throw IllegalArgumentException("Expected an object schema, got ${this::class.qualifiedName}")
    }

