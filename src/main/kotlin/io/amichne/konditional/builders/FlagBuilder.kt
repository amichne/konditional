package io.amichne.konditional.builders

import io.amichne.konditional.core.FeatureFlag
import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.core.Flag
import io.amichne.konditional.rules.Surjection
import io.amichne.konditional.rules.Rule

@FeatureFlagDsl
/**
 * A builder class for constructing and configuring a feature flag.
 *
 * @param S The type of the state associated with the feature flag.
 * @property key The feature flag key used to uniquely identify the flag.
 * @constructor Creates a new instance of the FlagBuilder with the specified feature flag key.
 */
class FlagBuilder<S : Any>(private val key: FeatureFlag<S>) {
    private val rules = mutableListOf<Rule>()
    private val surjections = mutableListOf<Surjection<S>>()
    private var defaultValue: S? = null
    private var fallbackValue: S? = null
    private var defaultCoverage: Double? = null
    private var salt: String = "v1"

    /**
     * Sets the default value for the flag being built.
     *
     * This function allows you to specify a default value that the flag will take
     * if no other value is explicitly provided. The default value ensures that the
     * flag has a meaningful state even when not explicitly set.
     *
     * @param value The default value to assign to the flag.
     * @return The builder instance for chaining further configurations.
     */
    fun default(
        value: S,
        fallback: S? = null,
        coverage: Double? = null,
    ) {
        defaultValue = value
        fallbackValue = fallback
        defaultCoverage = coverage
    }

    /**
     * Sets the salt value for the configuration.
     *
     * @param value The salt value to be used, represented as a string.
     */
    fun salt(value: String) {
        salt = value
    }

    /**
     * Defines a rule within the current context using the provided [build] lambda.
     * The [build] lambda is an extension function on [RuleBuilder] that allows
     * you to configure the rule's behavior and properties.
     *
     * @param build A lambda with receiver of type [RuleBuilder<S>] used to build the rule.
     */
    fun rule(build: RuleBuilder.() -> Unit): Rule = run {
        RuleBuilder().apply(build).build().also { rules += it }
    }

    @FeatureFlagDsl
    infix fun Rule.gives(value: S) {
        surjections += Surjection(this, value)
    }

    /**
     * Builds and returns a `Flag` instance of type `S`.
     *
     * @return A `Flag` instance constructed based on the current configuration.
     */
    fun build(): Flag<S> {
        requireNotNull(defaultValue) { "Default value must be set" }
        return Flag(
            key = key,
            rules = surjections.toList(),
            defaultValue = defaultValue!!,
            fallbackValue = fallbackValue ?: defaultValue!!,
            defaultEligibleSegment = defaultCoverage ?: 100.0,
            salt = salt
        )
    }
}
