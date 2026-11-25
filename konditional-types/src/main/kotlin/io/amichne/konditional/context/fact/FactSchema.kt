package io.amichne.konditional.context.fact

import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult

/** Immutable namespace vocabulary. Duplicate identifiers are a declaration error. */
class FactSchema(vararg facts: Fact<*>) {
    private val declarations = facts.associateBy { it.key.id }

    init {
        require(declarations.size == facts.size) { "Duplicate fact identifier" }
    }

    /** True only when this schema owns every descriptor in the expression. */
    operator fun contains(expression: FactExpression): Boolean = expression.declaredIn(this)

    internal fun owns(fact: Fact<*>): Boolean = declarations[fact.key.id] === fact

    /** Resolves external identifiers and operands before evaluation; returns the first typed error in term order. */
    fun resolve(spec: FactSpec): ParseResult<FactExpression> = when (spec) {
        is FactSpec.OneOf -> resolveFact(spec.fact) { parseMembership(it, spec.operands) }
        is FactSpec.AtLeast -> resolveOrdered(spec.fact) { it.lower(spec.operand) }
        is FactSpec.AtMost -> resolveOrdered(spec.fact) { it.upper(spec.operand) }
        is FactSpec.All -> spec.terms.traverse(::resolve).mapValue(::Conjunction)
        is FactSpec.AnyOf -> spec.terms.traverse(::resolve).mapValue(::Disjunction)
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
    return ParseResult.Success(java.util.List.copyOf(parsed))
}
