package io.amichne.konditional.rules.predicate

import io.amichne.konditional.context.Context

/**
 * Predicate-like value that can be composed into a [PredicateExpression].
 *
 * This is a code-only API for building explicit boolean predicate trees before
 * attaching them to a rule with `require(...)`.
 */
interface PredicateLike<C : Context> {
    fun toPredicateExpression(): PredicateExpression<C>
}

/**
 * Code-only boolean predicate expression.
 *
 * Expressions are normalized structurally:
 * - nested [allOf] groups are flattened
 * - nested [anyOf] groups are flattened
 * - double negation collapses
 *
 * Mixed conjunction/disjunction precedence is always explicit. Use nested
 * [allOf] / [anyOf] calls to group expressions rather than relying on infix precedence.
 *
 * Snapshot note: predicate-expression structure is not serialized. This API is
 * intended for in-process DSL composition.
 */
class PredicateExpression<C : Context> @PublishedApi internal constructor(
    @PublishedApi internal val node: Node<C>,
) : PredicateLike<C> {
    override fun toPredicateExpression(): PredicateExpression<C> = this

    @PublishedApi
    internal sealed interface Node<C : Context> {
        data class Inline<C : Context>(
            val block: C.() -> Boolean,
        ) : Node<C>

        data class Named<C : Context>(
            val predicate: NamespacePredicate<C>,
        ) : Node<C>

        data class Not<C : Context>(
            val expression: PredicateExpression<C>,
        ) : Node<C>

        data class All<C : Context>(
            val expressions: List<PredicateExpression<C>>,
        ) : Node<C>

        data class AnyOf<C : Context>(
            val expressions: List<PredicateExpression<C>>,
        ) : Node<C>
    }
}

/**
 * Creates an inline predicate expression from [block].
 */
fun <C : Context> predicateOf(block: C.() -> Boolean): PredicateExpression<C> =
    PredicateExpression(PredicateExpression.Node.Inline(block))

/**
 * Concise alias for [predicateOf] intended for rule DSL composition.
 */
fun <C : Context> where(block: C.() -> Boolean): PredicateExpression<C> = predicateOf(block)

/**
 * Explicit AND-grouping for predicate expressions.
 *
 * Use this to compose mixed boolean expressions without relying on operator precedence.
 */
fun <C : Context> allOf(vararg predicates: PredicateLike<C>): PredicateExpression<C> =
    allOf(predicates.asList())

/**
 * Explicit AND-grouping for predicate expressions.
 */
fun <C : Context> allOf(predicates: Iterable<PredicateLike<C>>): PredicateExpression<C> {
    val normalized = buildList {
        predicates.forEach { predicate ->
            when (val node = predicate.toPredicateExpression().node) {
                is PredicateExpression.Node.All -> addAll(node.expressions)
                else -> add(predicate.toPredicateExpression())
            }
        }
    }
    return when (normalized.size) {
        0 -> PredicateExpression(PredicateExpression.Node.All(emptyList()))
        1 -> normalized.single()
        else -> PredicateExpression(PredicateExpression.Node.All(normalized))
    }
}

/**
 * Explicit OR-grouping for predicate expressions.
 *
 * Use this to compose mixed boolean expressions without relying on operator precedence.
 */
fun <C : Context> anyOf(vararg predicates: PredicateLike<C>): PredicateExpression<C> =
    anyOf(predicates.asList())

/**
 * Explicit OR-grouping for predicate expressions.
 */
fun <C : Context> anyOf(predicates: Iterable<PredicateLike<C>>): PredicateExpression<C> {
    val normalized = buildList {
        predicates.forEach { predicate ->
            when (val node = predicate.toPredicateExpression().node) {
                is PredicateExpression.Node.AnyOf -> addAll(node.expressions)
                else -> add(predicate.toPredicateExpression())
            }
        }
    }
    return when (normalized.size) {
        0 -> PredicateExpression(PredicateExpression.Node.AnyOf(emptyList()))
        1 -> normalized.single()
        else -> PredicateExpression(PredicateExpression.Node.AnyOf(normalized))
    }
}

/**
 * Unary negation operator for predicate-like values.
 *
 * `!` is normalized so `!!x == x`.
 */
operator fun <C : Context> PredicateLike<C>.not(): PredicateExpression<C> =
    when (val node = toPredicateExpression().node) {
        is PredicateExpression.Node.Not -> node.expression
        else -> PredicateExpression(PredicateExpression.Node.Not(toPredicateExpression()))
    }

/**
 * Infix AND sugar for predicate expressions.
 *
 * Parenthesize mixed `and` / `or` expressions explicitly; Kotlin infix-call
 * precedence does not match boolean operator precedence.
 */
infix fun <C : Context> PredicateLike<C>.and(other: PredicateLike<C>): PredicateExpression<C> =
    allOf(this, other)

/**
 * Infix OR sugar for predicate expressions.
 *
 * Parenthesize mixed `and` / `or` expressions explicitly; Kotlin infix-call
 * precedence does not match boolean operator precedence.
 */
infix fun <C : Context> PredicateLike<C>.or(other: PredicateLike<C>): PredicateExpression<C> =
    anyOf(this, other)
