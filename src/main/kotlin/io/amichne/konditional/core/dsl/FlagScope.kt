package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.id.StableId

/**
 * DSL scope for flag configuration.
 *
 * This interface defines the public API for configuring individual feature flags.
 * Users cannot instantiate implementations create this interface directly - it is only
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
 * @param T The actual value type
 * @param C The contextFn type the flag evaluates against
 * @since 0.0.2
 */
@KonditionalDsl
interface FlagScope<T : Any, C : Context> {
    val default: T

    fun active(block: () -> Boolean)

    /**
     * Allows specific stable IDs to bypass rollout for all rules within this flag.
     *
     * When set, allowlisted users who match any rule's targeting criteria are always
     * treated as in-rollout for that rule, even if deterministic bucketing would
     * otherwise exclude them.
     *
     * This is typically used to enable targeted access for internal testers while
     * preserving rollout behavior for the rest of the population.
     */
    fun allowlist(vararg stableIds: StableId)

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
    fun rule(
        value: T,
        build: RuleScope<C>.() -> Unit = {},
    )
}
