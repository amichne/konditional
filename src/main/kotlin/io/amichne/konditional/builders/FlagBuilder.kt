package io.amichne.konditional.builders

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.FeatureFlag
import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.rules.ConditionalValue
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy
import io.amichne.konditional.rules.Rule

/**
 * A builder class for constructing and configuring a feature flag.
 *
 * @param S The EncodableValue type (Boolean, String, Int, or Double wrapper).
 * @param C The type of the context that the feature flag evaluates against.
 * @property conditional The feature flag key used to uniquely identify the flag.
 * @constructor Creates a new instance of the FlagBuilder with the specified feature flag key.
 */
@ConsistentCopyVisibility
@FeatureFlagDsl
data class FlagBuilder<S : EncodableValue<*>, C : Context> internal constructor(
    private val conditional: Conditional<S, C>,
) {
    private val conditionalValues = mutableListOf<ConditionalValue<S, C>>()
    private var defaultValue: S? = null
    private var defaultCoverage: Double? = null
    private var salt: String = "v1"

    companion object {
        fun <S : EncodableValue<*>, C : Context> Conditional<S, C>.flag(
            flagBuilder: FlagBuilder<S, C>.() -> Unit = {},
        ): FeatureFlag<S, C> = FlagBuilder(this).apply(flagBuilder).build()
    }

    /**
     * Sets the default value for the flag being built.
     *
     * This function allows you to specify a default value that the flag will take
     * if no other value is explicitly provided. The default value ensures that the
     * flag has a meaningful state even when not explicitly set.
     *
     * @param value The default EncodableValue to assign to the flag.
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
        conditionalValues += targetedBy(value)
    }

    /**
     * Builds and returns a `FlagDefinition` instance of type `S` with context type `C`.
     * Internal method - not intended for direct use.
     *
     * @return A `FlagDefinition` instance constructed based on the current configuration.
     */
    internal fun build(): FeatureFlag<S, C> {
        requireNotNull(defaultValue) { "Default value must be set" }
        return FeatureFlag(
            conditional = conditional,
            bounds = conditionalValues.toList(),
            defaultValue = defaultValue!!,
            salt = salt,
        )
    }
}
