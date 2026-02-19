file=konditional-core/src/main/kotlin/io/amichne/konditional/rules/Rule.kt
package=io.amichne.konditional.rules
imports=io.amichne.konditional.context.Context,io.amichne.konditional.context.RampUp,io.amichne.konditional.core.id.HexId,io.amichne.konditional.rules.targeting.Targeting
type=io.amichne.konditional.rules.Rule|kind=class|decl=data class Rule<in C : Context> internal constructor( val rampUp: RampUp = RampUp.default, internal val rampUpAllowlist: Set<HexId> = emptySet(), val note: String? = null, val targeting: Targeting.All<@UnsafeVariance C> = Targeting.catchAll(), )
methods:
- fun matches(context: C): Boolean
- fun specificity(): Int
