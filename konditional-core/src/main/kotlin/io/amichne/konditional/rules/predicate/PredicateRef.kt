package io.amichne.konditional.rules.predicate

import io.amichne.konditional.values.NamespaceId
import io.amichne.konditional.values.PredicateId

/**
 * A typed, stable reference to a named predicate.
 *
 * [PredicateRef] is a descriptor — it names a predicate without carrying the evaluation
 * logic itself. Resolution from a ref to a live predicate implementation happens via
 * [PredicateRegistry].
 *
 * ## Variants
 *
 * - [BuiltIn] — a predicate provided by the konditional-core evaluation engine.
 * - [Registered] — a predicate registered by the consumer within a specific namespace.
 *
 * ## Ordering
 *
 * [PredicateRef] implements [Comparable]. The natural order is:
 * 1. [BuiltIn] before [Registered]
 * 2. Within each variant, lexicographic on [id] (and [namespaceId] for [Registered])
 *
 * This ordering is stable and deterministic across JVM restarts.
 */
sealed interface PredicateRef : Comparable<PredicateRef> {
    /** Stable identifier for the predicate. Must be non-blank. */
    val id: PredicateId

    /**
     * A built-in predicate provided by the konditional-core evaluation engine.
     *
     * @property id Stable identifier for the predicate.
     */
    data class BuiltIn(override val id: PredicateId) : PredicateRef {

        override fun compareTo(other: PredicateRef): Int = when (other) {
            is BuiltIn -> id.value.compareTo(other.id.value)
            is Registered -> -1 // BuiltIn sorts before Registered
        }
    }

    /**
     * A predicate registered by the consumer within a specific namespace.
     *
     * The combination of [namespaceId] and [id] must be unique within the owning
     * [PredicateRegistry].
     *
     * @property namespaceId The namespace this predicate is scoped to.
     * @property id Stable identifier for the predicate within its namespace.
     */
    data class Registered(
        val namespaceId: NamespaceId,
        override val id: PredicateId,
    ) : PredicateRef {
        companion object {
            @Deprecated(
                message = "Use the constructor with typed NamespaceId and PredicateId for type safety.",
                replaceWith = ReplaceWith("Registered(NamespaceId(namespaceId), PredicateId(id))")
            )
            @JvmName("invoke")
            operator fun invoke(
                namespaceId: String,
                id: String
            ) = Registered(NamespaceId(namespaceId), PredicateId(id))
        }

        override fun compareTo(other: PredicateRef): Int = when (other) {
            is PredicateRef -> 1 // Registered sorts after BuiltIn
            is Registered -> compareValuesBy(this, other, { it.namespaceId.value }, { it.id.value })
        }
    }
}
