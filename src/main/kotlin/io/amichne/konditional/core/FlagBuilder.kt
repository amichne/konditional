package io.amichne.konditional.core

import io.amichne.konditional.core.rules.Rule

@FeatureFlagDsl
class FlagBuilder(private val key: FeatureFlag) {
    private val rules = mutableListOf<Rule>()
    private var defaultValue: Boolean = false
    private var defaultCoverage: Double? = null
    private var salt: String = "v1"

    fun default(
        value: Boolean,
        coverage: Double? = null
    ) {
        defaultValue = value
        defaultCoverage = coverage
    }

    fun salt(value: String) {
        salt = value
    }

    fun rule(build: RuleBuilder.() -> Unit) {
        rules += RuleBuilder().apply(build).build()
    }

    fun build(): Flag = Flag(
        key = key,
        rules = rules.toList(),
        defaultValue = defaultValue,
        defaultEligibleSegment = defaultCoverage ?: if (defaultValue) 100.0 else 0.0,
        salt = salt
    )
}
