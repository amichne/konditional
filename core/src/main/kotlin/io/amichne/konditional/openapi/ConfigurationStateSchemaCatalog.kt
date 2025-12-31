package io.amichne.konditional.openapi

import io.amichne.konditional.configstate.FieldDescriptor
import io.amichne.konditional.configstate.FieldType
import io.amichne.konditional.configstate.UiControlType
import io.amichne.kontracts.schema.FieldSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.MapSchema
import io.amichne.kontracts.schema.ObjectSchema

internal object ConfigurationStateSchemaCatalog {
    val schemas: Map<String, JsonSchema<*>>
        get() =
            SerializationSchemaCatalog.schemas +
                mapOf(
                    "ConfigurationStateResponse" to configurationStateResponseSchema,
                    "SupportedValues" to supportedValuesSchema,
                    "FieldType" to fieldTypeSchema,
                    "UiControlType" to uiControlTypeSchema,
                    "FieldDescriptorKind" to fieldDescriptorKindSchema,
                    "UiHints" to uiHintsSchema,
                    "Option" to optionSchema,
                    "FieldDescriptor" to fieldDescriptorSchema,
                )

    private val stringSchema = JsonSchema.string()
    private val intSchema = JsonSchema.int()
    private val doubleSchema = JsonSchema.double()
    private val booleanSchema = JsonSchema.boolean()

    private val uiControlTypeSchema: JsonSchema<UiControlType> =
        JsonSchema.enum(UiControlType.entries.toList())

    private val fieldTypeSchema: JsonSchema<FieldType> =
        JsonSchema.enum(FieldType.entries.toList())

    private val fieldDescriptorKindSchema: JsonSchema<FieldDescriptor.Kind> =
        JsonSchema.enum(FieldDescriptor.Kind.entries.toList())

    private val uiHintsSchema: ObjectSchema =
        ObjectSchema(
            fields =
                mapOf(
                    "control" to required(uiControlTypeSchema),
                    "label" to optional(stringSchema),
                    "helpText" to optional(stringSchema),
                    "placeholder" to optional(stringSchema),
                    "advanced" to optional(booleanSchema, defaultValue = false),
                    "order" to optional(intSchema),
                ),
            description = "UI rendering hints for a field type.",
        )

    private val optionSchema: ObjectSchema =
        ObjectSchema(
            fields =
                mapOf(
                    "value" to required(stringSchema),
                    "label" to required(stringSchema),
                ),
        )

    private val booleanDescriptorSchema: ObjectSchema =
        ObjectSchema(
            fields =
                mapOf(
                    "kind" to required(enumString(FieldDescriptor.Kind.BOOLEAN.name)),
                    "uiHints" to required(uiHintsSchema),
                ),
        )

    private val enumOptionsDescriptorSchema: ObjectSchema =
        ObjectSchema(
            fields =
                mapOf(
                    "kind" to required(enumString(FieldDescriptor.Kind.ENUM_OPTIONS.name)),
                    "uiHints" to required(uiHintsSchema),
                    "options" to required(JsonSchema.array(optionSchema)),
                ),
        )

    private val numberRangeDescriptorSchema: ObjectSchema =
        ObjectSchema(
            fields =
                mapOf(
                    "kind" to required(enumString(FieldDescriptor.Kind.NUMBER_RANGE.name)),
                    "uiHints" to required(uiHintsSchema),
                    "min" to required(doubleSchema),
                    "max" to required(doubleSchema),
                    "step" to required(doubleSchema),
                    "unit" to optional(stringSchema),
                ),
        )

    private val semverConstraintsDescriptorSchema: ObjectSchema =
        ObjectSchema(
            fields =
                mapOf(
                    "kind" to required(enumString(FieldDescriptor.Kind.SEMVER_CONSTRAINTS.name)),
                    "uiHints" to required(uiHintsSchema),
                    "minimum" to required(stringSchema),
                    "allowAnyAboveMinimum" to optional(booleanSchema, defaultValue = true),
                    "pattern" to optional(stringSchema),
                ),
            description = "SemVer constraints: any valid semver >= minimum.",
        )

    private val stringConstraintsDescriptorSchema: ObjectSchema =
        ObjectSchema(
            fields =
                mapOf(
                    "kind" to required(enumString(FieldDescriptor.Kind.STRING_CONSTRAINTS.name)),
                    "uiHints" to required(uiHintsSchema),
                    "minLength" to optional(intSchema),
                    "maxLength" to optional(intSchema),
                    "pattern" to optional(stringSchema),
                    "suggestions" to optional(JsonSchema.array(stringSchema)),
                ),
        )

    private val schemaRefDescriptorSchema: ObjectSchema =
        ObjectSchema(
            fields =
                mapOf(
                    "kind" to required(enumString(FieldDescriptor.Kind.SCHEMA_REF.name)),
                    "uiHints" to required(uiHintsSchema),
                    "ref" to required(stringSchema),
                ),
            description = "Refers to a named schema component in the schema registry.",
        )

    private val mapConstraintsDescriptorSchema: ObjectSchema =
        ObjectSchema(
            fields =
                mapOf(
                    "kind" to required(enumString(FieldDescriptor.Kind.MAP_CONSTRAINTS.name)),
                    "uiHints" to required(uiHintsSchema),
                    "key" to required(stringConstraintsDescriptorSchema),
                    "values" to required(stringConstraintsDescriptorSchema),
                ),
        )

    private val fieldDescriptorSchema: JsonSchema<Any> =
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
        )

    private val supportedValuesSchema: ObjectSchema =
        ObjectSchema(
            fields =
                mapOf(
                    "bindings" to required(MapSchema(fieldTypeSchema)),
                    "byType" to required(MapSchema(fieldDescriptorSchema)),
                ),
            description =
                "Mutation metadata: supported values and constraints, plus bindings for fields in the snapshot JSON.",
        )

    private val configurationStateResponseSchema: ObjectSchema =
        ObjectSchema(
            fields =
                mapOf(
                    "currentState" to required(SerializationSchemaCatalog.schemas.getValue("SerializableSnapshot")),
                    "supportedValues" to required(supportedValuesSchema),
                ),
            description = "Configuration state response: snapshot + mutation metadata.",
        )

    private fun required(
        schema: JsonSchema<*>,
        description: String? = null,
    ): FieldSchema = FieldSchema(schema = schema, required = true, description = description)

    private fun optional(
        schema: JsonSchema<*>,
        description: String? = null,
        defaultValue: Any? = null,
    ): FieldSchema =
        FieldSchema(schema = schema, required = false, defaultValue = defaultValue, description = description)

    private fun enumString(value: String): JsonSchema<String> = JsonSchema.string(enum = listOf(value))
}
