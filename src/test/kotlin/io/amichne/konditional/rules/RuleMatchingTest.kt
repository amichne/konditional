package io.amichne.konditional.rules

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.context.Version
import io.amichne.konditional.rules.versions.VersionRange
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.FullyBound
import io.amichne.konditional.core.StableId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for Rule matching logic and specificity calculation.
 * Validates that rules correctly match contexts based on locale, platform, version, and ramp-up.
 */
class RuleMatchingTest {
    private fun ctx(
        locale: AppLocale = AppLocale.EN_US,
        platform: Platform = Platform.IOS,
        version: String = "1.0.0",
        idHex: String = "00000000000000000000000000000000",
    ) = Context(locale, platform, Version.parse(version), StableId.of(idHex))

    @Test
    fun `Given rule with no constraints, When matching, Then all contexts match`() {
        val rule = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        assertTrue(rule.matches(ctx()))
        assertTrue(rule.matches(ctx(locale = AppLocale.ES_US)))
        assertTrue(rule.matches(ctx(platform = Platform.ANDROID)))
        assertTrue(rule.matches(ctx(version = "99.99.99")))
    }

    @Test
    fun `Given rule with single platform, When matching, Then only that platform matches`() {
        val rule = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded,
        )

        assertTrue(rule.matches(ctx(platform = Platform.IOS)))
        assertFalse(rule.matches(ctx(platform = Platform.ANDROID)))
        assertFalse(rule.matches(ctx(platform = Platform.WEB)))
    }

    @Test
    fun `Given rule with multiple platforms, When matching, Then any of those platforms match`() {
        val rule = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS, Platform.ANDROID),
            versionRange = Unbounded,
        )

        assertTrue(rule.matches(ctx(platform = Platform.IOS)))
        assertTrue(rule.matches(ctx(platform = Platform.ANDROID)))
        assertFalse(rule.matches(ctx(platform = Platform.WEB)))
    }

    @Test
    fun `Given rule with single locale, When matching, Then only that locale matches`() {
        val rule = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        assertTrue(rule.matches(ctx(locale = AppLocale.EN_US)))
        assertFalse(rule.matches(ctx(locale = AppLocale.ES_US)))
        assertFalse(rule.matches(ctx(locale = AppLocale.HI_IN)))
    }

    @Test
    fun `Given rule with multiple locales, When matching, Then any of those locales match`() {
        val rule = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = setOf(AppLocale.EN_US, AppLocale.EN_CA),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        assertTrue(rule.matches(ctx(locale = AppLocale.EN_US)))
        assertTrue(rule.matches(ctx(locale = AppLocale.EN_CA)))
        assertFalse(rule.matches(ctx(locale = AppLocale.ES_US)))
    }

    @Test
    fun `Given rule with version range, When matching, Then only versions in range match`() {
        val rule = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = FullyBound(
                min = Version(1, 0, 0),
                max = Version(2, 0, 0),
            ),
        )

        assertFalse(rule.matches(ctx(version = "0.9.9")))
        assertTrue(rule.matches(ctx(version = "1.0.0")))
        assertTrue(rule.matches(ctx(version = "1.5.0")))
        assertTrue(rule.matches(ctx(version = "2.0.0")))
        assertFalse(rule.matches(ctx(version = "2.0.1")))
    }

    @Test
    fun `Given rule with combined constraints, When matching, Then all constraints must match`() {
        val rule = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = setOf(AppLocale.EN_US, AppLocale.EN_CA),
            platforms = setOf(Platform.IOS),
            versionRange = FullyBound(
                min = Version(2, 0, 0),
                max = Version(3, 0, 0),
            ),
        )

        // All constraints match
        assertTrue(rule.matches(ctx(locale = AppLocale.EN_US, platform = Platform.IOS, version = "2.5.0")))

        // Wrong locale
        assertFalse(rule.matches(ctx(locale = AppLocale.ES_US, platform = Platform.IOS, version = "2.5.0")))

        // Wrong platform
        assertFalse(rule.matches(ctx(locale = AppLocale.EN_US, platform = Platform.ANDROID, version = "2.5.0")))

        // Wrong version
        assertFalse(rule.matches(ctx(locale = AppLocale.EN_US, platform = Platform.IOS, version = "1.5.0")))
    }

    @Test
    fun `Given rules with different specificity, When calculating specificity, Then more specific rules have higher scores`() {
        val ruleEmpty = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val rulePlatform = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded,
        )

        val ruleLocale = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val ruleVersion = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = FullyBound(Version(1, 0, 0), Version(2, 0, 0)),
        )

        val rulePlatformAndLocale = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded,
        )

        val ruleAll = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = setOf(AppLocale.EN_US, AppLocale.EN_CA),
            platforms = setOf(Platform.IOS, Platform.ANDROID),
            versionRange = FullyBound(Version(1, 0, 0), Version(2, 0, 0)),
        )

        // Empty rule has Unbounded version range, which counts as having bounds
        // So specificity is 1, not 0
        assertEquals(1, ruleEmpty.specificity())

        // Single constraint rules with Unbounded have specificity of 2
        // (their constraint + version bounds from Unbounded)
        assertEquals(2, rulePlatform.specificity())
        assertEquals(2, ruleLocale.specificity())
        // ruleVersion has explicit FullyBound, so only version constraint counts
        assertEquals(1, ruleVersion.specificity())

        // Two constraints with Unbounded have specificity of 3
        assertEquals(3, rulePlatformAndLocale.specificity())

        // All three constraints have specificity of 3 (all explicitly set)
        assertEquals(3, ruleAll.specificity())
    }

    @Test
    fun `Given multiple locales or platforms, When calculating specificity, Then specificity counts presence not quantity`() {
        val ruleOnePlatform = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded,
        )

        val ruleTwoPlatforms = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS, Platform.ANDROID),
            versionRange = Unbounded,
        )

        val ruleOneLocale = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val ruleTwoLocales = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = setOf(AppLocale.EN_US, AppLocale.EN_CA),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        // Having multiple values in a dimension doesn't increase specificity - only presence matters
        // All rules here have specificity of 2 (platform/locale constraint + version bounds from Unbounded)
        assertEquals(2, ruleOnePlatform.specificity())
        assertEquals(2, ruleTwoPlatforms.specificity())
        assertEquals(2, ruleOneLocale.specificity())
        assertEquals(2, ruleTwoLocales.specificity())
    }

    @Test
    fun `Given rules with notes, When comparing specificity, Then notes serve as tiebreaker`() {
        val ruleA = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded,
            note = "Rule A",
        )

        val ruleB = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded,
            note = "Rule B",
        )

        // Same specificity
        assertEquals(ruleA.specificity(), ruleB.specificity())

        // Notes can be used as tiebreaker in sorting (alphabetically)
        val sorted = listOf(ruleB, ruleA).sortedWith(
            compareByDescending<Rule<Context>> { it.specificity() }.thenBy { it.note ?: "" }
        )

        assertEquals("Rule A", sorted[0].note)
        assertEquals("Rule B", sorted[1].note)
    }
}
