package io.amichne.konditional.rules

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rampup
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.fixtures.core.id.TestStableId
import io.amichne.konditional.rules.evaluable.Evaluable.matches
import io.amichne.konditional.rules.versions.FullyBound
import io.amichne.konditional.rules.versions.Unbounded
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
        locale: AppLocale = AppLocale.UNITED_STATES,
        platform: Platform = Platform.IOS,
        version: String = "1.0.0",
        idHex: String = TestStableId.id,
    ) = Context(locale, platform, Version.parseUnsafe(version), StableId.of(idHex))

    @Test
    fun `Given rule with no constraints, When matching, Then all contexts match`() {
        val rule = Rule<Context>(
            rollout = Rampup.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        assertTrue(ctx().matches())
        assertTrue(ctx(locale = AppLocale.UNITED_STATES).matches())
        assertTrue(ctx(platform = Platform.ANDROID).matches())
        assertTrue(ctx(version = "99.99.99").matches())
    }

    @Test
    fun `Given rule with single platform, When matching, Then only that platform matches`() {
        val rule = Rule<Context>(
            rollout = Rampup.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded(),
        )

        assertTrue(ctx(platform = Platform.IOS).matches())
        assertFalse(ctx(platform = Platform.ANDROID).matches())
        assertFalse(ctx(platform = Platform.WEB).matches())
    }

    @Test
    fun `Given rule with multiple platforms, When matching, Then any of those platforms match`() {
        val rule = Rule<Context>(
            rollout = Rampup.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS, Platform.ANDROID),
            versionRange = Unbounded(),
        )

        assertTrue(ctx(platform = Platform.IOS).matches())
        assertTrue(ctx(platform = Platform.ANDROID).matches())
        assertFalse(ctx(platform = Platform.WEB).matches())
    }

    @Test
    fun `Given rule with single locale, When matching, Then only that locale matches`() {
        val rule = Rule<Context>(
            rollout = Rampup.MAX,
            locales = setOf(AppLocale.UNITED_STATES),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        assertTrue(ctx(locale = AppLocale.UNITED_STATES).matches())
        assertFalse(ctx(locale = AppLocale.MEXICO).matches())
        assertFalse(ctx(locale = AppLocale.INDIA).matches())
    }

    @Test
    fun `Given rule with multiple locales, When matching, Then any of those locales match`() {
        val rule = Rule<Context>(
            rollout = Rampup.MAX,
            locales = setOf(AppLocale.UNITED_STATES, AppLocale.CANADA),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        assertTrue(ctx(locale = AppLocale.UNITED_STATES).matches())
        assertTrue(ctx(locale = AppLocale.CANADA).matches())
        assertFalse(ctx(locale = AppLocale.MEXICO).matches())
    }

    @Test
    fun `Given rule with version range, When matching, Then only versions in range match`() {
        val rule = Rule<Context>(
            rollout = Rampup.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = FullyBound(
                min = Version(1, 0, 0),
                max = Version(2, 0, 0),
            ),
        )

        assertFalse(ctx(version = "0.9.9").matches())
        assertTrue(ctx(version = "1.0.0").matches())
        assertTrue(ctx(version = "1.5.0").matches())
        assertTrue(ctx(version = "2.0.0").matches())
        assertFalse(ctx(version = "2.0.1").matches())
    }

    @Test
    fun `Given rule with combined constraints, When matching, Then all constraints must match`() {
        val rule = Rule<Context>(
            rollout = Rampup.MAX,
            locales = setOf(AppLocale.UNITED_STATES, AppLocale.CANADA),
            platforms = setOf(Platform.IOS),
            versionRange = FullyBound(
                min = Version(2, 0, 0),
                max = Version(3, 0, 0),
            ),
        )

        // All constraints match
        assertTrue(ctx(locale = AppLocale.UNITED_STATES, platform = Platform.IOS, version = "2.5.0").matches())

        // Wrong locale
        assertFalse(ctx(locale = AppLocale.MEXICO, platform = Platform.IOS, version = "2.5.0").matches())

        // Wrong platform
        assertFalse(ctx(locale = AppLocale.UNITED_STATES, platform = Platform.ANDROID, version = "2.5.0").matches())

        // Wrong version
        assertFalse(ctx(locale = AppLocale.UNITED_STATES, platform = Platform.IOS, version = "1.5.0").matches())
    }

    @Test
    fun `Given rules with different specificity, When calculating specificity, Then more specific rules have higher scores`() {
        val ruleEmpty = Rule<Context>(
            rollout = Rampup.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val rulePlatform = Rule<Context>(
            rollout = Rampup.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded(),
        )

        val ruleLocale = Rule<Context>(
            rollout = Rampup.MAX,
            locales = setOf(AppLocale.UNITED_STATES),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val ruleVersion = Rule<Context>(
            rollout = Rampup.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = FullyBound(Version(1, 0, 0), Version(2, 0, 0)),
        )

        val rulePlatformAndLocale = Rule<Context>(
            rollout = Rampup.MAX,
            locales = setOf(AppLocale.UNITED_STATES),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded(),
        )

        val ruleAll = Rule<Context>(
            rollout = Rampup.MAX,
            locales = setOf(AppLocale.UNITED_STATES, AppLocale.CANADA),
            platforms = setOf(Platform.IOS, Platform.ANDROID),
            versionRange = FullyBound(Version(1, 0, 0), Version(2, 0, 0)),
        )

        // Empty rule has Unbounded version range, which doesn't count towards specificity
        assertEquals(0, ruleEmpty.specificity())

        // Single constraint rules have specificity of 1 (just their constraint)
        // Unbounded doesn't count towards specificity
        assertEquals(1, rulePlatform.specificity())
        assertEquals(1, ruleLocale.specificity())
        // ruleVersion has explicit FullyBound, so only version constraint counts
        assertEquals(1, ruleVersion.specificity())

        // Two constraints with Unbounded have specificity of 2
        assertEquals(2, rulePlatformAndLocale.specificity())

        // All three constraints have specificity of 3 (all explicitly set)
        assertEquals(3, ruleAll.specificity())
    }

    @Test
    fun `Given multiple locales or platforms, When calculating specificity, Then specificity counts presence not quantity`() {
        val ruleOnePlatform = Rule<Context>(
            rollout = Rampup.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded(),
        )

        val ruleTwoPlatforms = Rule<Context>(
            rollout = Rampup.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS, Platform.ANDROID),
            versionRange = Unbounded(),
        )

        val ruleOneLocale = Rule<Context>(
            rollout = Rampup.MAX,
            locales = setOf(AppLocale.UNITED_STATES),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val ruleTwoLocales = Rule<Context>(
            rollout = Rampup.MAX,
            locales = setOf(AppLocale.UNITED_STATES, AppLocale.CANADA),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        // Having multiple values in a dimension doesn't increase specificity - only presence matters
        // All rules here have specificity of 1 (just platform/locale constraint)
        // Unbounded doesn't count towards specificity
        assertEquals(1, ruleOnePlatform.specificity())
        assertEquals(1, ruleTwoPlatforms.specificity())
        assertEquals(1, ruleOneLocale.specificity())
        assertEquals(1, ruleTwoLocales.specificity())
    }

    @Test
    fun `Given rules with notes, When comparing specificity, Then notes serve as tiebreaker`() {
        val ruleA = Rule<Context>(
            rollout = Rampup.MAX,
            locales = setOf(AppLocale.UNITED_STATES),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded(),
            note = "Rule A",
        )

        val ruleB = Rule<Context>(
            rollout = Rampup.MAX,
            locales = setOf(AppLocale.UNITED_STATES),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded(),
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
