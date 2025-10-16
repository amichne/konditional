package io.amichne.konditional.builders

import io.amichne.konditional.builders.versions.VersionRangeBuilder
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.versions.VersionRange

@FeatureFlagDsl
/**
 * A builder class for constructing rules with a specific state type.
 *
 * @param S The type of the state that the rules will operate on. It must be a non-nullable type.
 */
class RuleBuilder {
    var rampUp: Double? = null
    private val locales = linkedSetOf<AppLocale>()
    private val platforms = linkedSetOf<Platform>()
    private var range: VersionRange = VersionRange()
    private var note: String? = null

    @Deprecated("This method is deprecated and will be removed in future versions.")
    fun rampUp(function: () -> Double) = function().also {
        require(it in 0.0..100.0) { "Ramp Up out of range: $it" }
        this@RuleBuilder.rampUp = it
    }

    fun locales(vararg appLocales: AppLocale) {
        locales += appLocales
    }

    fun platforms(vararg ps: Platform) {
        platforms += ps
    }

    fun version(build: VersionRangeBuilder.() -> Unit) {
        range = VersionRangeBuilder().apply(build).build()
    }

    fun note(text: String) {
        note = text
    }

    fun build(): Rule {
        return Rule(
            coveragePct = rampUp ?: 100.0,
            locales = locales,
            platforms = platforms,
            versionRange = range,
            note = note
        )
    }
}
