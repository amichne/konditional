@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core.dsl

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.rules.ContextRuleScope
import io.amichne.konditional.core.dsl.rules.RuleScope
import io.amichne.konditional.rules.RuleValue

/**
 * Defines a rule that computes its value from the evaluation context.
 */
fun <T : Any, C : Context, M : Namespace> FlagScope<T, C, M>.rule(
    valueProvider: C.() -> T,
    build: RuleScope<C>.() -> Unit = {},
) = ruleValue(RuleValue.contextual(valueProvider), build)

/**
 * Defines a composable rule that computes its value from the evaluation context.
 */
fun <T : Any, C : Context, M : Namespace> FlagScope<T, C, M>.ruleScoped(
    valueProvider: C.() -> T,
    build: ContextRuleScope<C>.() -> Unit = {},
) = ruleScopedValue(RuleValue.contextual(valueProvider), build)
