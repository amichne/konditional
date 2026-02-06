package io.amichne.konditional.openapi

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
import io.amichne.kontracts.schema.ObjectTraits
import io.amichne.kontracts.schema.OneOfSchema
import io.amichne.kontracts.schema.StringSchema

internal object OpenApiSchemaConverter {
    fun toSchema(schema: JsonSchema<*>): Map<String, Any?> =
        openApiProps(schema) + schemaDetails(schema)
}

private fun schemaDetails(schema: JsonSchema<*>): Map<String, Any?> =
    primitiveDetails(schema)
        ?: objectDetails(schema as? ObjectTraits)
        ?: collectionDetails(schema)
        ?: compositionDetails(schema)
        ?: emptyMap()

private fun primitiveDetails(schema: JsonSchema<*>): Map<String, Any?>? =
    when (schema) {
        is BooleanSchema -> mapOf("type" to "boolean")
        is StringSchema -> stringDetails(schema)
        is IntSchema -> intDetails(schema)
        is DoubleSchema -> doubleDetails(schema)
        is EnumSchema<*> -> mapOf("type" to "string", "enum" to schema.values.map { it.name })
        is NullSchema -> mapOf("nullable" to true)
        else -> null
    }

private fun stringDetails(schema: StringSchema): Map<String, Any?> =
    buildMap {
        put("type", "string")
        schema.minLength?.let { put("minLength", it) }
        schema.maxLength?.let { put("maxLength", it) }
        schema.pattern?.let { put("pattern", it) }
        schema.format?.let { put("format", it) }
        schema.enum?.let { put("enum", it) }
    }

private fun intDetails(schema: IntSchema): Map<String, Any?> =
    buildMap {
        put("type", "integer")
        put("format", "int32")
        schema.minimum?.let { put("minimum", it) }
        schema.maximum?.let { put("maximum", it) }
        schema.enum?.let { put("enum", it) }
    }

private fun doubleDetails(schema: DoubleSchema): Map<String, Any?> =
    buildMap {
        put("type", "number")
        put("format", schema.format ?: "double")
        schema.minimum?.let { put("minimum", it) }
        schema.maximum?.let { put("maximum", it) }
        schema.enum?.let { put("enum", it) }
    }

private fun objectDetails(schema: ObjectTraits?): Map<String, Any?>? =
    schema?.let { objectSchema ->
        buildMap {
            put("type", "object")
            put("additionalProperties", false)
            put("properties", objectSchema.fields.mapValues { (_, field) -> toSchema(field) })

            val required =
                objectSchema.required?.toList()
                    ?: objectSchema.fields
                        .filter { (_, field) -> field.required }
                        .keys
                        .toList()
            if (required.isNotEmpty()) {
                put("required", required)
            }
        }
    }

private fun collectionDetails(schema: JsonSchema<*>): Map<String, Any?>? =
    when (schema) {
        is ArraySchema<*> ->
            buildMap {
                put("type", "array")
                put("items", OpenApiSchemaConverter.toSchema(schema.elementSchema))
                schema.minItems?.let { put("minItems", it) }
                schema.maxItems?.let { put("maxItems", it) }
                if (schema.uniqueItems) {
                    put("uniqueItems", true)
                }
            }

        is MapSchema<*> ->
            buildMap {
                put("type", "object")
                put("additionalProperties", OpenApiSchemaConverter.toSchema(schema.valueSchema))
                schema.minProperties?.let { put("minProperties", it) }
                schema.maxProperties?.let { put("maxProperties", it) }
            }

        else -> null
    }

private fun compositionDetails(schema: JsonSchema<*>): Map<String, Any?>? =
    when (schema) {
        is OneOfSchema ->
            buildMap {
                put("oneOf", schema.options.map(OpenApiSchemaConverter::toSchema))
                schema.discriminator?.let { discriminator ->
                    put(
                        "discriminator",
                        mapOf(
                            "propertyName" to discriminator.propertyName,
                            "mapping" to discriminator.mapping,
                        ),
                    )
                }
            }

        is AllOfSchema -> mapOf("allOf" to schema.options.map(OpenApiSchemaConverter::toSchema))
        is AnySchema -> emptyMap()
        else -> null
    }

private fun toSchema(fieldSchema: FieldSchema): Map<String, Any?> =
    buildMap {
        putAll(OpenApiSchemaConverter.toSchema(fieldSchema.schema))
        fieldSchema.description?.let { put("description", it) }
        fieldSchema.defaultValue?.let { put("default", it) }
        if (fieldSchema.deprecated) {
            put("deprecated", true)
        }
    }

private fun openApiProps(schema: JsonSchema<*>): Map<String, Any?> =
    buildMap {
        schema.title?.let { put("title", it) }
        schema.description?.let { put("description", it) }
        schema.default?.let { put("default", it) }
        schema.example?.let { put("example", it) }
        if (schema.nullable) {
            put("nullable", true)
        }
        if (schema.deprecated) {
            put("deprecated", true)
        }
    }
