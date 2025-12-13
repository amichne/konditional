package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Dimension
import io.amichne.konditional.context.DimensionKey
import io.amichne.konditional.context.Platform

/**
 * DSL scope for rule configuration.
 *
 * This interface defines the public API for configuring targeting rules.
 * Users cannot instantiate implementations of this interface directly - it is only
 * available as a receiver in DSL blocks through internal implementations.
 *
 * Example usage:
 * ```kotlin
 * rule {
 *     locales(AppLocale.UNITED_STATES, AppLocale.CANADA)
 *     platforms(Platform.IOS, Platform.ANDROID)
 *     versions {
 *         min(1, 2, 0)
 *         max(2, 0, 0)
 *     }
 *     rollout {  Rampup.of(50.0) }
 *     note("Rampup to mobile users only")
 * }
 * ```
 *
 * @param C The contextFn type the rule evaluates against
 * @since 0.0.2
 */
@KonditionalDsl
interface RuleScope<C : Context> {
    /**
     * Specifies which locales this rule applies to.
     *
     * The rule will only match contexts with one of the specified locales.
     *
     * @param appLocales The locales to target
     */
    fun locales(vararg appLocales: AppLocale)

    /**
     * Specifies which platforms this rule applies to.
     *
     * The rule will only match contexts with one of the specified platforms.
     *
     * @param ps The platforms to target
     */
    fun platforms(vararg ps: Platform)

    /**
     * Specifies the version range this rule applies to.
     *
     * Example:
     * ```kotlin
     * versions {
     *     min(1, 2, 0)  // Minimum version 1.2.0
     *     max(2, 0, 0)  // Maximum version 2.0.0
     * }
     * ```
     *
     * @param build DSL block for configuring the version range
     */
    fun versions(build: VersionRangeScope.() -> Unit)

    /**
     * Specifies custom [Dimension] targeting for this rule.
     *
     * Example:
     * ```kotlin
     * dimension(Environment) {
     *    Environment.PRODUCTION,
     *    Environment.STAGING
     * }
     * ```
     *
     * Adds targeting criteria based on custom dimensions defined in the context,
     * allowing for more granular control over rule applicability based on consumer-defined axes.
     *
     * @param T
     * @param axis
     * @param values
     */
    fun <T> dimension(
        axis: Dimension<T>,
        vararg values: T,
    ) where T : DimensionKey, T : Enum<T>

    /**
     * Adds a custom targeting extension using an Evaluable.
     *
     * Extensions allow for domain-specific targeting beyond the standard
     * platform, locale, and version criteria.
     *
     * Example:
     * ```kotlin
     * extension {
     *     organizationId == "enterprise"
     * }
     * ```
     *
     * @param block The extension logic as a lambda
     */
    fun extension(block: C.() -> Boolean)

    /**
     * Adds a human-readable note to document the rule's purpose.
     *
     * Notes are useful for explaining complex targeting logic or
     * tracking the rationale behind specific rules.
     *
     * @param text The note text
     */
    fun note(text: String)

    /**
     * Sets the rollout percentage for this rule.
     *
     * When set, only the specified percentage of users matching this rule
     * will receive the associated value. The rollout is stable and deterministic
     * based on the user's stable ID.
     *
     * Example:
     * ```kotlin
     * rollout {  50.0  // 50% of matching users }
     * ```
     */
    fun rollout(function: () -> Number)
}
