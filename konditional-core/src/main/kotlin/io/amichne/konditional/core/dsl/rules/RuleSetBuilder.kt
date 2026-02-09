package io.amichne.konditional.core.dsl.rules

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.internal.builders.RuleBuilder
import io.amichne.konditional.rules.RuleValue

@KonditionalDsl
class RuleSetBuilder<T : Any, C : Context> @PublishedApi internal constructor() {
    private val rules = mutableListOf<RuleSpec<T, C>>()

    fun rule(
        value: T,
        build: RuleScope<C>.() -> Unit = {},
    ) {
        val rule = RuleBuilder<C>().apply(build).build()
        rules += RuleSpec(RuleValue.fixed(value), rule)
    }

    fun rule(
        resolver: C.() -> T,
        build: RuleScope<C>.() -> Unit = {},
    ) {
        val rule = RuleBuilder<C>().apply(build).build()
        rules += RuleSpec(RuleValue.contextual(resolver), rule)
    }

    fun ruleScoped(
        value: T,
        build: ContextRuleScope<C>.() -> Unit = {},
    ) {
        val rule = RuleBuilder<C>().apply {
            @Suppress("UNCHECKED_CAST")
            (this as ContextRuleScope<C>).apply(build)
        }.build()
        rules += RuleSpec(RuleValue.fixed(value), rule)
    }

    fun ruleScoped(
        resolver: C.() -> T,
        build: ContextRuleScope<C>.() -> Unit = {},
    ) {
        val rule = RuleBuilder<C>().apply {
            @Suppress("UNCHECKED_CAST")
            (this as ContextRuleScope<C>).apply(build)
        }.build()
        rules += RuleSpec(RuleValue.contextual(resolver), rule)
    }

    @PublishedApi
    internal fun build(): List<RuleSpec<T, C>> = rules.toList()
}
