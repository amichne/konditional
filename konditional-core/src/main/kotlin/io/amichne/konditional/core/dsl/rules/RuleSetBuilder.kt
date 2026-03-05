@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core.dsl.rules

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.internal.builders.RuleBuilder
import io.amichne.konditional.rules.predicate.PredicateRef
import io.amichne.konditional.rules.targeting.Targeting

@KonditionalDsl
class RuleSetBuilder<T : Any, C : Context> @PublishedApi internal constructor(
    private val predicateResolver: ((PredicateRef) -> Result<Targeting.Custom<C>>)? = null,
) {
    private val rules = mutableListOf<RuleSpec<T, C>>()

    fun rule(
        value: T,
        build: RuleScope<C>.() -> Unit = {},
    ) {
        val rule = RuleBuilder(predicateResolver = predicateResolver).apply(build).build()
        rules += RuleSpec(value, rule)
    }

    @KonditionalInternalApi
    fun ruleScoped(
        value: T,
        build: ContextRuleScope<C>.() -> Unit = {},
    ) {
        val rule = RuleBuilder(predicateResolver = predicateResolver).apply {
            @Suppress("UNCHECKED_CAST")
            (this as ContextRuleScope<C>).apply(build)
        }.build()
        rules += RuleSpec(value, rule)
    }

    @PublishedApi
    internal fun build(): List<RuleSpec<T, C>> = rules.toList()
}
