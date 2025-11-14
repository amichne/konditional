package io.amichne.konditional.internal.builders

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.core.dsl.FeatureFlagDsl
import io.amichne.konditional.core.dsl.RuleScope
import io.amichne.konditional.core.dsl.VersionRangeScope
import io.amichne.konditional.internal.builders.versions.VersionRangeBuilder
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.evaluable.Placeholder
import io.amichne.konditional.rules.evaluable.Evaluable
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

/**
 * Internal implementation of [RuleScope].
 *
 * This class is the internal implementation of the rule configuration DSL scope.
 * Users interact with the public [RuleScope] interface,
 * not this implementation directly.
 *
 * @param C The type of the context that the rules will evaluate against.
 * @constructor Internal constructor - users cannot instantiate this class directly.
 */
@FeatureFlagDsl
@PublishedApi
internal data class RuleBuilder<C : Context>(
    private var extension: Evaluable<C> = Placeholder,
    private var note: String? = null,
    private var range: VersionRange = Unbounded(),
    private val platforms: LinkedHashSet<Platform> = linkedSetOf(),
    private val locales: LinkedHashSet<AppLocale> = linkedSetOf(),
    private var rollout: Rollout? = null,
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
    @FeatureFlagDsl
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
    override fun extension(function: () -> Evaluable<C>) {
        extension = function()
    }

    /**
     * Implementation of [RuleScope.note].
     */
    override fun note(text: String) {
        note = text
    }

    override fun rollout(function: () -> Number) {
        this.rollout = Rollout.of(function().toDouble())
    }

    /**
     * Builds a Rule instance. Override this method in custom builders to create
     * custom rule implementations. Internal method - not intended for direct use.
     *
     * @return A Rule instance (Rule by default)
     */
    internal fun build(): Rule<C> =
        Rule(
            rollout = rollout ?: Rollout.default,
            locales = locales,
            platforms = platforms,
            versionRange = range,
            note = note,
            extension = extension,
        )
}
