package io.amichne.konditional.builders

import io.amichne.konditional.core.Condition
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.Surjection

/**
 * A builder class for constructing and configuring a feature flag.
 *
 * @param S The type of the state associated with the feature flag.
 * @property key The feature flag key used to uniquely identify the flag.
 * @constructor Creates a new instance of the FlagBuilder with the specified feature flag key.
 */
@FeatureFlagDsl
class FlagBuilder<S : Any>(
    private val key: Conditional<S>,
) {
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
     * Defines a boundary boundary the current context using the provided [build] lambda.
     * The [build] lambda is an extension function on [RuleBuilder] that allows
     * you to configure the boundary's behavior and properties.
     *
     * @param build A lambda with receiver of type [RuleBuilder<S>] used to build the boundary.
     */
    fun boundary(build: RuleBuilder.() -> Unit): Rule = RuleBuilder().apply(build).build()

    @FeatureFlagDsl
    infix fun Rule.implies(value: S) {
        surjections += Surjection(this, value)
    }

    /**
     * Builds and returns a `Condition` instance of type `S`.
     *
     * @return A `Condition` instance constructed based on the current configuration.
     */
    fun build(): Condition<S> {
        requireNotNull(defaultValue) { "Default value must be set" }
        return Condition(
            key = key,
            bounds = surjections.toList(),
            defaultValue = defaultValue!!,
            fallbackValue = fallbackValue ?: defaultValue!!,
            salt = salt,
        )
    }
}
