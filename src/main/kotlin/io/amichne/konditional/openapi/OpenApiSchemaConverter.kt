package io.amichne.konditional.openapi

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
import io.amichne.kontracts.schema.OpenApiProps
import io.amichne.kontracts.schema.RootObjectSchema
import io.amichne.kontracts.schema.StringSchema

internal object OpenApiSchemaConverter {
    fun toSchema(schema: JsonSchema): Map<String, Any?> =
        buildMap {
            addOpenApiProps(this, schema)
            when (schema) {
                is BooleanSchema -> put("type", "boolean")
                is StringSchema -> {
                    put("type", "string")
                    schema.minLength?.let { put("minLength", it) }
                    schema.maxLength?.let { put("maxLength", it) }
                    schema.pattern?.let { put("pattern", it) }
                    schema.format?.let { put("format", it) }
                    schema.enum?.let { put("enum", it) }
                }
                is IntSchema -> {
                    put("type", "integer")
                    put("format", "int32")
                    schema.minimum?.let { put("minimum", it) }
                    schema.maximum?.let { put("maximum", it) }
                    schema.enum?.let { put("enum", it) }
                }
                is DoubleSchema -> {
                    put("type", "number")
                    put("format", schema.format ?: "double")
                    schema.minimum?.let { put("minimum", it) }
                    schema.maximum?.let { put("maximum", it) }
                    schema.enum?.let { put("enum", it) }
                }
                is EnumSchema<*> -> {
                    put("type", "string")
                    put("enum", schema.values.map { it.name })
                }
                is ArraySchema -> {
                    put("type", "array")
                    put("items", toSchema(schema.elementSchema))
                    schema.minItems?.let { put("minItems", it) }
                    schema.maxItems?.let { put("maxItems", it) }
                    if (schema.uniqueItems) {
                        put("uniqueItems", true)
                    }
                }
                is ObjectSchema -> addObjectSchema(this, schema)
                is RootObjectSchema -> addObjectSchema(this, schema)
                is NullSchema -> put("nullable", true)
                is MapSchema -> addMapSchema(this, schema)
                is OneOfSchema -> put("oneOf", schema.options.map { toSchema(it) })
                is AnySchema -> Unit
            }
        }

    private fun addObjectSchema(
        target: MutableMap<String, Any?>,
        schema: ObjectTraits,
    ) {
        target["type"] = "object"
        target["additionalProperties"] = false
        target["properties"] = schema.fields.mapValues { (_, field) -> toSchema(field) }
        val required = schema.required?.toList() ?: schema.fields.filter { it.value.required }.keys.toList()
        if (required.isNotEmpty()) {
            target["required"] = required
        }
    }

    private fun addMapSchema(
        target: MutableMap<String, Any?>,
        schema: MapSchema,
    ) {
        target["type"] = "object"
        target["additionalProperties"] = toSchema(schema.valueSchema)
        schema.minProperties?.let { target["minProperties"] = it }
        schema.maxProperties?.let { target["maxProperties"] = it }
    }

    private fun toSchema(fieldSchema: FieldSchema): Map<String, Any?> =
        buildMap {
            putAll(toSchema(fieldSchema.schema))
            fieldSchema.description?.let { put("description", it) }
            fieldSchema.defaultValue?.let { put("default", it) }
            if (fieldSchema.deprecated) {
                put("deprecated", true)
            }
        }

    private fun addOpenApiProps(
        target: MutableMap<String, Any?>,
        props: OpenApiProps,
    ) {
        props.title?.let { target["title"] = it }
        props.description?.let { target["description"] = it }
        props.default?.let { target["default"] = it }
        props.example?.let { target["example"] = it }
        if (props.nullable) {
            target["nullable"] = true
        }
        if (props.deprecated) {
            target["deprecated"] = true
        }
    }
}
