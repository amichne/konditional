package io.amichne.konditional.core.dsl.rules

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.registry.AxisCatalog
import io.amichne.konditional.internal.builders.RuleBuilder

@KonditionalDsl
class RuleSetBuilder<T : Any, C : Context> @PublishedApi internal constructor(
    private val axisCatalog: AxisCatalog? = null,
) {
    private val rules = mutableListOf<RuleSpec<T, C>>()

    fun rule(
        value: T,
        build: RuleScope<C>.() -> Unit = {},
    ) {
        val rule = RuleBuilder<C>(axisCatalog = axisCatalog).apply(build).build()
        rules += RuleSpec(value, rule)
    }

    fun ruleScoped(
        value: T,
        build: ContextRuleScope<C>.() -> Unit = {},
    ) {
        val rule = RuleBuilder<C>(axisCatalog = axisCatalog).apply {
            @Suppress("UNCHECKED_CAST")
            (this as ContextRuleScope<C>).apply(build)
        }.build()
        rules += RuleSpec(value, rule)
    }

    @PublishedApi
    internal fun build(): List<RuleSpec<T, C>> = rules.toList()
}
