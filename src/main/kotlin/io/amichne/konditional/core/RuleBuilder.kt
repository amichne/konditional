package io.amichne.konditional.core

import io.amichne.konditional.core.context.AppLocale
import io.amichne.konditional.core.context.Platform
import io.amichne.konditional.core.context.Version
import io.amichne.konditional.core.rules.Rule
import io.amichne.konditional.core.rules.versions.VersionRange

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

    fun versions(
        min: String? = null,
        max: String? = null
    ) {
        range = VersionRange(min?.let(Version::parse), max?.let(Version::parse))
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
