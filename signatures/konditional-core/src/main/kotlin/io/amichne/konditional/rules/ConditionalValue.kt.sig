file=konditional-core/src/main/kotlin/io/amichne/konditional/rules/ConditionalValue.kt
package=io.amichne.konditional.rules
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.context.Context
type=io.amichne.konditional.rules.ConditionalValue|kind=class|decl=data class ConditionalValue<T : Any, C : Context> private constructor( val rule: Rule<C>, val value: T, )
