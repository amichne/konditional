@file:OptIn(io.amichne.konditional.api.KonditionalInternalApi::class)

package io.amichne.konditional.internal.builders

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.LocaleTag
import io.amichne.konditional.context.PlatformTag
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.dsl.VariantDispatchHost
import io.amichne.konditional.core.dsl.VersionRangeScope
import io.amichne.konditional.core.dsl.rules.targeting.scopes.AnyOfScope
import io.amichne.konditional.core.dsl.rules.targeting.scopes.NarrowingTargetingScope
import io.amichne.konditional.internal.builders.versions.VersionRangeBuilder
import io.amichne.konditional.rules.predicate.PredicateRef
import io.amichne.konditional.rules.targeting.Targeting
import io.amichne.konditional.values.NamespaceId
import io.amichne.konditional.values.PredicateId

/**
 * Internal builder for [Targeting.AnyOf] nodes.
 *
 * Accumulates [Targeting] leaves via the [AnyOfScope] methods; [build] wraps
 * them in a [Targeting.AnyOf] OR-disjunction. An empty block produces an
 * empty [Targeting.AnyOf] which never matches — callers should guard against
 * this before appending to the enclosing [Targeting.All].
 */
@KonditionalDsl
@PublishedApi
@Suppress("OVERRIDE_DEPRECATION")
internal class AnyOfBuilder<C : Context>(
    private val namespaceId: NamespaceId? = null,
    private val predicateResolver: ((PredicateRef) -> Result<Targeting.Custom<C>>)? = null,
    private val predicateRegistrar: ((PredicateRef.Registered, Targeting.Custom<C>) -> Unit)? = null,
    private val inlinePredicateIdFactory: (() -> PredicateId)? = null,
) : AnyOfScope<C>, NarrowingTargetingScope<C>, VariantDispatchHost {

    private val leaves = mutableListOf<Targeting<C>>()
    private val predicateRefs = mutableListOf<PredicateRef>()
    private val variantScope = RuleVariantScope<C>(leaves)
    internal val referencedPredicateRefs: List<PredicateRef>
        get() = predicateRefs.toList()

    override fun locales(vararg appLocales: LocaleTag) {
        if (appLocales.isNotEmpty())
            leaves += Targeting.locale(appLocales.mapTo(linkedSetOf()) { it.id })
    }

    override fun platforms(vararg ps: PlatformTag) {
        if (ps.isNotEmpty())
            leaves += Targeting.platform(ps.mapTo(linkedSetOf()) { it.id })
    }

    override fun versions(build: VersionRangeScope.() -> Unit) {
        val range = VersionRangeBuilder().apply(build).build()
        leaves += Targeting.version(range)
    }

    override fun extension(block: C.() -> Boolean) {
        leaves += Targeting.Custom(block = { c -> c.block() })
    }

    override fun predicate(ref: PredicateRef) {
        val resolver = requireNotNull(predicateResolver) {
            "predicate(ref) is only available when rules are built with a namespace-scoped PredicateRegistry."
        }
        leaves += resolver(ref).getOrThrow()
        predicateRefs += ref
    }

    override fun require(block: C.() -> Boolean) {
        val resolvedNamespaceId = namespaceId
        val resolver = predicateResolver
        val registrar = predicateRegistrar
        val predicateIdFactory = inlinePredicateIdFactory
        if (shouldFallbackInlineRequire(resolvedNamespaceId, resolver, registrar, predicateIdFactory)) {
            extension(block)
            return
        }
        val nonNullNamespaceId = checkNotNull(resolvedNamespaceId)
        val nonNullRegistrar = checkNotNull(registrar)
        val nonNullPredicateIdFactory = checkNotNull(predicateIdFactory)

        val ref = PredicateRef.Registered(
            namespaceId = nonNullNamespaceId,
            id = nonNullPredicateIdFactory(),
        )
        val inlinePredicate = Targeting.Custom<C>(block = { context: C -> context.block() })
        nonNullRegistrar(ref, inlinePredicate)
        leaves += inlinePredicate
        predicateRefs += ref
    }

    private fun shouldFallbackInlineRequire(
        resolvedNamespaceId: NamespaceId?,
        resolver: ((PredicateRef) -> Result<Targeting.Custom<C>>)?,
        registrar: ((PredicateRef.Registered, Targeting.Custom<C>) -> Unit)?,
        predicateIdFactory: (() -> PredicateId)?,
    ): Boolean = resolvedNamespaceId == null || resolver == null || registrar == null || predicateIdFactory == null

    override fun <R : Context> extensionNarrowed(
        evidence: (C) -> R?,
        block: R.() -> Boolean,
    ) {
        leaves += Targeting.Guarded(
            inner = Targeting.Custom(block = { narrowed: R -> narrowed.block() }),
            evidence = evidence,
        )
    }

    override fun <V> onAxisSelection(
        axis: Axis<V>,
        values: Set<V>,
    ) where V : AxisValue<V>, V : Enum<V> {
        variantScope.onAxisSelection(axis, values)
    }

    internal fun build(): Targeting.AnyOf<C> = Targeting.AnyOf(leaves.toList())
}
