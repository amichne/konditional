package io.amichne.konditional.context.fact

import io.amichne.konditional.context.ContextKey
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult

/** A namespace's declaration of a typed input. Different declaration instances denote different facts. */
sealed class Fact<T : Any> protected constructor(id: String, internal val domain: FactDomain<T>) {
    /** Stable namespace-local key, independent of Kotlin names. */
    val key: ContextKey = ContextKey(id)

    /** Matches a selected value equal to one of the supplied operands. Missing facts never match. */
    fun oneOf(first: T, vararg rest: T): FactExpression = membership(listOf(first) + rest)

    internal fun membership(values: List<T>): FactExpression =
        Membership(this, immutableListCopy(values.distinct().sortedBy(domain.encode)))

    internal fun parse(raw: String): ParseResult<T> = when (val parsed = domain.decode(raw)) {
        is ParseResult.Success -> parsed
        is ParseResult.Failure -> ParseResult.Failure(ParseError.InvalidFactOperand(key, parsed.error))
    }
}

/** A context can supply exactly one value for an assigned single-valued fact. */
class SingleFact<T : Any>(id: String, domain: FactDomain<T>) : Fact<T>(id, domain) {
    /** Supplies this fact in a context. */
    fun value(value: T): FactValue<T> = FactValue(this, listOf(value))
}

/** Membership tests a set-valued fact for at least one common value. */
class MultiFact<T : Any>(id: String, domain: FactDomain<T>) : Fact<T>(id, domain) {
    /** Copies the selection so later caller mutations cannot change the context. */
    fun values(values: Set<T>): FactValue<T> = FactValue(this, values)
}

/** A single-valued fact with a domain-defined, deterministic total order. */
class OrderedFact<T : Any>(
    id: String,
    domain: FactDomain<T>,
    internal val order: Comparator<T>,
) : Fact<T>(id, domain) {
    /** Supplies the value to compare. */
    fun value(value: T): FactValue<T> = FactValue(this, listOf(value))

    /** Inclusive lower bound. */
    fun atLeast(value: T): FactExpression = LowerBound(this, value)

    /** Inclusive upper bound. */
    fun atMost(value: T): FactExpression = UpperBound(this, value)

    internal fun lower(raw: String): ParseResult<FactExpression> = parse(raw).mapValue(::atLeast)
    internal fun upper(raw: String): ParseResult<FactExpression> = parse(raw).mapValue(::atMost)
}
