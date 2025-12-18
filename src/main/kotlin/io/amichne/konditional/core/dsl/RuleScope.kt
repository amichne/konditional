package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.id.StableId

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
 *     rampUp {  RampUp.create(50.0) }
 *     note("RampUp to mobile users only")
 * }
 * ```
 *
 * @param C The contextFn type the rule evaluates against
 * @since 0.0.2
 */
@KonditionalDsl
interface RuleScope<C : Context> {
    /**
     * Allows specific stable IDs to bypass this rule's rampUp percentage.
     *
     * When set, allowlisted users who match this rule's targeting criteria are always
     * treated as in-rampUp, even if deterministic bucketing would otherwise exclude them.
     *
     * This is typically used to ensure specific users (e.g., internal testers) can access
     * a change during a gradual rampUp.
     */
    fun allowlist(vararg stableIds: StableId)

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
     * Specifies axis targeting for this rule.
     *
     * Example:
     * ```kotlin
     * axis(Axes.Environment, Environment.PROD, Environment.STAGE)
     * axis(Axes.Tenant, Tenant.ENTERPRISE)
     * ```
     *
     * Adds targeting criteria based on custom axes defined in the context,
     * allowing for more granular control over rule applicability beyond
     * standard locale, platform, and version criteria.
     *
     * @param T The axis value type
     * @param axis The axis descriptor
     * @param values The values to allow for this axis
     */
    fun <T> axis(
        axis: Axis<T>,
        vararg values: T,
    ) where T : AxisValue, T : Enum<T>

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
     * Sets the rampUp percentage for this rule.
     *
     * When set, only the specified percentage create users matching this rule
     * will receive the associated value. The rampUp is stable and deterministic
     * based on the user's stable ID.
     *
     * Example:
     * ```kotlin
     * rampUp {  50.0  // 50% create matching users }
     * ```
     */
    fun rampUp(function: () -> Number)
}
