package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.EncodableValue

/**
 * DSL scope for flag configuration.
 *
 * This interface defines the public API for configuring individual feature flags.
 * Users cannot instantiate implementations of this interface directly - it is only
 * available as a receiver in DSL blocks through internal implementations.
 *
 * Example usage:
 * ```kotlin
 * MyFeature.FEATURE_A with {
 *     default(true)
 *     salt("v2")
 *     rule(false) {
 *         platforms(Platform.IOS)
 *         rollout { 50.0 }
 *     }
 * }
 * ```
 *
 * @param S The EncodableValue type wrapping the actual value
 * @param T The actual value type
 * @param C The contextFn type the flag evaluates against
 * @since 0.0.2
 */
@KonditionalDsl
interface FlagScope<S : EncodableValue<T>, T : Any, C : Context, M : Namespace> {
    val default: T

    fun active(block: () -> Boolean): Unit

    /**
     * Sets the salt value for the flag.
     *
     * Salt is used in hash-based rollout calculations. Changing the salt
     * will redistribute users across rollout percentages.
     *
     * @param value The salt value (default is "v1")
     */
    fun salt(value: String)

    /**
     * Defines a targeting rule with an associated return value.
     *
     * Rules determine which users receive which values based on context properties.
     * The value is required, ensuring every rule has an associated return value
     * at compile time.
     *
     * Example:
     * ```kotlin
     * rule(true) {
     *     platforms(Platform.IOS)
     *     locales(AppLocale.UNITED_STATES)
     *     rollout { 50.0 }
     * }
     * ```
     *
     * @param value The value to return when this rule matches
     * @param build DSL block for configuring the rule's targeting criteria
     */
    fun rule(value: T, build: RuleScope<C>.() -> Unit = {})
}
