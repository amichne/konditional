package io.amichne.konditional.rules.predicate

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.rules.targeting.Targeting
import io.amichne.konditional.values.NamespaceId

/**
 * Namespace-scoped registry for named predicate implementations.
 *
 * [PredicateRegistry] bridges the serializable [PredicateRef] descriptor to a live
 * [Targeting.Custom] implementation. All operations are scoped to a single namespace —
 * cross-namespace lookups are not provided by this interface.
 *
 * ## Registration
 *
 * Register predicates by calling [register] with a [PredicateRef.Registered] and a
 * [Targeting.Custom] implementation. Each `(namespaceId, id)` pair must be unique.
 *
 * ## Resolution order
 *
 * [registeredRefs] returns refs in insertion order (the order [register] was called).
 * Iteration order is stable across repeated calls for the same set of registrations.
 *
 * @param C The context type this registry evaluates against.
 */
interface PredicateRegistry<C : Context> {
    /** The namespace this registry is scoped to. */
    val namespaceId: NamespaceId

    /**
     * Resolves a [PredicateRef] to a live [Targeting.Custom] for evaluation.
     *
     * Returns [Result.failure] with [ParseError.UnknownPredicate] if the ref is not
     * registered in this registry.
     */
    fun resolve(ref: PredicateRef): Result<Targeting.Custom<C>>

    /**
     * All refs registered in this registry, in stable insertion order.
     */
    val registeredRefs: List<PredicateRef.Registered>

    /**
     * Registers a named predicate implementation for the given [ref].
     *
     * @throws IllegalArgumentException if [ref.namespaceId] does not match [namespaceId].
     * @throws IllegalStateException if a predicate with the same [ref.id] is already registered.
     */
    fun register(ref: PredicateRef.Registered, predicate: Targeting.Custom<C>)
}
