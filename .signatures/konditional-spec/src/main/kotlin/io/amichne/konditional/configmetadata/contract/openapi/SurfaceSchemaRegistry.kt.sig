file=konditional-spec/src/main/kotlin/io/amichne/konditional/configmetadata/contract/openapi/SurfaceSchemaRegistry.kt
package=io.amichne.konditional.configmetadata.contract.openapi
imports=io.amichne.kontracts.dsl.arraySchema,io.amichne.kontracts.dsl.booleanSchema,io.amichne.kontracts.dsl.doubleSchema,io.amichne.kontracts.dsl.mapSchema,io.amichne.kontracts.dsl.oneOfSchema,io.amichne.kontracts.dsl.schema,io.amichne.kontracts.dsl.schemaRef,io.amichne.kontracts.dsl.stringSchema,io.amichne.kontracts.schema.JsonSchema
type=io.amichne.konditional.configmetadata.contract.openapi.SurfaceSchemaRegistry|kind=object|decl=internal object SurfaceSchemaRegistry
fields:
- private val snapshotStateSchema
- private val snapshotEnvelopeSchema
- private val featureEnvelopeSchema
- private val ruleEnvelopeSchema
- private val surfaceProfileSchema
- private val surfaceCapabilitySchema
- private val capabilityProfileSchema
- private val targetSelectorNamespaceSchema
- private val targetSelectorFeatureSchema
- private val targetSelectorRuleSchema
- private val targetSelectorScopeSchema
- private val targetSelectorAllSchema
- private val targetSelectorSubsetSchema
- private val targetSelectorSchema
- private val snapshotMutationRequestSchema
- private val namespacePatchRequestSchema
- private val featureCreateRequestSchema
- private val featurePatchRequestSchema
- private val rulePatchRequestSchema
- private val codecStatusSchema
- private val codecPhaseSchema
- private val codecErrorSchema
- private val codecOutcomeSuccessSchema
- private val codecOutcomeFailureSchema
- private val codecOutcomeSchema
- private val mutationEnvelopeSchema
- private val apiErrorSchema
- private val errorEnvelopeSchema
- val components: Map<String, JsonSchema<*>>
methods:
- private fun stringSchema( minLength: Int? = null, enum: List<String>? = null, description: String? = null, ): JsonSchema<String>
- private fun booleanSchema(default: Boolean? = null): JsonSchema<Boolean>
- private fun doubleSchema( minimum: Double? = null, maximum: Double? = null, ): JsonSchema<Double>
- private fun arraySchema(elementSchema: JsonSchema<*>): JsonSchema<List<Any>>
- private fun <V : Any> mapSchema(valueSchema: JsonSchema<V>): JsonSchema<Map<String, V>>
- private fun componentRef(componentName: String): JsonSchema<Any>
