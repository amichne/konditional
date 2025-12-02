package io.amichne.konditional.internal.builders

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.dsl.RuleScope
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.rules.ConditionalValue
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy
import io.amichne.konditional.rules.Rule

/**
 * Internal implementation of [FlagScope].
 *
 * This class is the internal implementation of the flag configuration DSL scope.
 * Users interact with the public [FlagScope] interface,
 * not this implementation directly.
 *
 * @param S The EncodableValue type wrapping the actual value.
 * @param T The actual value type.
 * @param C The type of the context that the feature flag evaluates against.
 * @property feature The feature flag key used to uniquely identify the flag.
 * @constructor Internal constructor - users cannot instantiate this class directly.
 */
@KonditionalDsl
@PublishedApi
internal data class FlagBuilder<S : EncodableValue<T>, T : Any, C : Context, M : Namespace>(
    private val feature: Feature<S, T, C, M>,
) : FlagScope<S, T, C, M> {
    private val conditionalValues = mutableListOf<ConditionalValue<S, T, C, M>>()
    private var defaultValue: T? = null
    private var salt: String = "v1"
    private var isActive: Boolean = true

    /**
     * Sets a rule's [isActive] to the passed boolean
     *
     * @param block Evaluable boolean
     * @see FlagScope.active
     */
    override fun active(block: () -> Boolean) {
        isActive = block()
    }

    /**
     * Implementation of [FlagScope.default].
     */
    override fun default(value: T) {
        defaultValue = value
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
     * Implementation of [FlagScope.returns].
     */
    @KonditionalDsl
    override infix fun Rule<C>.returns(value: T) {
        conditionalValues += targetedBy(value)
    }

    /**
     * Builds and returns a `FlagDefinition` instance of type `S` with context type `C`.
     * Internal method - not intended for direct use.
     *
     * @return A `FlagDefinition` instance constructed based on the current configuration.
     */
    @PublishedApi
    internal fun build(): FlagDefinition<S, T, C, M> {
        requireNotNull(defaultValue) { "Default value must be set" }
        return FlagDefinition(
            feature = feature,
            bounds = conditionalValues.toList(),
            defaultValue = defaultValue!!,
            salt = salt,
            isActive = isActive
        )
    }
}
