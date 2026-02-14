file=openfeature/src/main/kotlin/io/amichne/konditional/openfeature/KonditionalOpenFeatureProvider.kt
package=io.amichne.konditional.openfeature
imports=dev.openfeature.sdk.ErrorCode,dev.openfeature.sdk.EvaluationContext,dev.openfeature.sdk.FeatureProvider,dev.openfeature.sdk.ImmutableMetadata,dev.openfeature.sdk.Metadata,dev.openfeature.sdk.ProviderEvaluation,dev.openfeature.sdk.ProviderState,dev.openfeature.sdk.Reason,dev.openfeature.sdk.Value,io.amichne.konditional.api.EvaluationResult,io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.api.explain,io.amichne.konditional.context.Context,io.amichne.konditional.context.axis.AxisValues,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.features.Feature,io.amichne.konditional.core.id.StableId,io.amichne.konditional.core.registry.NamespaceRegistry
type=io.amichne.konditional.openfeature.KonditionalContextMapper|kind=interface|decl=fun interface KonditionalContextMapper<C : Context>
type=io.amichne.konditional.openfeature.KonditionalContextMappingResult|kind=interface|decl=sealed interface KonditionalContextMappingResult<out C : Context>
type=io.amichne.konditional.openfeature.Success|kind=class|decl=data class Success<C : Context> @PublishedApi internal constructor( val value: C, ) : KonditionalContextMappingResult<C>
type=io.amichne.konditional.openfeature.Failure|kind=class|decl=data class Failure @PublishedApi internal constructor( val error: KonditionalContextMappingError, ) : KonditionalContextMappingResult<Nothing>
type=io.amichne.konditional.openfeature.KonditionalContextMappingError|kind=interface|decl=sealed interface KonditionalContextMappingError
type=io.amichne.konditional.openfeature.MissingTargetingKey|kind=object|decl=object MissingTargetingKey : KonditionalContextMappingError
type=io.amichne.konditional.openfeature.BlankTargetingKey|kind=object|decl=object BlankTargetingKey : KonditionalContextMappingError
type=io.amichne.konditional.openfeature.KonditionalProviderMetadata|kind=class|decl=data class KonditionalProviderMetadata(private val providerName: String = "Konditional") : Metadata
type=io.amichne.konditional.openfeature.TargetingKeyContext|kind=class|decl=data class TargetingKeyContext( override val stableId: StableId, override val axisValues: AxisValues = AxisValues.EMPTY, ) : Context, Context.StableIdContext
type=io.amichne.konditional.openfeature.TargetingKeyContextMapper|kind=class|decl=class TargetingKeyContextMapper( private val axisValuesProvider: (EvaluationContext) -> AxisValues = { AxisValues.EMPTY }, ) : KonditionalContextMapper<TargetingKeyContext>
type=io.amichne.konditional.openfeature.KonditionalOpenFeatureProvider|kind=class|decl=class KonditionalOpenFeatureProvider<C : Context>( private val namespaceRegistry: NamespaceRegistry, private val contextMapper: KonditionalContextMapper<C>, private val metadata: Metadata = KonditionalProviderMetadata(), ) : FeatureProvider
type=io.amichne.konditional.openfeature.FlagValueType|kind=enum|decl=private enum class FlagValueType
type=io.amichne.konditional.openfeature.FlagEntry|kind=class|decl=private data class FlagEntry<C : Context>( val feature: Feature<*, C, *>, val valueType: FlagValueType, )
fields:
- val message: String
- override val message: String
- override val message: String
- private val flagsByKey: Map<String, FlagEntry<C>>
methods:
- override fun getName(): String
- override fun toKonditionalContext(context: EvaluationContext): KonditionalContextMappingResult<TargetingKeyContext>
- override fun getMetadata(): Metadata
- override fun getState(): ProviderState
- override fun getBooleanEvaluation( key: String, defaultValue: Boolean, ctx: EvaluationContext, ): ProviderEvaluation<Boolean>
- override fun getStringEvaluation( key: String, defaultValue: String, ctx: EvaluationContext, ): ProviderEvaluation<String>
- override fun getIntegerEvaluation( key: String, defaultValue: Int, ctx: EvaluationContext, ): ProviderEvaluation<Int>
- override fun getDoubleEvaluation( key: String, defaultValue: Double, ctx: EvaluationContext, ): ProviderEvaluation<Double>
- override fun getObjectEvaluation( key: String, defaultValue: Value, ctx: EvaluationContext, ): ProviderEvaluation<Value>
- private fun <T : Any> evaluateTyped( key: String, defaultValue: T, ctx: EvaluationContext, expectedType: FlagValueType, transformValue: (Any) -> T?, ): ProviderEvaluation<T>
- private fun <T : Any> evaluateEntry( key: String, entry: FlagEntry<C>, defaultValue: T, ctx: EvaluationContext, transformValue: (Any) -> T?, ): ProviderEvaluation<T>
- private fun <T : Any> evaluateMappedEntry( key: String, entry: FlagEntry<C>, context: C, defaultValue: T, transformValue: (Any) -> T?, ): ProviderEvaluation<T>
- private fun reasonFor(decision: EvaluationResult.Decision): Reason
- private fun variantFor(decision: EvaluationResult.Decision): String?
- private fun metadataFor(result: EvaluationResult<*>): ImmutableMetadata
- private fun ImmutableMetadata.ImmutableMetadataBuilder.addDecisionMetadata( decision: EvaluationResult.Decision, ): ImmutableMetadata.ImmutableMetadataBuilder
- private fun ImmutableMetadata.ImmutableMetadataBuilder.addStringIfNotNull( key: String, value: String?, ): ImmutableMetadata.ImmutableMetadataBuilder
- private fun ImmutableMetadata.ImmutableMetadataBuilder.addIntegerIfNotNull( key: String, value: Int?, ): ImmutableMetadata.ImmutableMetadataBuilder
- private fun <T : Any> errorEvaluation( defaultValue: T, errorCode: ErrorCode, errorMessage: String, ): ProviderEvaluation<T>
- private fun resolveFlagEntry(flagKey: String): FlagEntry<C>?
- private fun Feature<*, *, *>.toTypedFeature(): Feature<*, C, *>
- private fun <T : Any> FlagEntry<C>.featureAs(): Feature<T, C, Namespace>
- private fun <T> ProviderEvaluation.ProviderEvaluationBuilder<T>.variantOrNull( variant: String?, ): ProviderEvaluation.ProviderEvaluationBuilder<T>
- fun displayName(): String
