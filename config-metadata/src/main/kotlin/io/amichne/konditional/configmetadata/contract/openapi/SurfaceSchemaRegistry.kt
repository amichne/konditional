package io.amichne.konditional.configmetadata.contract.openapi

import io.amichne.konditional.configmetadata.contract.BindingType
import io.amichne.konditional.configmetadata.descriptor.ValueDescriptor
import io.amichne.konditional.configmetadata.ui.UiControlType
import io.amichne.kontracts.schema.FieldSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.OneOfSchema

internal object SurfaceSchemaRegistry {
    private fun requiredField(
        schema: JsonSchema<*>,
        description: String? = null,
    ): FieldSchema = FieldSchema(schema = schema, required = true, description = description)

    private fun optionalField(
        schema: JsonSchema<*>,
        description: String? = null,
    ): FieldSchema = FieldSchema(schema = schema, required = false, description = description)

    private val uiControlTypeSchema =
        JsonSchema.string(
            description = "UI control variant.",
            enum = UiControlType.entries.map(UiControlType::name),
        )

    private val uiHintsSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "control" to requiredField(uiControlTypeSchema),
                    "label" to optionalField(JsonSchema.string()),
                    "helpText" to optionalField(JsonSchema.string()),
                    "placeholder" to optionalField(JsonSchema.string()),
                    "advanced" to optionalField(JsonSchema.boolean(default = false)),
                    "order" to optionalField(JsonSchema.int()),
                )
        )

    private val enumOptionSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "value" to requiredField(JsonSchema.string(minLength = 1)),
                    "label" to requiredField(JsonSchema.string(minLength = 1)),
                )
        )

    private val booleanDescriptorSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "kind" to requiredField(JsonSchema.string(enum = listOf(ValueDescriptor.Kind.BOOLEAN.name))),
                    "uiHints" to requiredField(uiHintsSchema),
                )
        )

    private val enumOptionsDescriptorSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "kind" to requiredField(JsonSchema.string(enum = listOf(ValueDescriptor.Kind.ENUM_OPTIONS.name))),
                    "uiHints" to requiredField(uiHintsSchema),
                    "options" to requiredField(JsonSchema.array(elementSchema = enumOptionSchema)),
                )
        )

    private val numberRangeDescriptorSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "kind" to requiredField(JsonSchema.string(enum = listOf(ValueDescriptor.Kind.NUMBER_RANGE.name))),
                    "uiHints" to requiredField(uiHintsSchema),
                    "min" to requiredField(JsonSchema.double()),
                    "max" to requiredField(JsonSchema.double()),
                    "step" to requiredField(JsonSchema.double()),
                    "unit" to optionalField(JsonSchema.string()),
                )
        )

    private val semverConstraintsDescriptorSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "kind" to
                        requiredField(
                            JsonSchema.string(enum = listOf(ValueDescriptor.Kind.SEMVER_CONSTRAINTS.name))
                        ),
                    "uiHints" to requiredField(uiHintsSchema),
                    "minimum" to requiredField(JsonSchema.string(minLength = 1)),
                    "allowAnyAboveMinimum" to requiredField(JsonSchema.boolean(default = true)),
                    "pattern" to optionalField(JsonSchema.string()),
                )
        )

    private val stringConstraintsDescriptorSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "kind" to
                        requiredField(
                            JsonSchema.string(enum = listOf(ValueDescriptor.Kind.STRING_CONSTRAINTS.name))
                        ),
                    "uiHints" to requiredField(uiHintsSchema),
                    "minLength" to optionalField(JsonSchema.int(minimum = 0)),
                    "maxLength" to optionalField(JsonSchema.int(minimum = 0)),
                    "pattern" to optionalField(JsonSchema.string()),
                    "suggestions" to optionalField(JsonSchema.array(elementSchema = JsonSchema.string())),
                )
        )

    private val schemaRefDescriptorSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "kind" to requiredField(JsonSchema.string(enum = listOf(ValueDescriptor.Kind.SCHEMA_REF.name))),
                    "uiHints" to requiredField(uiHintsSchema),
                    "ref" to requiredField(JsonSchema.string(minLength = 1)),
                )
        )

    private val mapConstraintsDescriptorSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "kind" to
                        requiredField(
                            JsonSchema.string(enum = listOf(ValueDescriptor.Kind.MAP_CONSTRAINTS.name))
                        ),
                    "uiHints" to requiredField(uiHintsSchema),
                    "key" to requiredField(stringConstraintsDescriptorSchema),
                    "values" to requiredField(stringConstraintsDescriptorSchema),
                )
        )

    private val valueDescriptorSchema =
        JsonSchema.oneOf(
            options =
                listOf(
                    booleanDescriptorSchema,
                    enumOptionsDescriptorSchema,
                    numberRangeDescriptorSchema,
                    semverConstraintsDescriptorSchema,
                    stringConstraintsDescriptorSchema,
                    schemaRefDescriptorSchema,
                    mapConstraintsDescriptorSchema,
                ),
            discriminator =
                OneOfSchema.Discriminator(
                    propertyName = "kind",
                    mapping =
                        linkedMapOf(
                            ValueDescriptor.Kind.BOOLEAN.name to "#/components/schemas/BooleanDescriptor",
                            ValueDescriptor.Kind.ENUM_OPTIONS.name to "#/components/schemas/EnumOptionsDescriptor",
                            ValueDescriptor.Kind.NUMBER_RANGE.name to "#/components/schemas/NumberRangeDescriptor",
                            ValueDescriptor.Kind.SEMVER_CONSTRAINTS.name to
                                "#/components/schemas/SemverConstraintsDescriptor",
                            ValueDescriptor.Kind.STRING_CONSTRAINTS.name to
                                "#/components/schemas/StringConstraintsDescriptor",
                            ValueDescriptor.Kind.SCHEMA_REF.name to "#/components/schemas/SchemaRefDescriptor",
                            ValueDescriptor.Kind.MAP_CONSTRAINTS.name to "#/components/schemas/MapConstraintsDescriptor",
                        ),
                ),
        )

    private val bindingTypeSchema =
        JsonSchema.string(
            enum = BindingType.entries.map(BindingType::name),
        )

    private val configMetadataSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "bindings" to requiredField(JsonSchema.map(valueSchema = bindingTypeSchema)),
                    "descriptors" to requiredField(JsonSchema.map(valueSchema = valueDescriptorSchema)),
                )
        )

    private val snapshotStateSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "namespaceId" to requiredField(JsonSchema.string(minLength = 1)),
                    "featureKey" to requiredField(JsonSchema.string(minLength = 1)),
                    "ruleId" to requiredField(JsonSchema.string(minLength = 1)),
                    "version" to requiredField(JsonSchema.string(minLength = 1)),
                )
        )

    private val snapshotEnvelopeSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "state" to requiredField(snapshotStateSchema),
                    "metadata" to requiredField(configMetadataSchema),
                )
        )

    private val snapshotMutationRequestSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "namespaceId" to requiredField(JsonSchema.string(minLength = 1)),
                    "requestedBy" to requiredField(JsonSchema.string(minLength = 1)),
                    "reason" to requiredField(JsonSchema.string(minLength = 1)),
                )
        )

    private val rulePatchRequestSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "note" to optionalField(JsonSchema.string()),
                    "active" to optionalField(JsonSchema.boolean()),
                    "rampUpPercent" to optionalField(JsonSchema.double(minimum = 0.0, maximum = 100.0)),
                )
        )

    private val codecOutcomeSuccessSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "status" to requiredField(JsonSchema.string(enum = listOf("SUCCESS"))),
                    "appliedVersion" to requiredField(JsonSchema.string(minLength = 1)),
                    "warnings" to requiredField(JsonSchema.array(elementSchema = JsonSchema.string())),
                )
        )

    private val codecOutcomeFailureSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "status" to requiredField(JsonSchema.string(enum = listOf("FAILURE"))),
                    "reason" to requiredField(JsonSchema.string(minLength = 1)),
                    "retryable" to requiredField(JsonSchema.boolean()),
                )
        )

    private val codecOutcomeSchema =
        JsonSchema.oneOf(
            options = listOf(codecOutcomeSuccessSchema, codecOutcomeFailureSchema),
            discriminator =
                OneOfSchema.Discriminator(
                    propertyName = "status",
                    mapping =
                        linkedMapOf(
                            "SUCCESS" to "#/components/schemas/CodecOutcomeSuccess",
                            "FAILURE" to "#/components/schemas/CodecOutcomeFailure",
                        ),
                ),
        )

    private val mutationEnvelopeSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "state" to requiredField(snapshotStateSchema),
                    "metadata" to requiredField(configMetadataSchema),
                    "codecOutcome" to requiredField(codecOutcomeSchema),
                )
        )

    private val apiErrorSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "code" to requiredField(JsonSchema.string(minLength = 1)),
                    "message" to requiredField(JsonSchema.string(minLength = 1)),
                    "details" to optionalField(JsonSchema.map(valueSchema = JsonSchema.string())),
                )
        )

    private val errorEnvelopeSchema =
        JsonSchema.obj(
            fields =
                linkedMapOf(
                    "error" to requiredField(apiErrorSchema),
                )
        )

    val components: Map<String, JsonSchema<*>> =
        linkedMapOf(
            "ApiError" to apiErrorSchema,
            "BindingType" to bindingTypeSchema,
            "BooleanDescriptor" to booleanDescriptorSchema,
            "CodecOutcome" to codecOutcomeSchema,
            "CodecOutcomeFailure" to codecOutcomeFailureSchema,
            "CodecOutcomeSuccess" to codecOutcomeSuccessSchema,
            "ConfigMetadata" to configMetadataSchema,
            "EnumOption" to enumOptionSchema,
            "EnumOptionsDescriptor" to enumOptionsDescriptorSchema,
            "ErrorEnvelope" to errorEnvelopeSchema,
            "MapConstraintsDescriptor" to mapConstraintsDescriptorSchema,
            "MutationEnvelope" to mutationEnvelopeSchema,
            "NumberRangeDescriptor" to numberRangeDescriptorSchema,
            "RulePatchRequest" to rulePatchRequestSchema,
            "SchemaRefDescriptor" to schemaRefDescriptorSchema,
            "SemverConstraintsDescriptor" to semverConstraintsDescriptorSchema,
            "SnapshotEnvelope" to snapshotEnvelopeSchema,
            "SnapshotMutationRequest" to snapshotMutationRequestSchema,
            "SnapshotState" to snapshotStateSchema,
            "StringConstraintsDescriptor" to stringConstraintsDescriptorSchema,
            "UiControlType" to uiControlTypeSchema,
            "UiHints" to uiHintsSchema,
            "ValueDescriptor" to valueDescriptorSchema,
        )
            .toSortedMap()
            .entries
            .associateTo(linkedMapOf()) { it.key to it.value }
}
