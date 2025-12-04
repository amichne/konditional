package io.amichne.konditional.rules

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rampup
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.fixtures.core.id.TestStableId
import io.amichne.konditional.rules.evaluable.Evaluable
import io.amichne.konditional.rules.evaluable.Evaluable.Companion.factory
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
 *     rollout: Rampup,
 *     locales: Set<AppLocale> = emptySet(),
 *     platforms: Set<Platform> = emptySet(),
 *     val requiredTier: String? = null,
 * ) : Rule<C>(rollout, locales, platforms) {
 *
 *     // Override to add custom matching logic
 *     override fun matches(contextFn: C): Boolean {
 *         return requiredTier == null || contextFn.subscriptionTier == requiredTier
 *     }
 *
 *     // Override to add custom specificity
 *     override fun specificity(): Int {
 *         return if (requiredTier != null) 1 else 0
 *     }
 * }
 * ```
 *
 * The base matching logic (locales, platforms, versions) is GUARANTEED to be
 * applied because the `matches()` method is final. Your custom matching logic
 * in `matches()` is only called after base attributes have matched.
 */
class RuleGuaranteesTest {
    private val defaultContext = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.ANDROID,
        appVersion = Version(1, 0, 0),
        stableId = TestStableId
    )

    /**
     * Custom contextFn for testing additional attributes.
     */
    interface CustomContext : Context {
        val subscriptionTier: String
        val userRole: String
    }

    private val customContext = object : CustomContext {
        override val locale = AppLocale.UNITED_STATES
        override val platform = Platform.ANDROID
        override val appVersion = Version(1, 0, 0)
        override val stableId = TestStableId
        override val subscriptionTier = "premium"
        override val userRole = "admin"
    }

    /**
     * Custom rule that adds subscription tier matching.
     */
    data class SubscriptionRule<C : CustomContext>(
        val requiredTier: String? = null,
    ) : Evaluable<C> by factory({ context -> requiredTier == null || context.subscriptionTier == requiredTier })

    @Test
    fun `base rule with no restrictions matches any context`() {
        val rule = Rule<Context>(
            rollout = Rampup.of(100.0)
        )

        assertTrue(rule.matches(defaultContext))
    }

    @Test
    fun `base rule locale restriction is always enforced`() {
        val rule = Rule<Context>(
            rollout = Rampup.of(100.0),
            locales = setOf(AppLocale.CANADA)
        )

        // Context has UNITED_STATES locale, rule requires UNITED_STATES
        assertFalse(rule.matches(defaultContext))
    }

    @Test
    fun `base rule platform restriction is always enforced`() {
        val rule = Rule<Context>(
            rollout = Rampup.of(100.0),
            platforms = setOf(Platform.IOS)
        )

        // Context has ANDROID platform, rule requires IOS
        assertFalse(rule.matches(defaultContext))
    }

    @Test
    fun `base rule version restriction is always enforced`() {
        val rule = Rule<Context>(
            rollout = Rampup.of(100.0),
            versionRange = LeftBound(Version(2, 0, 0))
        )

        // Context has version 1.0.0, rule requires 2.0.0+
        assertFalse(rule.matches(defaultContext))
    }

    @Test
    fun `custom rule cannot bypass base locale restriction`() {
        // Even though the custom rule matches the subscription tier,
        // the base locale restriction prevents a match
        val rule = Rule(
            rollout = Rampup.of(100.0),
            locales = setOf(AppLocale.MEXICO),
            extension = SubscriptionRule(
                requiredTier = "premium"
            )
        )

        // Context has premium tier (custom match) but UNITED_STATES locale (base mismatch)
        assertFalse(rule.matches(customContext))
    }

    @Test
    fun `custom rule cannot bypass base platform restriction`() {
        val rule = Rule(
            platforms = setOf(Platform.IOS),
            extension = SubscriptionRule(
                requiredTier = "premium"
            )
        )

        // Context has premium tier (custom match) but ANDROID platform (base mismatch)
        assertFalse(rule.matches(customContext))
    }

    @Test
    fun `custom rule requires both base and additional criteria to match`() {
        val rule = SubscriptionRule<CustomContext>(
            requiredTier = "premium"
        )

        // All criteria match
        assertTrue(rule.matches(customContext))

        // Create contextFn with wrong tier
        val basicContext = object : CustomContext {
            override val locale = AppLocale.UNITED_STATES
            override val platform = Platform.ANDROID
            override val appVersion = Version(1, 0, 0)
            override val stableId = StableId.of("33333333333333333333333333333333")
            override val subscriptionTier = "basic"
            override val userRole = "user"
        }

        // Base matches but additional criteria doesn't
        assertFalse(rule.matches(basicContext))
    }

    @Test
    fun `custom rule specificity includes both base and additional specificity`() {
        val ruleWithLocaleAndTier = Rule(
            locales = setOf(AppLocale.UNITED_STATES), extension = SubscriptionRule(
            requiredTier = "premium"
        )
        )

        // Base specificity: 1 (locale)
        // Additional specificity: 1 (tier)
        // Total: 2
        assertEquals(2, ruleWithLocaleAndTier.specificity())

        val ruleWithAllAttributes = ruleWithLocaleAndTier.copy(
            baseEvaluable = ruleWithLocaleAndTier.baseEvaluable.copy(
                locales = setOf(AppLocale.UNITED_STATES),
                platforms = setOf(Platform.ANDROID),
            )
        )

        // Base specificity: 2 (locale + platform)
        // Additional specificity: 1 (tier)
        // Total: 3
        assertEquals(3, ruleWithAllAttributes.specificity())
    }

    @Test
    fun `custom rule with no restrictions on base attributes still enforces custom criteria`() {
        val rule = SubscriptionRule<CustomContext>(
            requiredTier = "enterprise"
        )

        // Base attributes all pass (no restrictions), but custom tier doesn't match
        assertFalse(rule.matches(customContext)) // has "premium", needs "enterprise"
    }
}
