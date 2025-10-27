package io.amichne.konditional.builders

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.TargetedValue
import io.amichne.konditional.rules.TargetedValue.Companion.targetedBy

/**
 * A builder class for constructing and configuring a feature flag.
 *
 * @param S The type of the state associated with the feature flag.
 * @param C The type of the context that the feature flag evaluates against.
 * @property key The feature flag key used to uniquely identify the flag.
 * @constructor Creates a new instance of the FlagBuilder with the specified feature flag key.
 */
@FeatureFlagDsl
class FlagBuilder<S : Any, C : Context>(
    private val key: Conditional<S, C>,
) {
    private val targetedValues = mutableListOf<TargetedValue<S, C>>()
    private var defaultValue: S? = null
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
     * @param coverage The coverage percentage for the default value.
     */
    fun default(
        value: S,
        coverage: Double? = null,
    ) {
        defaultValue = value
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
     * Defines a targeting rule for this flag using the provided [build] lambda.
     * The [build] lambda is an extension function on [RuleBuilder] that allows
     * you to configure the rule's behavior and properties.
     *
     * @param build A lambda with receiver of type [RuleBuilder<C>] used to build the rule.
     */
    fun rule(build: RuleBuilder<C>.() -> Unit): Rule<C> = RuleBuilder<C>().apply(build).build()

    @FeatureFlagDsl
    infix fun Rule<C>.implies(value: S) {
        targetedValues += targetedBy(value)
    }

    /**
     * Builds and returns a `FlagDefinition` instance of type `S` with context type `C`.
     * Internal method - not intended for direct use.
     *
     * @return A `FlagDefinition` instance constructed based on the current configuration.
     */
    internal fun build(): FlagDefinition<S, C> {
        requireNotNull(defaultValue) { "Default value must be set" }
        return FlagDefinition(
            conditional = key,
            bounds = targetedValues.toList(),
            defaultValue = defaultValue!!,
            salt = salt,
        )
    }
}
