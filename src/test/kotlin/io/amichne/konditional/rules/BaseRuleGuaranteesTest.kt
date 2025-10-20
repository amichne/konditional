package io.amichne.konditional.rules

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.StableId
import io.amichne.konditional.rules.versions.LeftBound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests that verify Rule's guarantees about applying base matching logic.
 *
 * These tests demonstrate that the template method pattern ensures base attributes
 * (locales, platforms, versions) are always checked, even when custom rules add
 * additional matching criteria.
 *
 * ## Usage Example
 *
 * To create a custom rule with additional matching criteria:
 *
 * ```kotlin
 * class SubscriptionRule<C : CustomContext>(
 *     rampUp: RampUp,
 *     locales: Set<AppLocale> = emptySet(),
 *     platforms: Set<Platform> = emptySet(),
 *     val requiredTier: String? = null,
 * ) : Rule<C>(rampUp, locales, platforms) {
 *
 *     // Override to add custom matching logic
 *     override fun matches(context: C): Boolean {
 *         return requiredTier == null || context.subscriptionTier == requiredTier
 *     }
 *
 *     // Override to add custom internalSpecificity
 *     override fun specificity(): Int {
 *         return if (requiredTier != null) 1 else 0
 *     }
 * }
 * ```
 *
 * The base matching logic (locales, platforms, versions) is GUARANTEED to be
 * applied because the `internalMatches()` method is final. Your custom matching logic
 * in `matches()` is only called after base attributes have matched.
 */
class RuleGuaranteesTest {
    private val defaultContext = Context(
        locale = AppLocale.EN_US,
        platform = Platform.ANDROID,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("11111111111111111111111111111111")
    )

    /**
     * Custom context for testing additional attributes.
     */
    interface CustomContext : Context {
        val subscriptionTier: String
        val userRole: String
    }

    private val customContext = object : CustomContext {
        override val locale = AppLocale.EN_US
        override val platform = Platform.ANDROID
        override val appVersion = Version(1, 0, 0)
        override val stableId = StableId.of("22222222222222222222222222222222")
        override val subscriptionTier = "premium"
        override val userRole = "admin"
    }

    /**
     * Custom rule that adds subscription tier matching.
     */
    class SubscriptionRule<C : CustomContext>(
        rampUp: RampUp,
        locales: Set<AppLocale> = emptySet(),
        platforms: Set<Platform> = emptySet(),
        val requiredTier: String? = null,
    ) : Rule<C>(rampUp, locales = locales, platforms = platforms) {

        override fun matches(context: C): Boolean {
            return requiredTier == null || context.subscriptionTier == requiredTier
        }

        override fun specificity(): Int {
            return if (requiredTier != null) 1 else 0
        }
    }

    @Test
    fun `base rule with no restrictions matches any context`() {
        val rule = Rule<Context>(
            rampUp = RampUp.of(100.0)
        )

        assertTrue(rule.internalMatches(defaultContext))
    }

    @Test
    fun `base rule locale restriction is always enforced`() {
        val rule = Rule<Context>(
            rampUp = RampUp.of(100.0),
            locales = setOf(AppLocale.ES_US)
        )

        // Context has EN_US locale, rule requires ES_US
        assertFalse(rule.internalMatches(defaultContext))
    }

    @Test
    fun `base rule platform restriction is always enforced`() {
        val rule = Rule<Context>(
            rampUp = RampUp.of(100.0),
            platforms = setOf(Platform.IOS)
        )

        // Context has ANDROID platform, rule requires IOS
        assertFalse(rule.internalMatches(defaultContext))
    }

    @Test
    fun `base rule version restriction is always enforced`() {
        val rule = Rule<Context>(
            rampUp = RampUp.of(100.0),
            versionRange = LeftBound(Version(2, 0, 0))
        )

        // Context has version 1.0.0, rule requires 2.0.0+
        assertFalse(rule.internalMatches(defaultContext))
    }

    @Test
    fun `custom rule cannot bypass base locale restriction`() {
        // Even though the custom rule internalMatches the subscription tier,
        // the base locale restriction prevents a match
        val rule = SubscriptionRule<CustomContext>(
            rampUp = RampUp.of(100.0),
            locales = setOf(AppLocale.ES_US),
            requiredTier = "premium"
        )

        // Context has premium tier (custom match) but EN_US locale (base mismatch)
        assertFalse(rule.internalMatches(customContext))
    }

    @Test
    fun `custom rule cannot bypass base platform restriction`() {
        val rule = SubscriptionRule<CustomContext>(
            rampUp = RampUp.of(100.0),
            platforms = setOf(Platform.IOS),
            requiredTier = "premium"
        )

        // Context has premium tier (custom match) but ANDROID platform (base mismatch)
        assertFalse(rule.internalMatches(customContext))
    }

    @Test
    fun `custom rule requires both base and additional criteria to match`() {
        val rule = SubscriptionRule<CustomContext>(
            rampUp = RampUp.of(100.0),
            locales = setOf(AppLocale.EN_US),
            requiredTier = "premium"
        )

        // All criteria match
        assertTrue(rule.internalMatches(customContext))

        // Create context with wrong tier
        val basicContext = object : CustomContext {
            override val locale = AppLocale.EN_US
            override val platform = Platform.ANDROID
            override val appVersion = Version(1, 0, 0)
            override val stableId = StableId.of("33333333333333333333333333333333")
            override val subscriptionTier = "basic"
            override val userRole = "user"
        }

        // Base internalMatches but additional criteria doesn't
        assertFalse(rule.internalMatches(basicContext))
    }

    @Test
    fun `custom rule specificity includes both base and additional specificity`() {
        val ruleWithLocaleAndTier = SubscriptionRule<CustomContext>(
            rampUp = RampUp.of(100.0),
            locales = setOf(AppLocale.EN_US),
            requiredTier = "premium"
        )

        // Base internalSpecificity: 1 (locale)
        // Additional internalSpecificity: 1 (tier)
        // Total: 2
        assertEquals(2, ruleWithLocaleAndTier.internalSpecificity())

        val ruleWithAllAttributes = SubscriptionRule<CustomContext>(
            rampUp = RampUp.of(100.0),
            locales = setOf(AppLocale.EN_US),
            platforms = setOf(Platform.ANDROID),
            requiredTier = "premium"
        )

        // Base internalSpecificity: 2 (locale + platform)
        // Additional internalSpecificity: 1 (tier)
        // Total: 3
        assertEquals(3, ruleWithAllAttributes.internalSpecificity())
    }

    @Test
    fun `custom rule with no restrictions on base attributes still enforces custom criteria`() {
        val rule = SubscriptionRule<CustomContext>(
            rampUp = RampUp.of(100.0),
            requiredTier = "enterprise"
        )

        // Base attributes all pass (no restrictions), but custom tier doesn't match
        assertFalse(rule.internalMatches(customContext)) // has "premium", needs "enterprise"
    }
}
