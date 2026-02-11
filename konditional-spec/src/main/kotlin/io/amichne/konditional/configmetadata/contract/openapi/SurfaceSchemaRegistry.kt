package io.amichne.konditional.configmetadata.contract.openapi

import io.amichne.kontracts.dsl.ArraySchemaBuilder
import io.amichne.kontracts.dsl.BooleanSchemaBuilder
import io.amichne.kontracts.dsl.DoubleSchemaBuilder
import io.amichne.kontracts.dsl.StringSchemaBuilder
import io.amichne.kontracts.dsl.schema
import io.amichne.kontracts.dsl.schemaRef
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.MapSchema
import io.amichne.kontracts.schema.OneOfSchema

internal object SurfaceSchemaRegistry {
    private fun stringSchema(
        minLength: Int? = null,
        enum: List<String>? = null,
        description: String? = null,
    ): JsonSchema<String> =
        StringSchemaBuilder().apply {
            this.minLength = minLength
            this.enum = enum
            this.description = description
        }.build()

    private fun booleanSchema(default: Boolean? = null): JsonSchema<Boolean> =
        BooleanSchemaBuilder().apply {
            this.default = default
        }.build()

    private fun doubleSchema(
        minimum: Double? = null,
        maximum: Double? = null,
    ): JsonSchema<Double> =
        DoubleSchemaBuilder().apply {
            this.minimum = minimum
            this.maximum = maximum
        }.build()

    private fun arraySchema(elementSchema: JsonSchema<*>): JsonSchema<List<Any>> =
        ArraySchemaBuilder().apply {
            this.elementSchema = elementSchema
        }.build()

    private fun mapSchema(valueSchema: JsonSchema<*>): JsonSchema<Map<String, Any>> =
        MapSchema(valueSchema = valueSchema)

    private fun componentRef(componentName: String): JsonSchema<Any> =
        schemaRef(ref = "#/components/schemas/$componentName")

    private val snapshotStateSchema =
        schema {
            required(name = "namespaceId", schema = stringSchema(minLength = 1))
            required(name = "featureKey", schema = stringSchema(minLength = 1))
            required(name = "ruleId", schema = stringSchema(minLength = 1))
            required(name = "version", schema = stringSchema(minLength = 1))
        }

    private val snapshotEnvelopeSchema =
        schema {
            required(name = "state", schema = componentRef("SnapshotState"))
            optional(name = "snapshotVersion", schema = stringSchema(minLength = 1))
        }

    private val snapshotMutationRequestSchema =
        schema {
            required(name = "namespaceId", schema = stringSchema(minLength = 1))
            required(name = "requestedBy", schema = stringSchema(minLength = 1))
            required(name = "reason", schema = stringSchema(minLength = 1))
        }

    private val rulePatchRequestSchema =
        schema {
            optional(name = "note", schema = stringSchema())
            optional(name = "active", schema = booleanSchema())
            optional(name = "rampUpPercent", schema = doubleSchema(minimum = 0.0, maximum = 100.0))
        }

    private val codecPhaseSchema =
        stringSchema(
            enum = listOf("DECODE_REQUEST", "APPLY_MUTATION", "ENCODE_RESPONSE"),
            description = "Mutation lifecycle phase for codec reporting.",
        )

    private val codecErrorSchema =
        schema {
            required(name = "code", schema = stringSchema(minLength = 1))
            required(name = "message", schema = stringSchema(minLength = 1))
            optional(name = "details", schema = mapSchema(valueSchema = stringSchema()))
        }

    private val codecOutcomeSuccessSchema =
        schema {
            required(name = "status", schema = stringSchema(enum = listOf("SUCCESS")))
            required(name = "phase", schema = componentRef("CodecPhase"))
            required(name = "appliedVersion", schema = stringSchema(minLength = 1))
            required(name = "warnings", schema = arraySchema(elementSchema = stringSchema()))
        }

    private val codecOutcomeFailureSchema =
        schema {
            required(name = "status", schema = stringSchema(enum = listOf("FAILURE")))
            required(name = "phase", schema = componentRef("CodecPhase"))
            required(name = "error", schema = componentRef("CodecError"))
        }

    private val codecOutcomeSchema =
        OneOfSchema(
            options = listOf(componentRef("CodecOutcomeSuccess"), componentRef("CodecOutcomeFailure")),
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
        schema {
            required(name = "state", schema = componentRef("SnapshotState"))
            required(name = "codecOutcome", schema = componentRef("CodecOutcome"))
            optional(name = "snapshotVersion", schema = stringSchema(minLength = 1))
        }

    private val apiErrorSchema =
        schema {
            required(name = "code", schema = stringSchema(minLength = 1))
            required(name = "message", schema = stringSchema(minLength = 1))
            optional(name = "details", schema = mapSchema(valueSchema = stringSchema()))
        }

    private val errorEnvelopeSchema =
        schema {
            required(name = "error", schema = componentRef("ApiError"))
        }

    val components: Map<String, JsonSchema<*>> =
        linkedMapOf(
            "ApiError" to apiErrorSchema,
            "CodecError" to codecErrorSchema,
            "CodecOutcome" to codecOutcomeSchema,
            "CodecOutcomeFailure" to codecOutcomeFailureSchema,
            "CodecOutcomeSuccess" to codecOutcomeSuccessSchema,
            "CodecPhase" to codecPhaseSchema,
            "ErrorEnvelope" to errorEnvelopeSchema,
            "MutationEnvelope" to mutationEnvelopeSchema,
            "RulePatchRequest" to rulePatchRequestSchema,
            "SnapshotEnvelope" to snapshotEnvelopeSchema,
            "SnapshotMutationRequest" to snapshotMutationRequestSchema,
            "SnapshotState" to snapshotStateSchema,
        )
            .toSortedMap()
            .entries
            .associateTo(linkedMapOf()) { (componentName, componentSchema) ->
                componentName to componentSchema
            }
}
