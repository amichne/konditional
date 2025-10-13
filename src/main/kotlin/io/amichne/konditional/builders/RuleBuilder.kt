package io.amichne.konditional.builders

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.core.Flaggable
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.versions.VersionRange

@FeatureFlagDsl
class RuleBuilder<T : Flaggable<T>> {
    private var value: T? = null
    private var coverage: Double? = null
    private val locales = linkedSetOf<AppLocale>()
    private val platforms = linkedSetOf<Platform>()
    private var range: VersionRange = VersionRange()
    private var note: String? = null

    fun value(
        value: T,
        coveragePct: Double? = null
    ) {
        this@RuleBuilder.value = value
        coverage = coveragePct
    }

    fun locales(vararg appLocales: AppLocale) {
        locales += appLocales
    }

    fun platforms(vararg ps: Platform) {
        platforms += ps
    }

    fun versions(build: VersionBuilder.() -> Unit) {
        range = VersionBuilder().apply(build).build()
    }

    fun note(text: String) {
        note = text
    }

    fun build(): Rule<T> {
        requireNotNull(value) { "Rule value must be set" }
        return Rule(
            value = value!!,
            coveragePct = coverage ?: 100.0,
            locales = locales,
            platforms = platforms,
            versionRange = range,
            note = note
        )
    }
}
