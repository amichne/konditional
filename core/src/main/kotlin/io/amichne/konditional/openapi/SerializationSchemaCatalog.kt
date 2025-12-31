package io.amichne.konditional.openapi

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.rules.versions.VersionRange
import io.amichne.kontracts.dsl.schemaRoot
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.schema.OneOfSchema
import io.amichne.kontracts.schema.OpenApi

internal object SerializationSchemaCatalog {
    private val stringSchema = JsonSchema.string()
    private val uniqueStringArraySchema = JsonSchema.array(stringSchema, uniqueItems = true)
    private val featureIdSchema = JsonSchema.string(description = "Encoded feature identifier: feature::(namespace)::(key).")
    private val allLocales = AppLocale.entries.map { it.name }
    private val allPlatforms = Platform.entries.map { it.name }
    private val unboundedVersionRangeDefault = mapOf("type" to VersionRange.Type.UNBOUNDED.name)

    private val versionSchema =
        schemaRoot {
            required("major", JsonSchema.int(minimum = 0))
            required("minor", JsonSchema.int(minimum = 0))
            required("patch", JsonSchema.int(minimum = 0))
        }.copy(description = "Semantic version representation.")

    private val versionRangeUnboundedSchema =
        schemaRoot {
            required("type", enumString(VersionRange.Type.UNBOUNDED.name))
        }

    private val versionRangeMinBoundSchema =
        schemaRoot {
            required("type", enumString(VersionRange.Type.MIN_BOUND.name))
            required("min", versionSchema)
        }

    private val versionRangeMaxBoundSchema =
        schemaRoot {
            required("type", enumString(VersionRange.Type.MAX_BOUND.name))
            required("max", versionSchema)
        }

    private val versionRangeMinAndMaxBoundSchema =
        schemaRoot {
            required("type", enumString(VersionRange.Type.MIN_AND_MAX_BOUND.name))
            required("min", versionSchema)
            required("max", versionSchema)
        }

    private val versionRangeSchema =
        JsonSchema.oneOf(
            options = listOf(
                versionRangeUnboundedSchema,
                versionRangeMinBoundSchema,
                versionRangeMaxBoundSchema,
                versionRangeMinAndMaxBoundSchema,
            ),
            discriminator = OneOfSchema.Discriminator(
                propertyName = "type",
                mapping = mapOf(
                    VersionRange.Type.UNBOUNDED.name to "VersionRangeUnbounded",
                    VersionRange.Type.MIN_BOUND.name to "VersionRangeMinBound",
                    VersionRange.Type.MAX_BOUND.name to "VersionRangeMaxBound",
                    VersionRange.Type.MIN_AND_MAX_BOUND.name to "VersionRangeMinAndMaxBound",
                ),
            ),
        )

    private val booleanFlagValueSchema =
        schemaRoot {
            required("type", enumString("BOOLEAN"))
            required("value", JsonSchema.boolean())
        }

    private val stringFlagValueSchema =
        schemaRoot {
            required("type", enumString("STRING"))
            required("value", stringSchema)
        }

    private val intFlagValueSchema =
        schemaRoot {
            required("type", enumString("INT"))
            required("value", JsonSchema.int())
        }

    private val doubleFlagValueSchema =
        schemaRoot {
            required("type", enumString("DOUBLE"))
            required("value", JsonSchema.double())
        }

    private val enumFlagValueSchema =
        schemaRoot {
            required("type", enumString("ENUM"))
            required("value", stringSchema)
            required("enumClassName", stringSchema)
        }

    private val dataClassFlagValueSchema =
        schemaRoot {
            required("type", enumString("DATA_CLASS"))
            required("dataClassName", stringSchema)
            required("value", JsonSchema.map(JsonSchema.any(nullable = true)))
        }

    private val flagValueSchema =
        JsonSchema.oneOf(
            options = listOf(
                booleanFlagValueSchema,
                stringFlagValueSchema,
                intFlagValueSchema,
                doubleFlagValueSchema,
                enumFlagValueSchema,
                dataClassFlagValueSchema,
            ),
            discriminator = OneOfSchema.Discriminator(
                propertyName = "type",
                mapping = mapOf(
                    "BOOLEAN" to "BooleanFlagValue",
                    "STRING" to "StringFlagValue",
                    "INT" to "IntFlagValue",
                    "DOUBLE" to "DoubleFlagValue",
                    "ENUM" to "EnumFlagValue",
                    "DATA_CLASS" to "DataClassFlagValue",
                ),
            ),
        )

    private fun serializableRuleSchemaFor(valueSchema: JsonSchema<*>): ObjectSchema =
        schemaRoot {
            required("value", valueSchema)
            optional(
                "rampUp",
                JsonSchema.double(minimum = 0.0, maximum = 100.0),
                defaultValue = 100.0,
            )
            optional("rampUpAllowlist", uniqueStringArraySchema, defaultValue = emptyList<String>())
            optional("note", stringSchema)
            optional("locales", JsonSchema.enum<AppLocale>(AppLocale.entries), defaultValue = AppLocale.UNITED_STATES.id)
            optional("platforms", uniqueStringArraySchema, defaultValue = allPlatforms)
            optional("versionRange", versionRangeSchema, defaultValue = unboundedVersionRangeDefault)
            optional(
                "axes",
                JsonSchema.map(uniqueStringArraySchema),
                defaultValue = emptyMap<String, Any?>(),
            )
        }

    private val serializableRuleSchema = serializableRuleSchemaFor(flagValueSchema)
    private val serializableBooleanRuleSchema = serializableRuleSchemaFor(booleanFlagValueSchema)
    private val serializableStringRuleSchema = serializableRuleSchemaFor(stringFlagValueSchema)
    private val serializableIntRuleSchema = serializableRuleSchemaFor(intFlagValueSchema)
    private val serializableDoubleRuleSchema = serializableRuleSchemaFor(doubleFlagValueSchema)
    private val serializableEnumRuleSchema = serializableRuleSchemaFor(enumFlagValueSchema)
    private val serializableDataClassRuleSchema = serializableRuleSchemaFor(dataClassFlagValueSchema)

    private fun serializableFlagSchemaFor(
        defaultValueSchema: JsonSchema<*>,
        ruleSchema: JsonSchema<*>,
    ): ObjectSchema =
        schemaRoot {
            required("key", featureIdSchema)
            required("defaultValue", defaultValueSchema)
            optional("salt", stringSchema, defaultValue = "v1")
            optional("isActive", JsonSchema.boolean(), defaultValue = true)
            optional("rampUpAllowlist", uniqueStringArraySchema, defaultValue = emptyList<String>())
            optional("rules", JsonSchema.array(ruleSchema), defaultValue = emptyList<Any?>())
        }

    private val serializableBooleanFlagSchema =
        serializableFlagSchemaFor(
            defaultValueSchema = booleanFlagValueSchema,
            ruleSchema = serializableBooleanRuleSchema,
        )

    private val serializableStringFlagSchema =
        serializableFlagSchemaFor(
            defaultValueSchema = stringFlagValueSchema,
            ruleSchema = serializableStringRuleSchema,
        )

    private val serializableIntFlagSchema =
        serializableFlagSchemaFor(
            defaultValueSchema = intFlagValueSchema,
            ruleSchema = serializableIntRuleSchema,
        )

    private val serializableDoubleFlagSchema =
        serializableFlagSchemaFor(
            defaultValueSchema = doubleFlagValueSchema,
            ruleSchema = serializableDoubleRuleSchema,
        )

    private val serializableEnumFlagSchema =
        serializableFlagSchemaFor(
            defaultValueSchema = enumFlagValueSchema,
            ruleSchema = serializableEnumRuleSchema,
        )

    private val serializableDataClassFlagSchema =
        serializableFlagSchemaFor(
            defaultValueSchema = dataClassFlagValueSchema,
            ruleSchema = serializableDataClassRuleSchema,
        )

    private val serializableFlagSchema =
        JsonSchema.oneOf(
            listOf(
                serializableBooleanFlagSchema,
                serializableStringFlagSchema,
                serializableIntFlagSchema,
                serializableDoubleFlagSchema,
                serializableEnumFlagSchema,
                serializableDataClassFlagSchema,
            ),
            discriminator = OneOfSchema.Discriminator(
                propertyName = "defaultValue.type",
                mapping = mapOf(
                    OpenApi.Type.BOOLEAN.name to OpenApi.Type.BOOLEAN.serialized,
                    // Using OpenApi.Type names for consistency
                    OpenApi.Type.STRING.name to OpenApi.Type.STRING.serialized,
                    OpenApi.Type.INTEGER.name to OpenApi.Type.INTEGER.serialized,
                    OpenApi.Type.NUMBER.name to OpenApi.Type.NUMBER.serialized,
                    "ENUM" to "EnumFlag",
                    "DATA_CLASS" to "DataClassFlag",
                ),
            ),
        )

    private val serializableSnapshotMetadataSchema =
        schemaRoot {
            optional("version", stringSchema)
            optional("generatedAtEpochMillis", JsonSchema.double(format = "int64", minimum = 0.0))
            optional("source", stringSchema)
        }

    private val serializableSnapshotSchema =
        schemaRoot {
            optional("meta", serializableSnapshotMetadataSchema)
            required("flags", JsonSchema.array(serializableFlagSchema))
        }

    private val serializablePatchSchema =
        schemaRoot {
            optional("meta", serializableSnapshotMetadataSchema)
            required("flags", JsonSchema.array(serializableFlagSchema))
            optional("removeKeys", JsonSchema.array(featureIdSchema), defaultValue = emptyList<String>())
        }

    val schemas: Map<String, JsonSchema<*>> =
        mapOf(
            "FeatureId" to featureIdSchema,
            "Version" to versionSchema,
            "VersionRangeUnbounded" to versionRangeUnboundedSchema,
            "VersionRangeMinBound" to versionRangeMinBoundSchema,
            "VersionRangeMaxBound" to versionRangeMaxBoundSchema,
            "VersionRangeMinAndMaxBound" to versionRangeMinAndMaxBoundSchema,
            "VersionRange" to versionRangeSchema,
            "BooleanFlagValue" to booleanFlagValueSchema,
            "StringFlagValue" to stringFlagValueSchema,
            "IntFlagValue" to intFlagValueSchema,
            "DoubleFlagValue" to doubleFlagValueSchema,
            "EnumFlagValue" to enumFlagValueSchema,
            "DataClassFlagValue" to dataClassFlagValueSchema,
            "FlagValue" to flagValueSchema,
            "Rule" to serializableRuleSchema,
            "BooleanRule" to serializableBooleanRuleSchema,
            "StringRule" to serializableStringRuleSchema,
            "IntRule" to serializableIntRuleSchema,
            "DoubleRule" to serializableDoubleRuleSchema,
            "EnumRule" to serializableEnumRuleSchema,
            "DataClassRule" to serializableDataClassRuleSchema,
            "Flag" to serializableFlagSchema,
            "BooleanFlag" to serializableBooleanFlagSchema,
            "StringFlag" to serializableStringFlagSchema,
            "IntFlag" to serializableIntFlagSchema,
            "DoubleFlag" to serializableDoubleFlagSchema,
            "EnumFlag" to serializableEnumFlagSchema,
            "DataClassFlag" to serializableDataClassFlagSchema,
            "SnapshotMetadata" to serializableSnapshotMetadataSchema,
            "Snapshot" to serializableSnapshotSchema,
            "Patch" to serializablePatchSchema,
        )

    private fun enumString(value: String): JsonSchema<String> = JsonSchema.string(enum = listOf(value))
}
