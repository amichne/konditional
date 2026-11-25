package io.amichne.konditional.context.fact

/** Unresolved external expression. Only [FactSchema.resolve] can establish its meaning in a namespace. */
sealed interface FactSpec {
    /** Tests for overlap with a nonempty list of external operand identifiers. */
    data class OneOf(val fact: String, val operands: List<String>) : FactSpec
    /** Inclusive lower bound on an ordered fact. */
    data class AtLeast(val fact: String, val operand: String) : FactSpec
    /** Inclusive upper bound on an ordered fact. */
    data class AtMost(val fact: String, val operand: String) : FactSpec
    /** Ordered conjunction. */
    data class All(val terms: List<FactSpec>) : FactSpec
    /** Ordered disjunction. */
    data class AnyOf(val terms: List<FactSpec>) : FactSpec
}
