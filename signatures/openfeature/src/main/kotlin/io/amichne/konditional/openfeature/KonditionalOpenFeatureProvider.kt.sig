file=openfeature/src/main/kotlin/io/amichne/konditional/openfeature/KonditionalOpenFeatureProvider.kt
package=io.amichne.konditional.openfeature
imports=dev.openfeature.sdk.ErrorCode,dev.openfeature.sdk.EvaluationContext,dev.openfeature.sdk.FeatureProvider,dev.openfeature.sdk.ImmutableMetadata,dev.openfeature.sdk.Metadata,dev.openfeature.sdk.ProviderEvaluation,dev.openfeature.sdk.ProviderState,dev.openfeature.sdk.Reason,dev.openfeature.sdk.Value,io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.api.evaluateInternalApi,io.amichne.konditional.context.Context,io.amichne.konditional.context.axis.AxisValues,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.features.Feature,io.amichne.konditional.core.id.StableId,io.amichne.konditional.core.ops.Metrics,io.amichne.konditional.core.registry.NamespaceRegistry,io.amichne.konditional.internal.evaluation.EvaluationDiagnostics
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
fields:
- val message: String
- override val message: String
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
