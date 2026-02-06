@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.openapi

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Version
import io.amichne.konditional.internal.serialization.models.SerializableFlag
import io.amichne.konditional.internal.serialization.models.SerializablePatch
import io.amichne.konditional.internal.serialization.models.SerializableRule
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import io.amichne.konditional.internal.serialization.models.SerializableSnapshotMetadata
import io.amichne.kontracts.dsl.reflectiveSchema
import io.amichne.kontracts.schema.AnySchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.MapSchema
import io.amichne.kontracts.schema.OneOfSchema
import io.amichne.kontracts.schema.ReflectiveObjectSchema

private data class VersionRangeUnboundedPayload(
    val type: String,
)

private data class VersionRangeMinBoundPayload(
    val type: String,
    val min: Version,
)

private data class VersionRangeMaxBoundPayload(
    val type: String,
    val max: Version,
)

private data class VersionRangeMinAndMaxBoundPayload(
    val type: String,
    val min: Version,
    val max: Version,
)

private data class BooleanFlagValuePayload(
    val type: String,
    val value: Boolean,
)

private data class StringFlagValuePayload(
    val type: String,
    val value: String,
)

private data class IntFlagValuePayload(
    val type: String,
    val value: Int,
)

private data class DoubleFlagValuePayload(
    val type: String,
    val value: Double,
)

private data class EnumFlagValuePayload(
    val type: String,
    val value: String,
    val enumClassName: String,
)

private data class DataClassFlagValuePayload(
    val type: String,
    val dataClassName: String,
    val value: Map<String, Any?>,
)

internal data class SerializationComponentSchemas(
    val featureId: JsonSchema<String>,
    val version: ReflectiveObjectSchema<Version>,
    val versionRange: JsonSchema<*>,
    val flagValue: JsonSchema<*>,
    val serializableRule: ReflectiveObjectSchema<SerializableRule>,
    val serializableFlag: ReflectiveObjectSchema<SerializableFlag>,
    val serializableSnapshotMetadata: ReflectiveObjectSchema<SerializableSnapshotMetadata>,
    val serializableSnapshot: ReflectiveObjectSchema<SerializableSnapshot>,
    val serializablePatch: ReflectiveObjectSchema<SerializablePatch>,
) {
    val asMap: Map<String, JsonSchema<*>> =
        linkedMapOf(
            "FeatureId" to featureId,
            "Version" to version,
            "VersionRange" to versionRange,
            "FlagValue" to flagValue,
            "SerializableRule" to serializableRule,
            "SerializableFlag" to serializableFlag,
            "SerializableSnapshotMetadata" to serializableSnapshotMetadata,
            "SerializableSnapshot" to serializableSnapshot,
            "SerializablePatch" to serializablePatch,
        )
}

internal object SerializationSchemaCatalog {
    val schemas: SerializationComponentSchemas = createSchemas()
}

private fun createSchemas(): SerializationComponentSchemas {
    val stringSchema = JsonSchema.string()
    val uniqueStringArraySchema = JsonSchema.array(stringSchema, uniqueItems = true)
    val featureIdSchema =
        JsonSchema.string(description = "Encoded feature identifier: feature::<namespace>::<key>.")
    val versionSchema = createVersionSchema()
    val versionRangeSchema = createVersionRangeSchema(versionSchema)
    val flagValueSchema = createFlagValueSchema(stringSchema)
    val serializableRuleSchema =
        createSerializableRuleSchema(
            flagValueSchema = flagValueSchema,
            uniqueStringArraySchema = uniqueStringArraySchema,
            stringSchema = stringSchema,
            versionRangeSchema = versionRangeSchema,
        )
    val serializableFlagSchema =
        createSerializableFlagSchema(
            featureIdSchema = featureIdSchema,
            flagValueSchema = flagValueSchema,
            stringSchema = stringSchema,
            uniqueStringArraySchema = uniqueStringArraySchema,
            serializableRuleSchema = serializableRuleSchema,
        )
    val serializableSnapshotMetadataSchema =
        createSerializableSnapshotMetadataSchema(stringSchema)
    val serializableSnapshotSchema =
        createSerializableSnapshotSchema(serializableFlagSchema, serializableSnapshotMetadataSchema)
    val serializablePatchSchema =
        createSerializablePatchSchema(
            featureIdSchema = featureIdSchema,
            serializableFlagSchema = serializableFlagSchema,
            serializableSnapshotMetadataSchema = serializableSnapshotMetadataSchema,
        )

    return SerializationComponentSchemas(
        featureId = featureIdSchema,
        version = versionSchema,
        versionRange = versionRangeSchema,
        flagValue = flagValueSchema,
        serializableRule = serializableRuleSchema,
        serializableFlag = serializableFlagSchema,
        serializableSnapshotMetadata = serializableSnapshotMetadataSchema,
        serializableSnapshot = serializableSnapshotSchema,
        serializablePatch = serializablePatchSchema,
    )
}

private fun createVersionSchema(): ReflectiveObjectSchema<Version> =
    reflectiveSchema<Version> {
        required(Version::major, JsonSchema.int(minimum = 0))
        required(Version::minor, JsonSchema.int(minimum = 0))
        required(Version::patch, JsonSchema.int(minimum = 0))
        description = "Semantic version representation."
    }

private fun createVersionRangeSchema(
    versionSchema: ReflectiveObjectSchema<Version>,
): JsonSchema<*> =
    JsonSchema.oneOf(
        listOf(
            reflectiveSchema<VersionRangeUnboundedPayload> {
                required(VersionRangeUnboundedPayload::type, enumString("UNBOUNDED"))
            },
            reflectiveSchema<VersionRangeMinBoundPayload> {
                required(VersionRangeMinBoundPayload::type, enumString("MIN_BOUND"))
                required(VersionRangeMinBoundPayload::min, versionSchema)
            },
            reflectiveSchema<VersionRangeMaxBoundPayload> {
                required(VersionRangeMaxBoundPayload::type, enumString("MAX_BOUND"))
                required(VersionRangeMaxBoundPayload::max, versionSchema)
            },
            reflectiveSchema<VersionRangeMinAndMaxBoundPayload> {
                required(VersionRangeMinAndMaxBoundPayload::type, enumString("MIN_AND_MAX_BOUND"))
                required(VersionRangeMinAndMaxBoundPayload::min, versionSchema)
                required(VersionRangeMinAndMaxBoundPayload::max, versionSchema)
            },
        ),
        discriminator =
            OneOfSchema.Discriminator(
                propertyName = "type",
                mapping =
                    mapOf(
                        "UNBOUNDED" to "#/components/schemas/VersionRangeUnbounded",
                        "MIN_BOUND" to "#/components/schemas/VersionRangeMinBound",
                        "MAX_BOUND" to "#/components/schemas/VersionRangeMaxBound",
                        "MIN_AND_MAX_BOUND" to "#/components/schemas/VersionRangeMinAndMaxBound",
                    ),
            ),
    )

private fun createFlagValueSchema(
    stringSchema: JsonSchema<String>,
): JsonSchema<*> =
    JsonSchema.oneOf(
        listOf(
            reflectiveSchema<BooleanFlagValuePayload> {
                required(BooleanFlagValuePayload::type, enumString("BOOLEAN"))
                required(BooleanFlagValuePayload::value, JsonSchema.boolean())
            },
            reflectiveSchema<StringFlagValuePayload> {
                required(StringFlagValuePayload::type, enumString("STRING"))
                required(StringFlagValuePayload::value, stringSchema)
            },
            reflectiveSchema<IntFlagValuePayload> {
                required(IntFlagValuePayload::type, enumString("INT"))
                required(IntFlagValuePayload::value, JsonSchema.int())
            },
            reflectiveSchema<DoubleFlagValuePayload> {
                required(DoubleFlagValuePayload::type, enumString("DOUBLE"))
                required(DoubleFlagValuePayload::value, JsonSchema.double())
            },
            reflectiveSchema<EnumFlagValuePayload> {
                required(EnumFlagValuePayload::type, enumString("ENUM"))
                required(EnumFlagValuePayload::value, stringSchema)
                required(EnumFlagValuePayload::enumClassName, stringSchema)
            },
            reflectiveSchema<DataClassFlagValuePayload> {
                required(DataClassFlagValuePayload::type, enumString("DATA_CLASS"))
                required(DataClassFlagValuePayload::dataClassName, stringSchema)
                required(DataClassFlagValuePayload::value, MapSchema(AnySchema(nullable = true)))
            },
        ),
        discriminator =
            OneOfSchema.Discriminator(
                propertyName = "type",
                mapping =
                    mapOf(
                        "BOOLEAN" to "#/components/schemas/FlagValueBoolean",
                        "STRING" to "#/components/schemas/FlagValueString",
                        "INT" to "#/components/schemas/FlagValueInt",
                        "DOUBLE" to "#/components/schemas/FlagValueDouble",
                        "ENUM" to "#/components/schemas/FlagValueEnum",
                        "DATA_CLASS" to "#/components/schemas/FlagValueDataClass",
                    ),
            ),
    )

private fun createSerializableRuleSchema(
    flagValueSchema: JsonSchema<*>,
    uniqueStringArraySchema: JsonSchema<*>,
    stringSchema: JsonSchema<String>,
    versionRangeSchema: JsonSchema<*>,
): ReflectiveObjectSchema<SerializableRule> =
    reflectiveSchema<SerializableRule> {
        required(SerializableRule::value, flagValueSchema)
        optional(
            SerializableRule::rampUp,
            JsonSchema.double(minimum = 0.0, maximum = 100.0),
            defaultValue = 100.0,
        )
        optional(SerializableRule::rampUpAllowlist, uniqueStringArraySchema, defaultValue = emptyList<String>())
        optional(SerializableRule::note, stringSchema)
        optional(SerializableRule::locales, uniqueStringArraySchema, defaultValue = emptyList<String>())
        optional(SerializableRule::platforms, uniqueStringArraySchema, defaultValue = emptyList<String>())
        optional(SerializableRule::versionRange, versionRangeSchema)
        optional(
            SerializableRule::axes,
            MapSchema(uniqueStringArraySchema),
            defaultValue = emptyMap<String, List<String>>(),
        )
    }

private fun createSerializableFlagSchema(
    featureIdSchema: JsonSchema<String>,
    flagValueSchema: JsonSchema<*>,
    stringSchema: JsonSchema<String>,
    uniqueStringArraySchema: JsonSchema<*>,
    serializableRuleSchema: ReflectiveObjectSchema<SerializableRule>,
): ReflectiveObjectSchema<SerializableFlag> =
    reflectiveSchema<SerializableFlag> {
        required(SerializableFlag::key, featureIdSchema)
        required(SerializableFlag::defaultValue, flagValueSchema)
        optional(SerializableFlag::salt, stringSchema, defaultValue = "v1")
        optional(SerializableFlag::isActive, JsonSchema.boolean(), defaultValue = true)
        optional(SerializableFlag::rampUpAllowlist, uniqueStringArraySchema, defaultValue = emptyList<String>())
        optional(
            SerializableFlag::rules,
            JsonSchema.array(serializableRuleSchema),
            defaultValue = emptyList<Any?>(),
        )
    }

private fun createSerializableSnapshotMetadataSchema(
    stringSchema: JsonSchema<String>,
): ReflectiveObjectSchema<SerializableSnapshotMetadata> =
    reflectiveSchema<SerializableSnapshotMetadata> {
        optional(SerializableSnapshotMetadata::version, stringSchema)
        optional(
            SerializableSnapshotMetadata::generatedAtEpochMillis,
            JsonSchema.double(format = "int64", minimum = 0.0),
        )
        optional(SerializableSnapshotMetadata::source, stringSchema)
    }

private fun createSerializableSnapshotSchema(
    serializableFlagSchema: ReflectiveObjectSchema<SerializableFlag>,
    serializableSnapshotMetadataSchema: ReflectiveObjectSchema<SerializableSnapshotMetadata>,
): ReflectiveObjectSchema<SerializableSnapshot> =
    reflectiveSchema<SerializableSnapshot> {
        optional(SerializableSnapshot::meta, serializableSnapshotMetadataSchema)
        required(SerializableSnapshot::flags, JsonSchema.array(serializableFlagSchema))
    }

private fun createSerializablePatchSchema(
    featureIdSchema: JsonSchema<String>,
    serializableFlagSchema: ReflectiveObjectSchema<SerializableFlag>,
    serializableSnapshotMetadataSchema: ReflectiveObjectSchema<SerializableSnapshotMetadata>,
): ReflectiveObjectSchema<SerializablePatch> =
    reflectiveSchema<SerializablePatch> {
        optional(SerializablePatch::meta, serializableSnapshotMetadataSchema)
        required(SerializablePatch::flags, JsonSchema.array(serializableFlagSchema))
        optional(
            SerializablePatch::removeKeys,
            JsonSchema.array(featureIdSchema),
            defaultValue = emptyList<String>(),
        )
    }

private fun enumString(value: String): JsonSchema<String> = JsonSchema.string(enum = listOf(value))
