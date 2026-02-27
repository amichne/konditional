package io.amichne.konditional.internal.builders

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.LocaleTag
import io.amichne.konditional.context.PlatformTag
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.dsl.VersionRangeScope
import io.amichne.konditional.core.dsl.rules.targeting.scopes.AnyOfScope
import io.amichne.konditional.core.dsl.rules.targeting.scopes.NarrowingTargetingScope
import io.amichne.konditional.core.registry.AxisCatalog
import io.amichne.konditional.internal.builders.versions.VersionRangeBuilder
import io.amichne.konditional.rules.targeting.Targeting

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
internal class AnyOfBuilder<C : Context>(
    private val axisCatalog: AxisCatalog? = null,
) : AnyOfScope<C>, NarrowingTargetingScope<C> {

    private val leaves = mutableListOf<Targeting<C>>()

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

    override fun <R : Context> extensionNarrowed(
        evidence: (C) -> R?,
        block: R.() -> Boolean,
    ) {
        leaves += Targeting.Guarded(
            inner = Targeting.Custom(block = { narrowed: R -> narrowed.block() }),
            evidence = evidence,
        )
    }

    override fun <T> axis(
        axis: Axis<T>,
        vararg values: T,
    ) where T : AxisValue<T>, T : Enum<T> {
        require(values.isNotEmpty()) { "axis(...) requires at least one value." }
        val allowedIds = values.mapTo(linkedSetOf()) { it.id }
        val idx = leaves.indexOfFirst { it is Targeting.Axis && it.axisId == axis.id }
        if (idx >= 0) {
            val existing = leaves[idx] as Targeting.Axis
            leaves[idx] = existing.copy(allowedIds = existing.allowedIds + allowedIds)
        } else {
            leaves += Targeting.Axis(axis.id, allowedIds)
        }
    }

    override fun <T> axis(vararg values: T) where T : AxisValue<T>, T : Enum<T> {
        require(values.isNotEmpty()) { "axis(...) requires at least one value to infer the axis type." }
        val catalog = axisCatalog
            ?: throw IllegalArgumentException(
                "Type-inferred axis(...) requires an AxisCatalog. " +
                    "Use axis(axisHandle, values...) or declare axes with Namespace.axis(...).",
            )
        axis(catalog.axisForOrThrow(values.first()::class), *values)
    }

    internal fun build(): Targeting.AnyOf<C> = Targeting.AnyOf(leaves.toList())
}
