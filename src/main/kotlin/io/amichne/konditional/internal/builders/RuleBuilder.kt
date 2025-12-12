package io.amichne.konditional.internal.builders

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Dimension
import io.amichne.konditional.context.DimensionKey
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rampup
import io.amichne.konditional.core.dsl.DimensionScope
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.dsl.RuleScope
import io.amichne.konditional.core.dsl.VersionRangeScope
import io.amichne.konditional.internal.builders.versions.VersionRangeBuilder
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.evaluable.DimensionConstraint
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
 * @param C The type of the contextFn that the rules will evaluate against.
 * @constructor Internal constructor - users cannot instantiate this class directly.
 */
@KonditionalDsl
@PublishedApi
internal data class RuleBuilder<C : Context>(
    private var extension: Evaluable<C> = Placeholder,
    private var note: String? = null,
    private var range: VersionRange = Unbounded(),
    private val platforms: LinkedHashSet<Platform> = linkedSetOf(),
    private val dimensionConstraints: MutableList<DimensionConstraint> = mutableListOf(),
    private val locales: LinkedHashSet<AppLocale> = linkedSetOf(),
    private var rollout: Rampup? = null,
) : RuleScope<C> {

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

    fun dimensions(block: DimensionScope.() -> Unit) {
        DimensionBuilder().apply(block).build()
    }

    override fun <T : DimensionKey> dimension(
        axis: Dimension<T>,
        vararg values: T,
    ) {
        val allowedIds = values.mapTo(linkedSetOf()) { it.id }
        val idx = dimensionConstraints.indexOfFirst { it.axisId == axis.id }
        if (idx >= 0) {
            val existing = dimensionConstraints[idx]
            dimensionConstraints[idx] = existing.copy(
                allowedIds = existing.allowedIds + allowedIds
            )
        } else {
            dimensionConstraints += DimensionConstraint(axis.id, allowedIds)
        }
    }

    /**
     * Implementation of [RuleScope.note].
     */
    override fun note(text: String) {
        note = text
    }

    override fun rollout(function: () -> Number) {
        this.rollout = Rampup.of(function().toDouble())
    }

    /**
     * Builds a Rule instance. Override this method in custom builders to create
     * custom rule implementations. Internal method - not intended for direct use.
     *
     * @return A Rule instance (Rule by default)
     */
    internal fun build(): Rule<C> =
        Rule(
            rollout = rollout ?: Rampup.default,
            locales = locales,
            platforms = platforms,
            versionRange = range,
            dimensionConstraints = dimensionConstraints.toList(),
            note = note,
            extension = extension,
        )
}
