file=config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/contract/openapi/SurfaceSchemaRegistry.kt
package=io.amichne.konditional.configmetadata.contract.openapi
imports=io.amichne.konditional.configmetadata.contract.BindingType,io.amichne.konditional.configmetadata.descriptor.ValueDescriptor,io.amichne.konditional.configmetadata.ui.UiControlType,io.amichne.kontracts.dsl.ArraySchemaBuilder,io.amichne.kontracts.dsl.BooleanSchemaBuilder,io.amichne.kontracts.dsl.DoubleSchemaBuilder,io.amichne.kontracts.dsl.IntSchemaBuilder,io.amichne.kontracts.dsl.StringSchemaBuilder,io.amichne.kontracts.dsl.schema,io.amichne.kontracts.dsl.schemaRef,io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.MapSchema,io.amichne.kontracts.schema.OneOfSchema
type=io.amichne.konditional.configmetadata.contract.openapi.SurfaceSchemaRegistry|kind=object|decl=internal object SurfaceSchemaRegistry
fields:
- private val uiControlTypeSchema
- private val uiHintsSchema
- private val enumOptionSchema
- private val booleanDescriptorSchema
- private val enumOptionsDescriptorSchema
- private val numberRangeDescriptorSchema
- private val semverConstraintsDescriptorSchema
- private val stringConstraintsDescriptorSchema
- private val schemaRefDescriptorSchema
- private val mapConstraintsDescriptorSchema
- private val valueDescriptorSchema
- private val bindingTypeSchema
- private val configMetadataSchema
- private val snapshotStateSchema
- private val snapshotEnvelopeSchema
- private val snapshotMutationRequestSchema
- private val rulePatchRequestSchema
- private val codecOutcomeSuccessSchema
- private val codecOutcomeFailureSchema
- private val codecOutcomeSchema
- private val mutationEnvelopeSchema
- private val apiErrorSchema
- private val errorEnvelopeSchema
- val components: Map<String, JsonSchema<*>>
methods:
- private fun stringSchema( minLength: Int? = null, enum: List<String>? = null, description: String? = null, pattern: String? = null, ): JsonSchema<String>
- private fun booleanSchema(default: Boolean? = null): JsonSchema<Boolean>
- private fun intSchema( minimum: Int? = null, maximum: Int? = null, ): JsonSchema<Int>
- private fun doubleSchema( minimum: Double? = null, maximum: Double? = null, ): JsonSchema<Double>
- private fun arraySchema(elementSchema: JsonSchema<*>): JsonSchema<List<Any>>
- private fun mapSchema(valueSchema: JsonSchema<*>): JsonSchema<Map<String, Any>>
- private fun componentRef(componentName: String): JsonSchema<Any>
