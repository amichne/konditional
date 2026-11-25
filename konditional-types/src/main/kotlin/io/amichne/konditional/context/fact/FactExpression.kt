package io.amichne.konditional.context.fact

import io.amichne.konditional.context.Context

/**
 * A portable predicate with resolved descriptors and typed operands.
 *
 * Evaluation is three-valued. A comparison over a fact the context does not supply is unknown.
 * Unknown stays unknown under [not], so an absent fact can never make an expression match.
 */
sealed class FactExpression {
    /** Stable, format-independent representation for snapshot adapters and explanations. */
    abstract val spec: FactSpec

    /** True only when the supplied context establishes this expression. Unknown does not match. */
    fun matches(context: Context): Boolean = truth(context.facts) == Truth.TRUE

    /**
     * The number of distinct comparisons a context must satisfy.
     *
     * Labels, nesting of like groups, and repeated terms do not change the result:
     * `allOf(a, allOf(a, b))` and `allOf(a, b)` both score two. A disjunction scores its
     * highest branch. A negation scores the same as its operand.
     */
    fun specificity(): Int = canonical().weight()

    internal abstract fun truth(facts: Facts): Truth

    /** Descriptors this expression uses that [schema] did not declare, in term order. */
    internal abstract fun foreignTo(schema: FactSchema): List<Fact<*>>

    /** The same predicate without labels, nested like groups, or repeated terms. */
    internal abstract fun canonical(): FactExpression

    /** Defined on [canonical] forms only. */
    internal abstract fun weight(): Int
}

/** Kleene truth value of an expression in one context. */
internal enum class Truth {
    TRUE,
    FALSE,
    UNKNOWN,
    ;

    fun negated(): Truth = when (this) {
        TRUE -> FALSE
        FALSE -> TRUE
        UNKNOWN -> UNKNOWN
    }
}

internal sealed class Comparison<T : Any> : FactExpression() {
    abstract val fact: Fact<T>

    protected abstract fun holds(value: T): Boolean

    final override fun truth(facts: Facts): Truth {
        val supplied = facts.valuesOf(fact) ?: return Truth.UNKNOWN
        return if (supplied.any(::holds)) Truth.TRUE else Truth.FALSE
    }

    final override fun foreignTo(schema: FactSchema): List<Fact<*>> =
        if (schema.owns(fact)) emptyList() else listOf(fact)

    final override fun canonical(): FactExpression = this
    final override fun weight(): Int = 1
}

internal data class Membership<T : Any>(override val fact: Fact<T>, val operands: List<T>) : Comparison<T>() {
    private val allowed = operands.toSet()
    override val spec: FactSpec.OneOf =
        FactSpec.OneOf(fact.key.id, immutableListCopy(operands.map(fact.domain.encode)))
    override fun holds(value: T): Boolean = value in allowed
}

internal data class LowerBound<T : Any>(override val fact: OrderedFact<T>, val operand: T) : Comparison<T>() {
    override val spec: FactSpec.AtLeast = FactSpec.AtLeast(fact.key.id, fact.domain.encode(operand))
    override fun holds(value: T): Boolean = fact.order.compare(value, operand) >= 0
}

internal data class UpperBound<T : Any>(override val fact: OrderedFact<T>, val operand: T) : Comparison<T>() {
    override val spec: FactSpec.AtMost = FactSpec.AtMost(fact.key.id, fact.domain.encode(operand))
    override fun holds(value: T): Boolean = fact.order.compare(value, operand) <= 0
}

internal data class Conjunction(val terms: List<FactExpression>) : FactExpression() {
    init {
        require(terms.isNotEmpty()) { "allOf needs at least one term" }
    }

    override val spec: FactSpec.All = FactSpec.All(immutableListCopy(terms.map { it.spec }))

    override fun truth(facts: Facts): Truth {
        var result = Truth.TRUE
        for (term in terms) {
            when (term.truth(facts)) {
                Truth.FALSE -> return Truth.FALSE
                Truth.UNKNOWN -> result = Truth.UNKNOWN
                Truth.TRUE -> Unit
            }
        }
        return result
    }

    override fun foreignTo(schema: FactSchema): List<Fact<*>> = terms.flatMap { it.foreignTo(schema) }

    override fun canonical(): FactExpression = terms
        .map { it.canonical() }
        .flatMap { if (it is Conjunction) it.terms else listOf(it) }
        .distinct()
        .let { it.singleOrNull() ?: Conjunction(it) }

    override fun weight(): Int = terms.sumOf { it.weight() }
}

internal data class Disjunction(val terms: List<FactExpression>) : FactExpression() {
    init {
        require(terms.isNotEmpty()) { "anyOf needs at least one term" }
    }

    override val spec: FactSpec.AnyOf = FactSpec.AnyOf(immutableListCopy(terms.map { it.spec }))

    override fun truth(facts: Facts): Truth {
        var result = Truth.FALSE
        for (term in terms) {
            when (term.truth(facts)) {
                Truth.TRUE -> return Truth.TRUE
                Truth.UNKNOWN -> result = Truth.UNKNOWN
                Truth.FALSE -> Unit
            }
        }
        return result
    }

    override fun foreignTo(schema: FactSchema): List<Fact<*>> = terms.flatMap { it.foreignTo(schema) }

    override fun canonical(): FactExpression = terms
        .map { it.canonical() }
        .flatMap { if (it is Disjunction) it.terms else listOf(it) }
        .distinct()
        .let { it.singleOrNull() ?: Disjunction(it) }

    override fun weight(): Int = terms.maxOf { it.weight() }
}

internal data class Negation(val term: FactExpression) : FactExpression() {
    override val spec: FactSpec.Not = FactSpec.Not(term.spec)
    override fun truth(facts: Facts): Truth = term.truth(facts).negated()
    override fun foreignTo(schema: FactSchema): List<Fact<*>> = term.foreignTo(schema)
    override fun canonical(): FactExpression = Negation(term.canonical())
    override fun weight(): Int = term.weight()
}

internal data class Labeled(val label: String, val term: FactExpression) : FactExpression() {
    init {
        require(label.isNotBlank()) { "A condition label must not be blank" }
    }

    override val spec: FactSpec.Named = FactSpec.Named(label, term.spec)
    override fun truth(facts: Facts): Truth = term.truth(facts)
    override fun foreignTo(schema: FactSchema): List<Fact<*>> = term.foreignTo(schema)
    override fun canonical(): FactExpression = term.canonical()
    override fun weight(): Int = term.weight()
}

/** Matches when every term matches. Preserves term order in the portable form. */
fun allOf(first: FactExpression, vararg rest: FactExpression): FactExpression =
    Conjunction(immutableListCopy(listOf(first) + rest))

/** Matches when every term matches. An empty collection is a declaration error. */
fun allOf(terms: Collection<FactExpression>): FactExpression = Conjunction(immutableListCopy(terms))

/** Matches when at least one term matches. Preserves term order in the portable form. */
fun anyOf(first: FactExpression, vararg rest: FactExpression): FactExpression =
    Disjunction(immutableListCopy(listOf(first) + rest))

/** Matches when at least one term matches. An empty collection is a declaration error. */
fun anyOf(terms: Collection<FactExpression>): FactExpression = Disjunction(immutableListCopy(terms))

/**
 * Matches when the context supplies every fact [term] needs and [term] does not hold.
 * When a needed fact is absent, neither [term] nor its negation matches.
 */
fun not(term: FactExpression): FactExpression = Negation(term)

/**
 * Attaches a domain name that explanations and snapshots carry.
 * The label does not change matching or specificity. A blank label is a declaration error.
 */
fun named(label: String, term: FactExpression): FactExpression = Labeled(label, term)
