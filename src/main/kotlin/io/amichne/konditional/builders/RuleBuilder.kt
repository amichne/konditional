package io.amichne.konditional.builders

import io.amichne.konditional.builders.versions.VersionRangeBuilder
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.context.Platform
import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

/**
 * A builder class for constructing rules with a specific context type.
 *
 * @param C The type of the context that the rules will evaluate against.
 */
@FeatureFlagDsl
class RuleBuilder<C : Context> {
    var rampUp: RampUp? = null
    private val locales = linkedSetOf<AppLocale>()
    private val platforms = linkedSetOf<Platform>()
    private var range: VersionRange = Unbounded
    private var note: String? = null

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

    fun note(text: String) {
        note = text
    }

    fun build(): Rule<C> =
        Rule(
            rampUp = rampUp ?: RampUp.default,
            locales = locales,
            platforms = platforms,
            versionRange = range,
            note = note,
        )
}
