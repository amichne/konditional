package io.amichne.konditional.context.fact

import java.util.Collections
import java.util.Objects

internal fun <T : Any> immutableListCopy(values: Collection<T>): List<T> =
    Collections.unmodifiableList(values.map { Objects.requireNonNull(it) })

/** A typed assignment created by its declaring fact. */
class FactValue<T : Any> internal constructor(internal val fact: Fact<T>, values: Collection<T>) {
    internal val values: List<T> = immutableListCopy(values)
}

/** Immutable assignments, keyed by declaration identity. An undeclared assignment cannot satisfy a rule. */
class Facts private constructor(private val assignments: Map<Fact<*>, FactValue<*>>) {
    /** The values supplied for [fact], or null when the context does not supply it. */
    internal fun <T : Any> valuesOf(fact: Fact<T>): List<T>? =
        assignments[fact]?.values?.map(fact.domain::typed)

    companion object {
        /** Empty selection for contexts that supply no facts. */
        val EMPTY: Facts = Facts(emptyMap())

        /** Each descriptor can be assigned once. Rejects repeated assignments as ambiguous. */
        fun of(vararg assignments: FactValue<*>): Facts {
            val byFact = assignments.associateBy { it.fact }
            require(byFact.size == assignments.size) { "Duplicate fact assignment" }
            return Facts(byFact)
        }
    }
}
