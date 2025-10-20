package io.amichne.konditional.rules

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.StableId
import io.amichne.konditional.rules.versions.FullyBound
import io.amichne.konditional.rules.versions.Unbounded
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for Rule matching logic and internalSpecificity calculation.
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

        assertTrue(rule.internalMatches(ctx()))
        assertTrue(rule.internalMatches(ctx(locale = AppLocale.ES_US)))
        assertTrue(rule.internalMatches(ctx(platform = Platform.ANDROID)))
        assertTrue(rule.internalMatches(ctx(version = "99.99.99")))
    }

    @Test
    fun `Given rule with single platform, When matching, Then only that platform matches`() {
        val rule = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded,
        )

        assertTrue(rule.internalMatches(ctx(platform = Platform.IOS)))
        assertFalse(rule.internalMatches(ctx(platform = Platform.ANDROID)))
        assertFalse(rule.internalMatches(ctx(platform = Platform.WEB)))
    }

    @Test
    fun `Given rule with multiple platforms, When matching, Then any of those platforms match`() {
        val rule = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS, Platform.ANDROID),
            versionRange = Unbounded,
        )

        assertTrue(rule.internalMatches(ctx(platform = Platform.IOS)))
        assertTrue(rule.internalMatches(ctx(platform = Platform.ANDROID)))
        assertFalse(rule.internalMatches(ctx(platform = Platform.WEB)))
    }

    @Test
    fun `Given rule with single locale, When matching, Then only that locale matches`() {
        val rule = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        assertTrue(rule.internalMatches(ctx(locale = AppLocale.EN_US)))
        assertFalse(rule.internalMatches(ctx(locale = AppLocale.ES_US)))
        assertFalse(rule.internalMatches(ctx(locale = AppLocale.HI_IN)))
    }

    @Test
    fun `Given rule with multiple locales, When matching, Then any of those locales match`() {
        val rule = Rule<Context>(
            rampUp = RampUp.MAX,
            locales = setOf(AppLocale.EN_US, AppLocale.EN_CA),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        assertTrue(rule.internalMatches(ctx(locale = AppLocale.EN_US)))
        assertTrue(rule.internalMatches(ctx(locale = AppLocale.EN_CA)))
        assertFalse(rule.internalMatches(ctx(locale = AppLocale.ES_US)))
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

        assertFalse(rule.internalMatches(ctx(version = "0.9.9")))
        assertTrue(rule.internalMatches(ctx(version = "1.0.0")))
        assertTrue(rule.internalMatches(ctx(version = "1.5.0")))
        assertTrue(rule.internalMatches(ctx(version = "2.0.0")))
        assertFalse(rule.internalMatches(ctx(version = "2.0.1")))
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
        assertTrue(rule.internalMatches(ctx(locale = AppLocale.EN_US, platform = Platform.IOS, version = "2.5.0")))

        // Wrong locale
        assertFalse(rule.internalMatches(ctx(locale = AppLocale.ES_US, platform = Platform.IOS, version = "2.5.0")))

        // Wrong platform
        assertFalse(rule.internalMatches(ctx(locale = AppLocale.EN_US, platform = Platform.ANDROID, version = "2.5.0")))

        // Wrong version
        assertFalse(rule.internalMatches(ctx(locale = AppLocale.EN_US, platform = Platform.IOS, version = "1.5.0")))
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

        // Empty rule has Unbounded version range, which doesn't count towards internalSpecificity
        assertEquals(0, ruleEmpty.internalSpecificity())

        // Single constraint rules have internalSpecificity of 1 (just their constraint)
        // Unbounded doesn't count towards internalSpecificity
        assertEquals(1, rulePlatform.internalSpecificity())
        assertEquals(1, ruleLocale.internalSpecificity())
        // ruleVersion has explicit FullyBound, so only version constraint counts
        assertEquals(1, ruleVersion.internalSpecificity())

        // Two constraints with Unbounded have internalSpecificity of 2
        assertEquals(2, rulePlatformAndLocale.internalSpecificity())

        // All three constraints have internalSpecificity of 3 (all explicitly set)
        assertEquals(3, ruleAll.internalSpecificity())
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

        // Having multiple values in a dimension doesn't increase internalSpecificity - only presence matters
        // All rules here have internalSpecificity of 1 (just platform/locale constraint)
        // Unbounded doesn't count towards internalSpecificity
        assertEquals(1, ruleOnePlatform.internalSpecificity())
        assertEquals(1, ruleTwoPlatforms.internalSpecificity())
        assertEquals(1, ruleOneLocale.internalSpecificity())
        assertEquals(1, ruleTwoLocales.internalSpecificity())
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

        // Same internalSpecificity
        assertEquals(ruleA.internalSpecificity(), ruleB.internalSpecificity())

        // Notes can be used as tiebreaker in sorting (alphabetically)
        val sorted = listOf(ruleB, ruleA).sortedWith(
            compareByDescending<Rule<Context>> { it.internalSpecificity() }.thenBy { it.note ?: "" }
        )

        assertEquals("Rule A", sorted[0].note)
        assertEquals("Rule B", sorted[1].note)
    }
}
