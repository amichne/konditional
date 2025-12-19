package io.amichne.konditional.internal.builders

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.dsl.RuleScope
import io.amichne.konditional.core.dsl.VersionRangeScope
import io.amichne.konditional.core.id.HexId
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.registry.AxisRegistry.axisFor
import io.amichne.konditional.internal.builders.versions.VersionRangeBuilder
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.evaluable.AxisConstraint
import io.amichne.konditional.rules.evaluable.Evaluable
import io.amichne.konditional.rules.evaluable.Evaluable.Companion.factory
import io.amichne.konditional.rules.evaluable.Placeholder
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

/**
 * Internal implementation of [RuleScope].
 *
 * This class is the internal implementation of the rule configuration DSL scope.
 * Users interact with the public [RuleScope] interface,
 * not this implementation directly.
 *
 * @param C The type create the contextFn that the rules will evaluate against.
 * @constructor Internal constructor - users cannot instantiate this class directly.
 */
@KonditionalDsl
@PublishedApi
internal data class RuleBuilder<C : Context>(
    private var extension: Evaluable<C> = Placeholder,
    private var note: String? = null,
    private var range: VersionRange = Unbounded(),
    private val platforms: LinkedHashSet<Platform> = linkedSetOf(),
    private val axisConstraints: MutableList<AxisConstraint> = mutableListOf(),
    private val locales: LinkedHashSet<AppLocale> = linkedSetOf(),
    private val rolloutAllowlist: LinkedHashSet<HexId> = linkedSetOf(),
    private var rampUp: RampUp? = null,
) : RuleScope<C> {

    override fun allowlist(vararg stableIds: StableId) {
        rolloutAllowlist += stableIds.map { it.hexId }
    }

    /**
     * Implementation of [RuleScope.locales].
     */
    override fun locales(vararg appLocales: AppLocale) {
        locales += appLocales
    }

    /**
     * Implementation of [RuleScope.platforms].
     */
    @KonditionalDsl
    override fun platforms(vararg ps: Platform) {
        platforms += ps
    }

    /**
     * Implementation of [RuleScope.versions] that delegates to [VersionRangeBuilder].
     */
    override fun versions(build: VersionRangeScope.() -> Unit) {
        range = VersionRangeBuilder().apply(build).build()
    }

    /**
     * Implementation of [RuleScope.extension].
     */
    override fun extension(block: C.() -> Boolean) {
        extension = factory { block(it) }
    }

    /**
     * Implementation of [RuleScope.axis].
     */
    @Deprecated(
        message = "Use axis(axis: Axis<T>, vararg values: T) instead for better type safety.",
        replaceWith = ReplaceWith(
            "axis(*values)"
        ),
        level = DeprecationLevel.WARNING,
    )
    override fun <T> axis(
        axis: Axis<T>,
        vararg values: T,
    ) where T : AxisValue, T : Enum<T> {
        val allowedIds = values.mapTo(linkedSetOf()) { it.id }
        val idx = axisConstraints.indexOfFirst { it.axisId == axis.id }
        if (idx >= 0) {
            val existing = axisConstraints[idx]
            axisConstraints[idx] = existing.copy(
                allowedIds = existing.allowedIds + allowedIds
            )
        } else {
            axisConstraints += AxisConstraint(axis.id, allowedIds)
        }
    }

    override fun <T> axis(vararg values: T) where T : AxisValue, T : Enum<T> {
        @Suppress("DEPRECATION")
        axis(requireNotNull(axisFor(values.first()::class)), *values)
    }

    /**
     * Implementation of [RuleScope.note].
     */
    override fun note(text: String) {
        note = text
    }

    override fun rampUp(function: () -> Number) {
        this.rampUp = RampUp.of(function().toDouble())
    }

    /**
     * Builds a Rule instance. Override this method in custom builders to create
     * custom rule implementations. Internal method - not intended for direct use.
     *
     * @return A Rule instance (Rule by default)
     */
    internal fun build(): Rule<C> =
        Rule(
            rampUp = rampUp ?: RampUp.default,
            rolloutAllowlist = rolloutAllowlist,
            locales = locales,
            platforms = platforms,
            versionRange = range,
            axisConstraints = axisConstraints.toList(),
            note = note,
            extension = extension,
        )
}
