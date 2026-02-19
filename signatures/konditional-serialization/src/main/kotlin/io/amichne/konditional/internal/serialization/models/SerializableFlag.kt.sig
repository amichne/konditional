file=konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/models/SerializableFlag.kt
package=io.amichne.konditional.internal.serialization.models
imports=com.squareup.moshi.JsonClass,io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.context.Context,io.amichne.konditional.core.FlagDefinition,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.features.Feature,io.amichne.konditional.core.result.ParseError,io.amichne.konditional.core.result.parseFailure,io.amichne.konditional.core.schema.CompiledNamespaceSchema,io.amichne.konditional.core.types.Konstrained,io.amichne.konditional.core.types.asObjectSchema,io.amichne.konditional.internal.SerializedFlagDefinitionMetadata,io.amichne.konditional.internal.SerializedFlagRuleSpec,io.amichne.konditional.internal.flagDefinitionFromSerialized,io.amichne.konditional.internal.toSerializedMetadata,io.amichne.konditional.internal.toSerializedRules,io.amichne.konditional.values.FeatureId,io.amichne.kontracts.schema.ObjectTraits
type=io.amichne.konditional.internal.serialization.models.SerializableFlag|kind=class|decl=data class SerializableFlag( val key: FeatureId, val defaultValue: FlagValue<*>, val salt: String = "v1", val isActive: Boolean = true, val rampUpAllowlist: Set<String> = emptySet(), val rules: List<SerializableRule> = emptyList(), )
methods:
- fun toFlagPair( schema: CompiledNamespaceSchema, ): Result<Pair<Feature<*, *, *>, FlagDefinition<*, *, *>>>
- private fun resolveFeature( schema: CompiledNamespaceSchema, ): Result<Feature<*, *, *>>
- private fun <T : Any, C : Context, M : Namespace> toFlagDefinition( conditional: Feature<T, C, M>, ): FlagDefinition<T, C, M>
- private fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.expectedDefaultValueOrNull(): T?
- private fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.declaredDefaultValueOrNull(): T?
