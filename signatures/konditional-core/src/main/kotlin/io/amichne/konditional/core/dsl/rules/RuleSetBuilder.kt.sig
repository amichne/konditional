file=konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/rules/RuleSetBuilder.kt
package=io.amichne.konditional.core.dsl.rules
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.context.Context,io.amichne.konditional.core.dsl.KonditionalDsl,io.amichne.konditional.core.registry.AxisCatalog,io.amichne.konditional.internal.builders.RuleBuilder
type=io.amichne.konditional.core.dsl.rules.RuleSetBuilder|kind=class|decl=class RuleSetBuilder<T : Any, C : Context> @PublishedApi internal constructor( private val axisCatalog: AxisCatalog? = null, )
fields:
- private val rules
methods:
- fun rule( value: T, build: RuleScope<C>.() -> Unit = {}, )
- fun ruleScoped( value: T, build: ContextRuleScope<C>.() -> Unit = {}, )
- internal fun build(): List<RuleSpec<T, C>>
