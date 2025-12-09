package io.amichne.konditional.openapi

import io.amichne.konditional.core.types.json.JsonSchema

/**
 * Converts JSONSchema definitions to OpenAPI 3.0 schema format.
 *
 * This converter creates a one-to-one mapping between Kotlin JSONSchema objects
 * and OpenAPI schema specifications, enabling automatic API documentation generation.
 *
 * Example:
 * ```kotlin
 * val jsonSchema = jsonObject {
 *     requiredField("id") { int() }
 *     requiredField("name") { string() }
 *     optionalField("email") { string() }
 * }
 *
 * val openApiSchema = OpenApiSchemaConverter.convert(jsonSchema)
 * // Produces JSON string with OpenAPI schema
 * ```
 */
object OpenApiSchemaConverter {

    /**
     * Converts a JsonSchema to an OpenAPI 3.0 schema as a Map.
     * This Map can be easily serialized to JSON using any JSON library.
     */
    fun convertToMap(schema: JsonSchema): Map<String, Any> {
        return when (schema) {
            is JsonSchema.BooleanSchema -> mapOf("type" to "boolean")

            is JsonSchema.StringSchema -> mapOf("type" to "string")

            is JsonSchema.IntSchema -> mapOf(
                "type" to "integer",
                "format" to "int32"
            )

            is JsonSchema.DoubleSchema -> mapOf(
                "type" to "number",
                "format" to "double"
            )

            is JsonSchema.NullSchema -> mapOf("type" to "null")

            is JsonSchema.EnumSchema<*> -> {
                val enumValues = schema.enumClass.java.enumConstants.map { it.name }
                mapOf(
                    "type" to "string",
                    "enum" to enumValues,
                    "description" to "Enum: ${schema.enumClass.simpleName}"
                )
            }

            is JsonSchema.ObjectSchema -> {
                val properties = schema.fields.mapValues { (_, fieldSchema) ->
                    convertToMap(fieldSchema.schema)
                }

                buildMap {
                    put("type", "object")
                    put("properties", properties)
                    if (schema.required.isNotEmpty()) {
                        put("required", schema.required.toList())
                    }
                    // Add descriptions for fields with defaults
                    val fieldsWithDefaults = schema.fields.filter { it.value.defaultValue != null }
                    if (fieldsWithDefaults.isNotEmpty()) {
                        put("description", "Fields with defaults: ${fieldsWithDefaults.keys.joinToString()}")
                    }
                }
            }

            is JsonSchema.ArraySchema -> mapOf(
                "type" to "array",
                "items" to convertToMap(schema.elementSchema)
            )
        }
    }

    /**
     * Converts a Map to a JSON string manually (simple implementation).
     */
    fun mapToJson(map: Map<String, Any>, indent: Int = 0): String {
        val indentStr = "  ".repeat(indent)
        val nextIndent = indent + 1

        return buildString {
            append("{\n")
            val entries = map.entries.toList()
            entries.forEachIndexed { index, (key, value) ->
                append("  ".repeat(nextIndent))
                append("\"$key\": ")
                append(valueToJson(value, nextIndent))
                if (index < entries.size - 1) append(",")
                append("\n")
            }
            append(indentStr)
            append("}")
        }
    }

    private fun valueToJson(value: Any, indent: Int = 0): String {
        return when (value) {
            is String -> "\"$value\""
            is Number -> value.toString()
            is Boolean -> value.toString()
            is Map<*, *> -> mapToJson(value as Map<String, Any>, indent)
            is List<*> -> {
                val items = value.joinToString(", ") {
                    when (it) {
                        is String -> "\"$it\""
                        is Map<*, *> -> mapToJson(it as Map<String, Any>, indent + 1)
                        else -> it.toString()
                    }
                }
                "[$items]"
            }
            else -> "\"$value\""
        }
    }

    /**
     * Converts a JsonSchema directly to a JSON string.
     */
    fun convert(schema: JsonSchema): String {
        return mapToJson(convertToMap(schema))
    }
}
