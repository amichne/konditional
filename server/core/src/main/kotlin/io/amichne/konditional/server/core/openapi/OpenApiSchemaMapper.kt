package io.amichne.konditional.server.core.openapi

import io.amichne.kontracts.schema.AllOfSchema
import io.amichne.kontracts.schema.AnySchema
import io.amichne.kontracts.schema.ArraySchema
import io.amichne.kontracts.schema.BooleanSchema
import io.amichne.kontracts.schema.DoubleSchema
import io.amichne.kontracts.schema.EnumSchema
import io.amichne.kontracts.schema.FieldSchema
import io.amichne.kontracts.schema.IntSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.MapSchema
import io.amichne.kontracts.schema.NullSchema
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.schema.ObjectTraits
import io.amichne.kontracts.schema.OneOfSchema
import io.amichne.kontracts.schema.RefSchema
import io.amichne.kontracts.schema.RootObjectSchema
import io.amichne.kontracts.schema.StringSchema

internal object OpenApiSchemaMapper {
    fun from(schema: JsonSchema<*>): OpenApiSchema =
        when (schema) {
            is RefSchema -> OpenApiSchema(ref = schema.ref)
            is BooleanSchema -> withCommonProperties(schema, OpenApiSchema(type = "boolean"))
            is StringSchema ->
                withCommonProperties(
                    schema,
                    OpenApiSchema(
                        type = "string",
                        minLength = schema.minLength,
                        maxLength = schema.maxLength,
                        pattern = schema.pattern,
                        format = schema.format,
                        enum = schema.enum?.takeIf(List<String>::isNotEmpty)?.map { value -> value },
                    ),
                )

            is IntSchema ->
                withCommonProperties(
                    schema,
                    OpenApiSchema(
                        type = "integer",
                        format = "int32",
                        minimum = schema.minimum,
                        maximum = schema.maximum,
                        enum = schema.enum?.takeIf(List<Int>::isNotEmpty)?.map { value -> value },
                    ),
                )

            is DoubleSchema ->
                withCommonProperties(
                    schema,
                    OpenApiSchema(
                        type = "number",
                        format = schema.format,
                        minimum = schema.minimum,
                        maximum = schema.maximum,
                        enum = schema.enum?.takeIf(List<Double>::isNotEmpty)?.map { value -> value },
                    ),
                )

            is EnumSchema<*> ->
                withCommonProperties(
                    schema,
                    OpenApiSchema(
                        type = "string",
                        enum = schema.values.map { value -> value.name },
                    ),
                )

            is NullSchema -> withCommonProperties(schema, OpenApiSchema(type = "null", nullable = true))
            is AnySchema -> withCommonProperties(schema, OpenApiSchema(type = "object"))
            is ArraySchema<*> ->
                withCommonProperties(
                    schema,
                    OpenApiSchema(
                        type = "array",
                        items = from(schema.elementSchema),
                        minItems = schema.minItems,
                        maxItems = schema.maxItems,
                        uniqueItems = schema.uniqueItems.takeIf { it },
                    ),
                )

            is MapSchema<*> ->
                withCommonProperties(
                    schema,
                    OpenApiSchema(
                        type = "object",
                        additionalProperties = from(schema.valueSchema),
                        minProperties = schema.minProperties,
                        maxProperties = schema.maxProperties,
                    ),
                )

            is ObjectSchema -> encodeObjectSchema(schema = schema, objectTraits = schema)
            is RootObjectSchema -> encodeObjectSchema(schema = schema, objectTraits = schema)
            is OneOfSchema ->
                withCommonProperties(
                    schema,
                    OpenApiSchema(
                        oneOf = schema.options.map(::from),
                        discriminator =
                            schema.discriminator?.let { discriminator ->
                                OpenApiDiscriminator(
                                    propertyName = discriminator.propertyName,
                                    mapping =
                                        discriminator.mapping
                                            .toSortedMap()
                                            .entries
                                            .associateTo(linkedMapOf()) { (key, value) -> key to value },
                                )
                            },
                    ),
                )

            is AllOfSchema ->
                withCommonProperties(
                    schema,
                    OpenApiSchema(
                        allOf = schema.options.map(::from),
                    ),
                )
        }

    private fun encodeObjectSchema(
        schema: JsonSchema<*>,
        objectTraits: ObjectTraits,
    ): OpenApiSchema {
        val requiredFields =
            (objectTraits.required ?: objectTraits.fields.filterValues(FieldSchema::required).keys)
                .toSortedSet()
                .toList()

        val properties =
            objectTraits.fields
                .toSortedMap()
                .entries
                .associateTo(linkedMapOf()) { (fieldName, fieldSchema) ->
                    fieldName to encodeFieldSchema(fieldSchema)
                }

        return withCommonProperties(
            schema,
            OpenApiSchema(
                type = "object",
                additionalProperties = false,
                properties = properties,
                required = requiredFields.takeIf(List<String>::isNotEmpty),
            ),
        )
    }

    private fun encodeFieldSchema(fieldSchema: FieldSchema): OpenApiSchema {
        val base = from(fieldSchema.schema)
        val defaultValue = fieldSchema.defaultValue?.let(::normalizeScalar) ?: base.default
        val description = fieldSchema.description ?: base.description
        val deprecated = fieldSchema.deprecated.takeIf { it } ?: base.deprecated
        return base.copy(default = defaultValue, description = description, deprecated = deprecated)
    }

    private fun withCommonProperties(
        schema: JsonSchema<*>,
        openApiSchema: OpenApiSchema,
    ): OpenApiSchema =
        openApiSchema.copy(
            title = schema.title ?: openApiSchema.title,
            description = schema.description ?: openApiSchema.description,
            default = schema.default?.let(::normalizeScalar) ?: openApiSchema.default,
            nullable = schema.nullable.takeIf { it } ?: openApiSchema.nullable,
            example = schema.example?.let(::normalizeScalar) ?: openApiSchema.example,
            deprecated = schema.deprecated.takeIf { it } ?: openApiSchema.deprecated,
        )

    private fun normalizeScalar(value: Any?): Any? =
        when (value) {
            null -> null
            is Enum<*> -> value.name
            is Map<*, *> ->
                value.entries
                    .map { (rawKey, rawValue) -> rawKey.toString() to normalizeScalar(rawValue) }
                    .sortedBy { (key, _) -> key }
                    .associateTo(linkedMapOf()) { (key, normalizedValue) -> key to normalizedValue }

            is List<*> -> value.map(::normalizeScalar)
            else -> value
        }
}
