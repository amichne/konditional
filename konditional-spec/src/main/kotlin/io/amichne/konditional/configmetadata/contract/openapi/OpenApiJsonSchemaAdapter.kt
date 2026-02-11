package io.amichne.konditional.configmetadata.contract.openapi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
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
import java.lang.reflect.Type

internal object OpenApiJsonSchemaAdapterFactory : JsonAdapter.Factory {
    override fun create(
        type: Type,
        annotations: Set<Annotation>,
        moshi: Moshi,
    ): JsonAdapter<*>? =
        OpenApiJsonSchemaAdapter.takeIf {
            annotations.isEmpty() && Types.getRawType(type) == JsonSchema::class.java
        }
}

private object OpenApiJsonSchemaAdapter : JsonAdapter<JsonSchema<*>>() {
    override fun toJson(
        writer: JsonWriter,
        value: JsonSchema<*>?,
    ) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.jsonValue(encodeSchema(value))
        }
    }

    override fun fromJson(reader: JsonReader): JsonSchema<*> =
        throw JsonDataException("JsonSchema deserialization is not supported for OpenAPI generation.")

    private fun encodeSchema(schema: JsonSchema<*>): Map<String, Any?> =
        when (schema) {
            is RefSchema -> linkedMapOf("\$ref" to schema.ref)
            is BooleanSchema -> withCommonProperties(schema, linkedMapOf("type" to "boolean"))
            is StringSchema ->
                withCommonProperties(
                    schema,
                    linkedMapOf<String, Any?>("type" to "string").apply {
                        schema.minLength?.let { put("minLength", it) }
                        schema.maxLength?.let { put("maxLength", it) }
                        schema.pattern?.let { put("pattern", it) }
                        schema.format?.let { put("format", it) }
                        schema.enum?.takeIf(List<String>::isNotEmpty)?.let { put("enum", it) }
                    },
                )

            is IntSchema ->
                withCommonProperties(
                    schema,
                    linkedMapOf<String, Any?>("type" to "integer", "format" to "int32").apply {
                        schema.minimum?.let { put("minimum", it) }
                        schema.maximum?.let { put("maximum", it) }
                        schema.enum?.takeIf(List<Int>::isNotEmpty)?.let { put("enum", it) }
                    },
                )

            is DoubleSchema ->
                withCommonProperties(
                    schema,
                    linkedMapOf<String, Any?>("type" to "number").apply {
                        schema.format?.let { put("format", it) }
                        schema.minimum?.let { put("minimum", it) }
                        schema.maximum?.let { put("maximum", it) }
                        schema.enum?.takeIf(List<Double>::isNotEmpty)?.let { put("enum", it) }
                    },
                )

            is EnumSchema<*> ->
                withCommonProperties(
                    schema,
                    linkedMapOf(
                        "type" to "string",
                        "enum" to schema.values.map { value -> value.name },
                    ),
                )

            is NullSchema -> withCommonProperties(schema, linkedMapOf("type" to "null", "nullable" to true))
            is AnySchema -> withCommonProperties(schema, linkedMapOf("type" to "object"))
            is ArraySchema<*> ->
                withCommonProperties(
                    schema,
                    linkedMapOf<String, Any?>(
                        "type" to "array",
                        "items" to encodeSchema(schema.elementSchema),
                    ).apply {
                        schema.minItems?.let { put("minItems", it) }
                        schema.maxItems?.let { put("maxItems", it) }
                        if (schema.uniqueItems) {
                            put("uniqueItems", true)
                        }
                    },
                )

            is MapSchema<*> ->
                withCommonProperties(
                    schema,
                    linkedMapOf<String, Any?>(
                        "type" to "object",
                        "additionalProperties" to encodeSchema(schema.valueSchema),
                    ).apply {
                        schema.minProperties?.let { put("minProperties", it) }
                        schema.maxProperties?.let { put("maxProperties", it) }
                    },
                )

            is ObjectSchema -> encodeObjectSchema(schema = schema, objectTraits = schema)
            is RootObjectSchema -> encodeObjectSchema(schema = schema, objectTraits = schema)
            is OneOfSchema ->
                withCommonProperties(
                    schema,
                    linkedMapOf<String, Any?>(
                        "oneOf" to schema.options.map(::encodeSchema),
                    ).apply {
                        schema.discriminator?.let { discriminator ->
                            put(
                                "discriminator",
                                linkedMapOf(
                                    "propertyName" to discriminator.propertyName,
                                    "mapping" to
                                        discriminator.mapping
                                            .toSortedMap()
                                            .entries
                                            .associateTo(linkedMapOf()) { it.key to it.value },
                                ),
                            )
                        }
                    },
                )

            is AllOfSchema ->
                withCommonProperties(
                    schema,
                    linkedMapOf(
                        "allOf" to schema.options.map(::encodeSchema),
                    ),
                )
        }

    private fun encodeObjectSchema(
        schema: JsonSchema<*>,
        objectTraits: ObjectTraits,
    ): Map<String, Any?> {
        val requiredFields =
            (objectTraits.required ?: objectTraits.fields.filterValues(FieldSchema::required).keys)
                .toSortedSet()

        val properties =
            objectTraits.fields
                .toSortedMap()
                .entries
                .associateTo(linkedMapOf()) { (fieldName, fieldSchema) ->
                    fieldName to encodeFieldSchema(fieldSchema)
                }

        return withCommonProperties(
            schema,
            linkedMapOf<String, Any?>(
                "type" to "object",
                "additionalProperties" to false,
                "properties" to properties,
            ).apply {
                requiredFields
                    .toList()
                    .takeIf(List<String>::isNotEmpty)
                    ?.let { put("required", it) }
            },
        )
    }

    private fun encodeFieldSchema(fieldSchema: FieldSchema): Map<String, Any?> =
        encodeSchema(fieldSchema.schema)
            .toMutableMap()
            .apply {
                fieldSchema.description?.let { this["description"] = it }
                fieldSchema.defaultValue?.let { this["default"] = normalizeScalar(it) }
                if (fieldSchema.deprecated) {
                    this["deprecated"] = true
                }
            }
            .toMap(linkedMapOf())

    private fun withCommonProperties(
        schema: JsonSchema<*>,
        rawSchema: LinkedHashMap<String, Any?>,
    ): Map<String, Any?> =
        rawSchema.apply {
            schema.title?.let { put("title", it) }
            schema.description?.let { put("description", it) }
            schema.default?.let { put("default", normalizeScalar(it)) }
            if (schema.nullable) {
                put("nullable", true)
            }
            schema.example?.let { put("example", normalizeScalar(it)) }
            if (schema.deprecated) {
                put("deprecated", true)
            }
        }.toMap(linkedMapOf())

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
