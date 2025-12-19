package io.amichne.konditional.openapi

import io.amichne.kontracts.schema.AnySchema
import io.amichne.kontracts.schema.FieldSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.MapSchema
import io.amichne.kontracts.schema.ObjectSchema

internal object SerializationSchemaCatalog {
    private val stringSchema = JsonSchema.string()
    private val uniqueStringArraySchema = JsonSchema.array(stringSchema, uniqueItems = true)
    private val featureIdSchema =
        JsonSchema.string(description = "Encoded feature identifier: feature::<namespace>::<key>.")

    private val versionSchema =
        ObjectSchema(
            fields =
                mapOf(
                    "major" to required(JsonSchema.int(minimum = 0)),
                    "minor" to required(JsonSchema.int(minimum = 0)),
                    "patch" to required(JsonSchema.int(minimum = 0)),
                ),
            description = "Semantic version representation.",
        )

    private val versionRangeSchema =
        JsonSchema.oneOf(
            listOf(
                ObjectSchema(
                    fields = mapOf("type" to required(enumString("UNBOUNDED"))),
                ),
                ObjectSchema(
                    fields =
                        mapOf(
                            "type" to required(enumString("MIN_BOUND")),
                            "min" to required(versionSchema),
                        ),
                ),
                ObjectSchema(
                    fields =
                        mapOf(
                            "type" to required(enumString("MAX_BOUND")),
                            "max" to required(versionSchema),
                        ),
                ),
                ObjectSchema(
                    fields =
                        mapOf(
                            "type" to required(enumString("MIN_AND_MAX_BOUND")),
                            "min" to required(versionSchema),
                            "max" to required(versionSchema),
                        ),
                ),
            ),
        )

    private val flagValueSchema =
        JsonSchema.oneOf(
            listOf(
                ObjectSchema(
                    fields =
                        mapOf(
                            "type" to required(enumString("BOOLEAN")),
                            "value" to required(JsonSchema.boolean()),
                        ),
                ),
                ObjectSchema(
                    fields =
                        mapOf(
                            "type" to required(enumString("STRING")),
                            "value" to required(stringSchema),
                        ),
                ),
                ObjectSchema(
                    fields =
                        mapOf(
                            "type" to required(enumString("INT")),
                            "value" to required(JsonSchema.int()),
                        ),
                ),
                ObjectSchema(
                    fields =
                        mapOf(
                            "type" to required(enumString("DOUBLE")),
                            "value" to required(JsonSchema.double()),
                        ),
                ),
                ObjectSchema(
                    fields =
                        mapOf(
                            "type" to required(enumString("ENUM")),
                            "value" to required(stringSchema),
                            "enumClassName" to required(stringSchema),
                        ),
                ),
                ObjectSchema(
                    fields =
                        mapOf(
                            "type" to required(enumString("DATA_CLASS")),
                            "dataClassName" to required(stringSchema),
                            "value" to required(MapSchema(AnySchema(nullable = true))),
                        ),
                ),
            ),
        )

    private val serializableRuleSchema =
        ObjectSchema(
            fields =
                mapOf(
                    "value" to required(flagValueSchema),
                    "rampUp" to optional(
                        JsonSchema.double(minimum = 0.0, maximum = 100.0),
                        defaultValue = 100.0,
                    ),
                    "rampUpAllowlist" to optional(uniqueStringArraySchema, defaultValue = emptyList<String>()),
                    "note" to optional(stringSchema),
                    "locales" to optional(uniqueStringArraySchema, defaultValue = emptyList<String>()),
                    "platforms" to optional(uniqueStringArraySchema, defaultValue = emptyList<String>()),
                    "versionRange" to optional(versionRangeSchema),
                    "axes" to optional(
                        MapSchema(uniqueStringArraySchema),
                        defaultValue = emptyMap<String, Any?>(),
                    ),
                ),
        )

    private val serializableFlagSchema =
        ObjectSchema(
            fields =
                mapOf(
                    "key" to required(featureIdSchema),
                    "defaultValue" to required(flagValueSchema),
                    "salt" to optional(stringSchema, defaultValue = "v1"),
                    "isActive" to optional(JsonSchema.boolean(), defaultValue = true),
                    "rampUpAllowlist" to optional(uniqueStringArraySchema, defaultValue = emptyList<String>()),
                    "rules" to optional(JsonSchema.array(serializableRuleSchema), defaultValue = emptyList<Any?>()),
                ),
        )

    private val serializableSnapshotMetadataSchema =
        ObjectSchema(
            fields =
                mapOf(
                    "version" to optional(stringSchema),
                    "generatedAtEpochMillis" to optional(
                        JsonSchema.double(format = "int64", minimum = 0.0),
                    ),
                    "source" to optional(stringSchema),
                ),
        )

    private val serializableSnapshotSchema =
        ObjectSchema(
            fields =
                mapOf(
                    "meta" to optional(serializableSnapshotMetadataSchema),
                    "flags" to required(JsonSchema.array(serializableFlagSchema)),
                ),
        )

    private val serializablePatchSchema =
        ObjectSchema(
            fields =
                mapOf(
                    "meta" to optional(serializableSnapshotMetadataSchema),
                    "flags" to required(JsonSchema.array(serializableFlagSchema)),
                    "removeKeys" to optional(
                        JsonSchema.array(featureIdSchema),
                        defaultValue = emptyList<String>(),
                    ),
                ),
        )

    val schemas: Map<String, JsonSchema> =
        mapOf(
            "FeatureId" to featureIdSchema,
            "Version" to versionSchema,
            "VersionRange" to versionRangeSchema,
            "FlagValue" to flagValueSchema,
            "SerializableRule" to serializableRuleSchema,
            "SerializableFlag" to serializableFlagSchema,
            "SerializableSnapshotMetadata" to serializableSnapshotMetadataSchema,
            "SerializableSnapshot" to serializableSnapshotSchema,
            "SerializablePatch" to serializablePatchSchema,
        )

    private fun required(
        schema: JsonSchema,
        description: String? = null,
    ): FieldSchema = FieldSchema(schema = schema, required = true, description = description)

    private fun optional(
        schema: JsonSchema,
        description: String? = null,
        defaultValue: Any? = null,
    ): FieldSchema =
        FieldSchema(schema = schema, required = false, defaultValue = defaultValue, description = description)

    private fun enumString(value: String): JsonSchema = JsonSchema.string(enum = listOf(value))
}
