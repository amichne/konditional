file=config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/contract/openapi/SurfaceSchemaRegistry.kt
package=io.amichne.konditional.configmetadata.contract.openapi
imports=io.amichne.konditional.configmetadata.contract.BindingType,io.amichne.konditional.configmetadata.descriptor.ValueDescriptor,io.amichne.konditional.configmetadata.ui.UiControlType,io.amichne.kontracts.schema.FieldSchema,io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.OneOfSchema
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
- private fun requiredField( schema: JsonSchema<*>, description: String? = null, ): FieldSchema
- private fun optionalField( schema: JsonSchema<*>, description: String? = null, ): FieldSchema
