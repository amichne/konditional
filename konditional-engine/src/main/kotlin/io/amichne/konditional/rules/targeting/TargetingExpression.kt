package io.amichne.konditional.rules.targeting

import io.amichne.konditional.context.Context

/**
 * A portable condition for the evaluation context [C]. Create conditions with
 * [ContextAttribute.isOneOf] or [NamedPredicate.invoke] and compose them with [allOf] and [anyOf].
 */
class TargetingExpression<in C : Context> internal constructor(internal val node: Targeting<C>)

/** Matches when all supplied conditions match the evaluation context. */
fun <C : Context> allOf(
    first: TargetingExpression<C>,
    vararg others: TargetingExpression<C>,
): TargetingExpression<C> = TargetingExpression(Targeting.All(children(first, others)))

/** Matches when at least one supplied condition matches the evaluation context. */
fun <C : Context> anyOf(
    first: TargetingExpression<C>,
    vararg others: TargetingExpression<C>,
): TargetingExpression<C> = TargetingExpression(Targeting.AnyOf(children(first, others)))

private fun <C : Context> children(
    first: TargetingExpression<C>,
    others: Array<out TargetingExpression<C>>,
): List<Targeting<C>> = java.util.List.copyOf(buildList {
    add(first.node)
    others.mapTo(this) { it.node }
})
