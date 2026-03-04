package io.amichne.konditional.rules.predicate

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.rules.targeting.Targeting

/**
 * Default [PredicateRegistry] backed by an insertion-ordered, immutable-on-read map.
 *
 * ## Thread safety
 *
 * Registration is not thread-safe. Registrations must complete before the registry
 * is shared across threads. Resolution ([resolve]) is safe to call concurrently.
 *
 * ## Ordering
 *
 * [registeredRefs] returns refs in insertion order — the order [register] was called.
 * This order is deterministic given the same registration sequence.
 *
 * @param C The context type this registry evaluates against.
 * @param namespaceId The namespace this registry is scoped to.
 */
class InMemoryPredicateRegistry<C : Context>(
    override val namespaceId: String,
) : PredicateRegistry<C> {

    init {
        require(namespaceId.isNotBlank()) { "PredicateRegistry.namespaceId must not be blank" }
    }

    private val store: LinkedHashMap<String, Pair<PredicateRef.Registered, Targeting.Custom<C>>> = LinkedHashMap()

    override fun resolve(ref: PredicateRef): Result<Targeting.Custom<C>> {
        val entry = when (ref) {
            is PredicateRef.Registered -> store[ref.id]?.second
            is PredicateRef.BuiltIn -> null // BuiltIn refs are resolved by the evaluation engine, not the registry
        }
        return if (entry != null) {
            Result.success(entry)
        } else {
            Result.failure(
                io.amichne.konditional.core.result.KonditionalBoundaryFailure(
                    ParseError.UnknownPredicate(ref)
                )
            )
        }
    }

    override val registeredRefs: List<PredicateRef.Registered>
        get() = store.values.map { it.first }

    override fun register(ref: PredicateRef.Registered, predicate: Targeting.Custom<C>) {
        require(ref.namespaceId == namespaceId) {
            "PredicateRef namespace '${ref.namespaceId}' does not match registry namespace '$namespaceId'"
        }
        check(!store.containsKey(ref.id)) {
            "Predicate '${ref.id}' is already registered in namespace '$namespaceId'"
        }
        store[ref.id] = ref to predicate
    }
}
