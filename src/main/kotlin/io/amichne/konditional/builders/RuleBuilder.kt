package io.amichne.konditional.builders

import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.versions.VersionRange

@FeatureFlagDsl
class RuleBuilder {
    private var value: Boolean = false
    private var coverage: Double? = null
    private val locales = linkedSetOf<AppLocale>()
    private val platforms = linkedSetOf<Platform>()
    private var range: VersionRange = VersionRange()
    private var note: String? = null

    fun value(
        value: Boolean,
        coveragePct: Double? = null
    ) {
        this@RuleBuilder.value = value
        coverage = coveragePct ?: if (value) 100.0 else 0.0
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

    fun build(): Rule = Rule(
        value = value,
        coveragePct = coverage ?: if (value) 100.0 else 0.0,
        locales = locales,
        platforms = platforms,
        versionRange = range,
        note = note
    )
}
