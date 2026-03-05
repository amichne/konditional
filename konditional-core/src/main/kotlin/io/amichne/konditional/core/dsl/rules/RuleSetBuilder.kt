@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core.dsl.rules

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.internal.builders.RuleBuilder
import io.amichne.konditional.rules.predicate.PredicateRef
import io.amichne.konditional.rules.targeting.Targeting
import io.amichne.konditional.values.NamespaceId
import io.amichne.konditional.values.RuleId

@KonditionalDsl
class RuleSetBuilder<T : Any, C : Context> @PublishedApi internal constructor(
    private val ruleIdFactory: (Int) -> RuleId = { RuleId.unspecified },
    private val namespaceId: NamespaceId? = null,
    private val predicateResolver: ((PredicateRef) -> Result<Targeting.Custom<C>>)? = null,
    private val predicateRegistrar: ((PredicateRef.Registered, Targeting.Custom<C>) -> Unit)? = null,
) {
    private val rules = mutableListOf<RuleSpec<T, C>>()
    private var nextRuleOrdinal: Int = 0

    fun rule(
        value: T,
        build: RuleScope<C>.() -> Unit = {},
    ) {
        val rule = RuleBuilder(
            ruleId = ruleIdFactory(nextRuleOrdinal++),
            namespaceId = namespaceId,
            predicateResolver = predicateResolver,
            predicateRegistrar = predicateRegistrar,
        ).apply(build).build()
        rules += RuleSpec(value, rule)
    }

    @KonditionalInternalApi
    fun ruleScoped(
        value: T,
        build: ContextRuleScope<C>.() -> Unit = {},
    ) {
        val rule = RuleBuilder(
            ruleId = ruleIdFactory(nextRuleOrdinal++),
            namespaceId = namespaceId,
            predicateResolver = predicateResolver,
            predicateRegistrar = predicateRegistrar,
        ).apply {
            @Suppress("UNCHECKED_CAST")
            (this as ContextRuleScope<C>).apply(build)
        }.build()
        rules += RuleSpec(value, rule)
    }

    @PublishedApi
    internal fun build(): List<RuleSpec<T, C>> = rules.toList()
}
