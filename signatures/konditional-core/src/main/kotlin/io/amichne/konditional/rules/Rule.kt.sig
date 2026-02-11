file=konditional-core/src/main/kotlin/io/amichne/konditional/rules/Rule.kt
package=io.amichne.konditional.rules
imports=io.amichne.konditional.context.Context,io.amichne.konditional.context.RampUp,io.amichne.konditional.core.id.HexId,io.amichne.konditional.rules.evaluable.AxisConstraint,io.amichne.konditional.rules.evaluable.BasePredicate,io.amichne.konditional.rules.evaluable.Placeholder,io.amichne.konditional.rules.evaluable.Predicate,io.amichne.konditional.rules.versions.Unbounded,io.amichne.konditional.rules.versions.VersionRange
type=io.amichne.konditional.rules.Rule|kind=class|decl=data class Rule<in C : Context> internal constructor( val rampUp: RampUp = RampUp.default, internal val rampUpAllowlist: Set<HexId> = emptySet(), val note: String? = null, internal val targeting: BasePredicate<@UnsafeVariance C> = BasePredicate(), val predicate: Predicate<@UnsafeVariance C> = Placeholder, ) : Predicate<C>
methods:
- override fun matches(context: C): Boolean
- override fun specificity(): Int
