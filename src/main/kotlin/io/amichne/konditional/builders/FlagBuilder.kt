package io.amichne.konditional.builders

import io.amichne.konditional.core.FeatureFlag
import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.core.Flag
import io.amichne.konditional.core.Flaggable
import io.amichne.konditional.rules.Rule

@FeatureFlagDsl
class FlagBuilder<T : Flaggable<T>>(private val key: FeatureFlag<T>) {
    private val rules = mutableListOf<Rule<T>>()
    private var defaultValue: T? = null
    private var fallbackValue: T? = null
    private var defaultCoverage: Double? = null
    private var salt: String = "v1"

    fun default(
        value: T,
        fallback: T? = null,
        coverage: Double? = null
    ) {
        defaultValue = value
        fallbackValue = fallback
        defaultCoverage = coverage
    }

    fun salt(value: String) {
        salt = value
    }

    fun rule(build: RuleBuilder<T>.() -> Unit) {
        rules += RuleBuilder<T>().apply(build).build()
    }

    fun build(): Flag<T> {
        requireNotNull(defaultValue) { "Default value must be set" }
        return Flag(
            key = key,
            rules = rules.toList(),
            defaultValue = defaultValue!!,
            fallbackValue = fallbackValue ?: defaultValue!!,
            defaultEligibleSegment = defaultCoverage ?: 100.0,
            salt = salt
        )
    }
}
