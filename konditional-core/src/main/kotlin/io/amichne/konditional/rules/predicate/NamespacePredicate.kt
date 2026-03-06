package io.amichne.konditional.rules.predicate

import io.amichne.konditional.context.Context
import io.amichne.konditional.rules.targeting.Targeting
import io.amichne.konditional.values.NamespaceId
import io.amichne.konditional.values.PredicateId

/**
 * Public, non-constructible handle to a namespace-declared predicate.
 *
 * Instances are created only through `Namespace.predicate { ... }` property delegation.
 * This keeps [PredicateRef] construction inside konditional internals while still giving
 * DSL consumers a stable, typed handle they can reference in rules.
 */
class NamespacePredicate<C : Context> @PublishedApi internal constructor(
    @PublishedApi internal val ref: PredicateRef.Registered,
    @PublishedApi internal val targeting: Targeting.Custom<C>,
    @Suppress("unused")
    @PublishedApi internal val witness: (C) -> Unit = {},
) : PredicateLike<C> {
    val namespaceId: NamespaceId
        get() = ref.namespaceId

    val id: PredicateId
        get() = ref.id

    override fun toPredicateExpression(): PredicateExpression<C> =
        PredicateExpression(PredicateExpression.Node.Named(this))
}
