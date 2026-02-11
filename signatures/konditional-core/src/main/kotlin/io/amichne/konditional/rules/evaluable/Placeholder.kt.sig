file=konditional-core/src/main/kotlin/io/amichne/konditional/rules/evaluable/Placeholder.kt
package=io.amichne.konditional.rules.evaluable
imports=io.amichne.konditional.context.Context
type=io.amichne.konditional.rules.evaluable.Placeholder|kind=object|decl=data object Placeholder : Predicate<Context>
methods:
- override fun matches(context: Context): Boolean
- override fun specificity(): Int
