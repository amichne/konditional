package io.amichne.konditional.internal.builders

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.LocaleTag
import io.amichne.konditional.context.PlatformTag
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.dsl.VersionRangeScope
import io.amichne.konditional.core.dsl.rules.RuleScope
import io.amichne.konditional.core.id.HexId
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.registry.AxisCatalog
import io.amichne.konditional.internal.builders.versions.VersionRangeBuilder
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.targeting.Targeting

/**
 * Internal implementation of [RuleScope].
 *
 * Accumulates [Targeting] leaves into a flat list; the final [build] call wraps
 * them in a [Targeting.All] conjunction. Multiple calls to targeting methods
 * compose with AND semantics — each call appends a leaf.
 *
 * @param C The context type that the rules will evaluate against.
 */
@KonditionalDsl
@PublishedApi
internal class RuleBuilder<C : Context>(
    private val axisCatalog: AxisCatalog? = null,
) : RuleScope<C> {

    private val leaves = mutableListOf<Targeting<C>>()
    private var note: String? = null
    private var rampUp: RampUp? = null
    private val allowlist = linkedSetOf<HexId>()

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

    /**
     * Adds a custom extension predicate.
     *
     * Multiple calls accumulate with AND semantics via the [leaves] list —
     * no ConjunctivePredicate wrapper needed.
     */
    override fun extension(block: C.() -> Boolean) {
        leaves += Targeting.Custom(block = { c -> c.block() })
    }

    override fun <T> axis(
        axis: Axis<T>,
        vararg values: T,
    ) where T : AxisValue<T>, T : Enum<T> {
        require(values.isNotEmpty()) { "axis(...) requires at least one value." }
        val allowedIds = values.mapTo(linkedSetOf()) { it.id }
        // Merge with existing constraint for the same axis (OR within axis, AND across axes)
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

    override fun allowlist(vararg stableIds: StableId) {
        allowlist += stableIds.map { it.hexId }
    }

    override fun note(text: String) {
        note = text
    }

    override fun rampUp(function: () -> Number) {
        this.rampUp = RampUp.of(function().toDouble())
    }

    internal fun build(): Rule<C> = Rule(
        rampUp = rampUp ?: RampUp.default,
        rampUpAllowlist = allowlist,
        note = note,
        targeting = Targeting.All(leaves.toList()),
    )
}
