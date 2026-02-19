file=konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/rules/RuleSet.kt
package=io.amichne.konditional.core.dsl.rules
imports=io.amichne.konditional.context.Context,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.dsl.KonditionalDsl,io.amichne.konditional.core.features.Feature
type=io.amichne.konditional.core.dsl.rules.RuleSet|kind=class|decl=class RuleSet<RC : Context, T : Any, C, M : Namespace> @PublishedApi internal constructor( val feature: Feature<T, C, M>, internal val rules: List<RuleSpec<T, RC>>, ) where C : RC
methods:
- operator fun plus(other: RuleSet<RC, T, C, M>): RuleSet<RC, T, C, M>
