package io.amichne.konditional.rules

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.fixtures.core.id.TestStableId
import io.amichne.konditional.rules.targeting.Targeting
import io.amichne.konditional.rules.versions.LeftBound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests that verify Rule's guarantees about applying base matching logic
 * through the [Targeting] hierarchy.
 *
 * These tests demonstrate that the Targeting.All conjunction ensures base attributes
 * (locales, platforms, versions) are always checked, even when custom targeting
 * leaves add additional matching criteria.
 */
class RuleGuaranteesTest {
    private val defaultContext = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.ANDROID,
        appVersion = Version(1, 0, 0),
        stableId = TestStableId,
    )

    interface CustomContext :
        Context,
        Context.LocaleContext,
        Context.PlatformContext,
        Context.VersionContext,
        Context.StableIdContext {
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

    /** Custom targeting leaf that checks subscription tier. */
    private fun subscriptionTargeting(requiredTier: String?): Targeting.Custom<CustomContext> =
        Targeting.Custom(
            block = { context -> requiredTier == null || context.subscriptionTier == requiredTier },
        )

    @Test
    fun `base rule with no restrictions matches any context`() {
        val rule = Rule<Context>(
            rampUp = RampUp.of(100.0),
        )

        assertTrue(rule.matches(defaultContext))
    }

    @Test
    fun `base rule locale restriction is always enforced`() {
        val rule = Rule<Context>(
            rampUp = RampUp.of(100.0),
            targeting = Targeting.All(
                listOf(Targeting.locale(setOf(AppLocale.CANADA.id))),
            ),
        )

        assertFalse(rule.matches(defaultContext))
    }

    @Test
    fun `base rule platform restriction is always enforced`() {
        val rule = Rule<Context>(
            rampUp = RampUp.of(100.0),
            targeting = Targeting.All(
                listOf(Targeting.platform(setOf(Platform.IOS.id))),
            ),
        )

        assertFalse(rule.matches(defaultContext))
    }

    @Test
    fun `base rule version restriction is always enforced`() {
        val rule = Rule<Context>(
            rampUp = RampUp.of(100.0),
            targeting = Targeting.All(
                listOf(Targeting.version(LeftBound(Version(2, 0, 0)))),
            ),
        )

        assertFalse(rule.matches(defaultContext))
    }

    @Test
    fun `custom targeting leaf cannot bypass base locale restriction`() {
        val rule = Rule<CustomContext>(
            rampUp = RampUp.of(100.0),
            targeting = Targeting.All(
                listOf(
                    Targeting.locale(setOf(AppLocale.MEXICO.id)),
                    subscriptionTargeting("premium"),
                ),
            ),
        )

        // Context has premium tier (custom match) but UNITED_STATES locale (base mismatch)
        assertFalse(rule.matches(customContext))
    }

    @Test
    fun `custom targeting leaf cannot bypass base platform restriction`() {
        val rule = Rule<CustomContext>(
            targeting = Targeting.All(
                listOf(
                    Targeting.platform(setOf(Platform.IOS.id)),
                    subscriptionTargeting("premium"),
                ),
            ),
        )

        // Context has premium tier (custom match) but ANDROID platform (base mismatch)
        assertFalse(rule.matches(customContext))
    }

    @Test
    fun `custom targeting leaf requires both base and additional criteria to match`() {
        val premiumRule = subscriptionTargeting("premium")

        // All criteria match
        assertTrue(premiumRule.matches(customContext))

        // Create context with wrong tier
        val basicContext = object : CustomContext {
            override val locale = AppLocale.UNITED_STATES
            override val platform = Platform.ANDROID
            override val appVersion = Version(1, 0, 0)
            override val stableId = StableId.of("33333333333333333333333333333333")
            override val subscriptionTier = "basic"
            override val userRole = "user"
        }

        // Custom criteria doesn't match
        assertFalse(premiumRule.matches(basicContext))
    }

    @Test
    fun `custom targeting leaf specificity includes both base and additional specificity`() {
        val ruleWithLocaleAndTier = Rule<CustomContext>(
            targeting = Targeting.All(
                listOf(
                    Targeting.locale(setOf(AppLocale.UNITED_STATES.id)),
                    subscriptionTargeting("premium"),
                ),
            ),
        )

        // Base specificity: 1 (locale)
        // Additional specificity: 1 (tier)
        // Total: 2
        assertEquals(2, ruleWithLocaleAndTier.specificity())

        val ruleWithAllAttributes = Rule<CustomContext>(
            targeting = Targeting.All(
                listOf(
                    Targeting.locale(setOf(AppLocale.UNITED_STATES.id)),
                    Targeting.platform(setOf(Platform.ANDROID.id)),
                    subscriptionTargeting("premium"),
                ),
            ),
        )

        // Base specificity: 2 (locale + platform)
        // Additional specificity: 1 (tier)
        // Total: 3
        assertEquals(3, ruleWithAllAttributes.specificity())
    }

    @Test
    fun `custom targeting leaf with no restrictions on base attributes still enforces custom criteria`() {
        val rule = subscriptionTargeting("enterprise")

        // Base attributes all pass (no restrictions), but custom tier doesn't match
        assertFalse(rule.matches(customContext)) // has "premium", needs "enterprise"
    }
}
