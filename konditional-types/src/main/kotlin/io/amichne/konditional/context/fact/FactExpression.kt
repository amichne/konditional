package io.amichne.konditional.context.fact

import io.amichne.konditional.context.Context

/** A portable predicate with resolved descriptors and typed operands. */
sealed class FactExpression {
    /** Stable, format-independent representation for snapshot adapters and explanations. */
    abstract val spec: FactSpec

    /** Evaluates only the supplied context. */
    abstract fun matches(context: Context): Boolean

    /** Membership and bound comparisons contribute one; conjunction sums, disjunction takes the maximum. */
    abstract fun specificity(): Int

    internal abstract fun declaredIn(schema: FactSchema): Boolean
}

internal sealed class Comparison<T : Any> : FactExpression() {
    abstract val fact: Fact<T>
    final override fun specificity(): Int = 1
    final override fun declaredIn(schema: FactSchema): Boolean = schema.owns(fact)
}

internal data class Membership<T : Any>(override val fact: Fact<T>, val operands: List<T>) : Comparison<T>() {
    private val allowed = operands.toSet()
    override val spec: FactSpec.OneOf =
        FactSpec.OneOf(fact.key.id, immutableListCopy(operands.map(fact.domain.encode)))
    override fun matches(context: Context): Boolean = context.facts.matches(fact, allowed::contains)
}

internal data class LowerBound<T : Any>(override val fact: OrderedFact<T>, val operand: T) : Comparison<T>() {
    override val spec: FactSpec.AtLeast = FactSpec.AtLeast(fact.key.id, fact.domain.encode(operand))
    override fun matches(context: Context): Boolean =
        context.facts.matches(fact) { fact.order.compare(it, operand) >= 0 }
}

internal data class UpperBound<T : Any>(override val fact: OrderedFact<T>, val operand: T) : Comparison<T>() {
    override val spec: FactSpec.AtMost = FactSpec.AtMost(fact.key.id, fact.domain.encode(operand))
    override fun matches(context: Context): Boolean =
        context.facts.matches(fact) { fact.order.compare(it, operand) <= 0 }
}

internal data class Conjunction(val terms: List<FactExpression>) : FactExpression() {
    override val spec: FactSpec.All = FactSpec.All(immutableListCopy(terms.map { it.spec }))
    override fun matches(context: Context): Boolean = terms.all { it.matches(context) }
    override fun specificity(): Int = terms.sumOf { it.specificity() }
    override fun declaredIn(schema: FactSchema): Boolean = terms.all { it.declaredIn(schema) }
}

internal data class Disjunction(val terms: List<FactExpression>) : FactExpression() {
    override val spec: FactSpec.AnyOf = FactSpec.AnyOf(immutableListCopy(terms.map { it.spec }))
    override fun matches(context: Context): Boolean = terms.any { it.matches(context) }
    override fun specificity(): Int = terms.maxOfOrNull { it.specificity() } ?: 0
    override fun declaredIn(schema: FactSchema): Boolean = terms.all { it.declaredIn(schema) }
}

/** Matches when all terms match, including the empty conjunction. Preserves term order and duplicates. */
fun allOf(vararg terms: FactExpression): FactExpression = Conjunction(immutableListCopy(terms.asList()))

/** Matches when at least one term matches. An empty disjunction misses. */
fun anyOf(vararg terms: FactExpression): FactExpression = Disjunction(immutableListCopy(terms.asList()))
