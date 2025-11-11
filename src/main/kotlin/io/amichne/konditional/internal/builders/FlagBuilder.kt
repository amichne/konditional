package io.amichne.konditional.internal.builders

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Feature
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.FlagScope
import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.core.RuleScope
import io.amichne.konditional.rules.ConditionalValue
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy
import io.amichne.konditional.rules.Rule

/**
 * Internal implementation of [io.amichne.konditional.core.FlagScope].
 *
 * This class is the internal implementation of the flag configuration DSL scope.
 * Users interact with the public [io.amichne.konditional.core.FlagScope] interface,
 * not this implementation directly.
 *
 * @param S The type of the state associated with the feature flag.
 * @param C The type of the context that the feature flag evaluates against.
 * @property conditional The feature flag key used to uniquely identify the flag.
 * @constructor Internal constructor - users cannot instantiate this class directly.
 */
@ConsistentCopyVisibility
@FeatureFlagDsl
@PublishedApi
internal data class FlagBuilder<S : Any, C : Context>(
    private val conditional: Feature<S, C>,
) : FlagScope<S, C> {
    private val conditionalValues = mutableListOf<ConditionalValue<S, C>>()
    private var defaultValue: S? = null
    private var defaultCoverage: Double? = null
    private var salt: String = "v1"

    /**
     * Implementation of [FlagScope.default].
     */
    override fun default(value: S, coverage: Double?) {
        defaultValue = value
        defaultCoverage = coverage
    }

    /**
     * Implementation of [FlagScope.salt].
     */
    override fun salt(value: String) {
        salt = value
    }

    /**
     * Implementation of [FlagScope.rule] that delegates to [RuleBuilder].
     */
    override fun rule(build: RuleScope<C>.() -> Unit): Rule<C> = RuleBuilder<C>().apply(build).build()

    /**
     * Implementation of [FlagScope.implies].
     */
    @FeatureFlagDsl
    override infix fun Rule<C>.implies(value: S) {
        conditionalValues += targetedBy(value)
    }

    /**
     * Builds and returns a `FlagDefinition` instance of type `S` with context type `C`.
     * Internal method - not intended for direct use.
     *
     * @return A `FlagDefinition` instance constructed based on the current configuration.
     */
    @PublishedApi
    internal fun build(): FlagDefinition<S, C> {
        requireNotNull(defaultValue) { "Default value must be set" }
        return FlagDefinition(
            conditional = conditional,
            bounds = conditionalValues.toList(),
            defaultValue = defaultValue!!,
            salt = salt,
        )
    }
}
