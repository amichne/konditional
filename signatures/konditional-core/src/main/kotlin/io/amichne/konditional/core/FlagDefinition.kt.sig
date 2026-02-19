file=konditional-core/src/main/kotlin/io/amichne/konditional/core/FlagDefinition.kt
package=io.amichne.konditional.core
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.context.Context,io.amichne.konditional.context.Context.StableIdContext,io.amichne.konditional.core.evaluation.Bucketing,io.amichne.konditional.core.features.Feature,io.amichne.konditional.core.id.HexId,io.amichne.konditional.rules.ConditionalValue,io.amichne.konditional.rules.Rule
type=io.amichne.konditional.core.FlagDefinition|kind=class|decl=data class FlagDefinition<T : Any, C : Context, M : Namespace>( /** * The default value returned when no targeting rules match or the flag is inactive. */ val defaultValue: T, val feature: Feature<T, C, M>, val values: List<ConditionalValue<T, C>> = listOf(), val isActive: Boolean = true, val salt: String = "v1", internal val rampUpAllowlist: Set<HexId> = emptySet(), )
type=io.amichne.konditional.core.Trace|kind=class|decl=internal data class Trace<T : Any, C : Context> internal constructor( val value: T, val bucket: Int?, val matched: ConditionalValue<T, C>?, val skippedByRampUp: ConditionalValue<T, C>?, )
type=io.amichne.konditional.core.EvaluationState|kind=class|decl=private data class EvaluationState<T : Any, C : Context>( var bucket: Int? = null, var skippedByRampUp: ConditionalValue<T, C>? = null, )
type=io.amichne.konditional.core.EvaluationInputs|kind=class|decl=private data class EvaluationInputs<C : Context>( val context: C, val stableId: HexId?, val fallbackBucket: Int, val isFlagAllowlisted: Boolean, )
fields:
- internal val valuesByPrecedence: List<ConditionalValue<T, C>>
methods:
- internal fun evaluate(context: C): T
- internal fun evaluateTrace(context: C): Trace<T, C>
- private fun evaluateCandidate( candidate: ConditionalValue<T, C>, inputs: EvaluationInputs<C>, state: EvaluationState<T, C>, ): Trace<T, C>?
- private fun isRampUpEligible( stableId: HexId?, isFlagAllowlisted: Boolean, candidate: ConditionalValue<T, C>, computedBucket: Int, ): Boolean
