package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.LocaleTag
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.PlatformTag
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
interface RuleScopeBase<C : Context> {
    /**
     * Explicitly marks this rule as matching all contexts.
     *
     * This is a no-op: an empty rule already matches all contexts. Use this to make "catch-all"
     * intent explicit in reviews and when reading configuration.
     */
    fun always() {}

    /**
     * Alias for [always].
     */
    fun matchAll() = always()
}

/**
 * Targeting mix-in for locale-based rules.
 */
@KonditionalDsl
interface LocaleTargetingScope<C : Context> {
    /**
     * Specifies which locales this rule applies to.
     *
     * The rule will only match contexts with one of the specified locales.
     *
     * @param appLocales The locales to target (use [io.amichne.konditional.context.AppLocale] or your own [LocaleTag])
     */
    fun locales(vararg appLocales: LocaleTag)
}

/**
 * Targeting mix-in for platform-based rules.
 */
@KonditionalDsl
interface PlatformTargetingScope<C : Context> {
    /**
     * Sugar for targeting iOS.
     *
     * Equivalent to `platforms(Platform.IOS)`.
     */
    fun ios() = platforms(Platform.IOS)

    /**
     * Sugar for targeting Android.
     *
     * Equivalent to `platforms(Platform.ANDROID)`.
     */
    fun android() = platforms(Platform.ANDROID)

    /**
     * Specifies which platforms this rule applies to.
     *
     * The rule will only match contexts with one of the specified platforms.
     *
     * @param ps The platforms to target (use [io.amichne.konditional.context.Platform] or your own [PlatformTag])
     */
    fun platforms(vararg ps: PlatformTag)
}

/**
 * Targeting mix-in for version-based rules.
 */
@KonditionalDsl
interface VersionTargetingScope<C : Context> {
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
}

/**
 * Targeting mix-in for stable-id based rollouts.
 */
@KonditionalDsl
interface StableIdTargetingScope<C : Context> {
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

/**
 * Targeting mix-in for axis constraints.
 */
@KonditionalDsl
interface AxisTargetingScope<C : Context> {
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
     * @param this@axis The axis descriptor
     * @param values The values to allow for this axis
     */
//    @Deprecated(
//        message = "Use axis(axis: Axis<T>, vararg values: T) instead for better type safety.",
//        replaceWith = ReplaceWith(
//            "axis(*values)"
//        ),
//        level = DeprecationLevel.WARNING,
//    )
//    fun <T> axis(
//        axis: Axis<T>,
//        vararg values: T,
//    ) where T : AxisValue<T>, T : Enum<T>

    fun <T> axis(
        vararg values: T,
    ) where T : AxisValue<T>, T : Enum<T>
}

/**
 * Targeting mix-in for custom predicates.
 */
@KonditionalDsl
interface ExtensionTargetingScope<C : Context> {
    /**
     * Adds a custom targeting extension using an Predicate.
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
}

/**
 * Targeting mix-in for human-readable notes.
 */
@KonditionalDsl
interface NoteScope<C : Context> {
    /**
     * Adds a human-readable note to document the rule's purpose.
     *
     * Notes are useful for explaining complex targeting logic or
     * tracking the rationale behind specific rules.
     *
     * @param text The note text
     */
    fun note(text: String)
}

/**
 * Base, user-agnostic rule scope that can be composed for configuration-centric targeting.
 */
@KonditionalDsl
interface ContextRuleScope<C : Context> :
    RuleScopeBase<C>,
    AxisTargetingScope<C>,
    ExtensionTargetingScope<C>,
    NoteScope<C>

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
interface RuleScope<C : Context> :
    RuleScopeBase<C>,
    LocaleTargetingScope<C>,
    PlatformTargetingScope<C>,
    VersionTargetingScope<C>,
    StableIdTargetingScope<C>,
    AxisTargetingScope<C>,
    ExtensionTargetingScope<C>,
    NoteScope<C>
