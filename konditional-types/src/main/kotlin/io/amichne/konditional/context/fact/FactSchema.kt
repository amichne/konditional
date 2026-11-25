package io.amichne.konditional.context.fact

import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult

/**
 * A namespace's typed vocabulary. Subclass it and declare each fact as a property:
 *
 * ```kotlin
 * object WarehouseFacts : FactSchema() {
 *     val zone = single("warehouse.zone", FactDomain.enumeration(Zone.entries) { it.name })
 * }
 * ```
 *
 * The schema is both the receiver that conditions read their facts from and the authority that
 * decides which descriptors a rule can use. A duplicate identifier is a declaration error.
 */
abstract class FactSchema {
    private val declarations = linkedMapOf<String, Fact<*>>()

    /** Declares a fact that a context supplies with exactly one value. */
    protected fun <T : Any> single(id: String, domain: FactDomain<T>): SingleFact<T> =
        declare(SingleFact(this, id, domain))

    /** Declares a fact that a context supplies with a set of values. */
    protected fun <T : Any> multi(id: String, domain: FactDomain<T>): MultiFact<T> =
        declare(MultiFact(this, id, domain))

    /** Declares a single-valued fact that supports bound comparisons under [order]. */
    protected fun <T : Any> ordered(id: String, domain: FactDomain<T>, order: Comparator<T>): OrderedFact<T> =
        declare(OrderedFact(this, id, domain, order))

    private fun <F : Fact<*>> declare(fact: F): F {
        require(declarations.putIfAbsent(fact.key.id, fact) == null) {
            "Duplicate fact identifier '${fact.key.id}'"
        }
        return fact
    }

    /** True only when this schema declared every descriptor in the expression. */
    operator fun contains(expression: FactExpression): Boolean = foreignFacts(expression).isEmpty()

    /** The descriptors in [expression] that another schema declared, in term order. */
    fun foreignFacts(expression: FactExpression): List<Fact<*>> = expression.foreignTo(this)

    internal fun owns(fact: Fact<*>): Boolean = fact.schema === this

    /** Resolves external identifiers and operands before evaluation; returns the first typed error in term order. */
    fun resolve(spec: FactSpec): ParseResult<FactExpression> = when (spec) {
        is FactSpec.OneOf -> resolveFact(spec.fact) { parseMembership(it, spec.operands) }
        is FactSpec.AtLeast -> resolveOrdered(spec.fact) { it.lower(spec.operand) }
        is FactSpec.AtMost -> resolveOrdered(spec.fact) { it.upper(spec.operand) }
        is FactSpec.All -> resolveGroup("all", spec.terms, ::Conjunction)
        is FactSpec.AnyOf -> resolveGroup("anyOf", spec.terms, ::Disjunction)
        is FactSpec.Not -> resolve(spec.term).mapValue(::Negation)
        is FactSpec.Named ->
            if (spec.label.isBlank()) {
                ParseResult.Failure(ParseError.BlankFactLabel)
            } else {
                resolve(spec.term).mapValue { Labeled(spec.label, it) }
            }
    }

    private fun resolveGroup(
        operator: String,
        terms: List<FactSpec>,
        group: (List<FactExpression>) -> FactExpression,
    ): ParseResult<FactExpression> =
        if (terms.isEmpty()) {
            ParseResult.Failure(ParseError.EmptyFactGroup(operator))
        } else {
            terms.traverse(::resolve).mapValue(group)
        }

    private fun resolveFact(
        id: String,
        parse: (Fact<*>) -> ParseResult<FactExpression>,
    ): ParseResult<FactExpression> = declarations[id]?.let(parse)
        ?: ParseResult.Failure(ParseError.UnknownFact(id))

    private fun resolveOrdered(
        id: String,
        parse: (OrderedFact<*>) -> ParseResult<FactExpression>,
    ): ParseResult<FactExpression> = resolveFact(id) { fact ->
        when (fact) {
            is OrderedFact<*> -> parse(fact)
            is SingleFact<*>, is MultiFact<*> -> ParseResult.Failure(ParseError.UnorderedFact(fact.key))
        }
    }

    private fun <T : Any> parseMembership(fact: Fact<T>, raw: List<String>): ParseResult<FactExpression> =
        if (raw.isEmpty()) {
            ParseResult.Failure(ParseError.EmptyFactMembership(fact.key))
        } else {
            raw.traverse(fact::parse).mapValue(fact::membership)
        }
}

/** The vocabulary of a namespace whose rules read no facts. */
object NoFacts : FactSchema()

internal fun <T, R> ParseResult<T>.mapValue(transform: (T) -> R): ParseResult<R> = when (this) {
    is ParseResult.Success -> ParseResult.Success(transform(value))
    is ParseResult.Failure -> this
}

private fun <T, R : Any> List<T>.traverse(parse: (T) -> ParseResult<R>): ParseResult<List<R>> {
    val parsed = ArrayList<R>(size)
    for (entry in this) {
        when (val result = parse(entry)) {
            is ParseResult.Failure -> return result
            is ParseResult.Success -> parsed.add(result.value)
        }
    }
    return ParseResult.Success(immutableListCopy(parsed))
}
