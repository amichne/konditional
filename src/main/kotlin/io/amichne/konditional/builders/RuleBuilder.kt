package io.amichne.konditional.builders

import io.amichne.konditional.builders.versions.VersionRangeBuilder
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.evaluable.Evaluable
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

/**
 * A builder class for constructing rules with a specific context type.
 *
 * @param C The type of the context that the rules will evaluate against.
 */
@ConsistentCopyVisibility
@FeatureFlagDsl
data class RuleBuilder<C : Context> internal constructor(
    private var extension: Evaluable<C> = object : Evaluable<C>() {},
    private var note: String? = null,
    private var range: VersionRange = Unbounded,
    private val platforms: LinkedHashSet<Platform> = linkedSetOf(),
    var rollout: Rollout? = null,
    private val locales: LinkedHashSet<AppLocale> = linkedSetOf()
) {

    fun locales(vararg appLocales: AppLocale) {
        locales += appLocales
    }

    @FeatureFlagDsl
    fun platforms(vararg ps: Platform) {
        platforms += ps
    }

    fun versions(build: VersionRangeBuilder.() -> Unit) {
        range = VersionRangeBuilder().apply(build).build()
    }

    fun extension(function: () -> Evaluable<C>) {
        extension = function()
    }

    fun note(text: String) {
        note = text
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
