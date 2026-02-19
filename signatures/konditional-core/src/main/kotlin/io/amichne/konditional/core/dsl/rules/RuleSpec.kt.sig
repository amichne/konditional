file=konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/rules/RuleSpec.kt
package=io.amichne.konditional.core.dsl.rules
imports=io.amichne.konditional.context.Context,io.amichne.konditional.core.dsl.KonditionalDsl,io.amichne.konditional.rules.Rule
type=io.amichne.konditional.core.dsl.rules.RuleSpec|kind=class|decl=data class RuleSpec<out T : Any, in C : Context> @PublishedApi internal constructor( val value: T, val rule: Rule<C>, )
