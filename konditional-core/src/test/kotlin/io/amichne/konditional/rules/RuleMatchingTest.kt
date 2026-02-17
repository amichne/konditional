package io.amichne.konditional.rules

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.fixtures.core.id.TestStableId
import io.amichne.konditional.fixtures.utilities.localeIds
import io.amichne.konditional.fixtures.utilities.platformIds
import io.amichne.konditional.rules.targeting.Targeting
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

    private fun rule(
        rampUp: RampUp = RampUp.MAX,
        locales: Set<String> = emptySet(),
        platforms: Set<String> = emptySet(),
        versionRange: io.amichne.konditional.rules.versions.VersionRange = Unbounded,
        note: String? = null,
    ): Rule<Context> {
        val leaves = buildList<Targeting<Context>> {
            if (locales.isNotEmpty()) add(Targeting.locale(locales))
            if (platforms.isNotEmpty()) add(Targeting.platform(platforms))
            if (versionRange != Unbounded) add(Targeting.version(versionRange))
        }
        return Rule(
            rampUp = rampUp,
            note = note,
            targeting = Targeting.All(leaves),
        )
    }

    @Test
    fun `Given rule with no constraints, When matching, Then all contexts match`() {
        val r = rule()

        assertTrue(r.matches(ctx()))
        assertTrue(r.matches(ctx(locale = AppLocale.UNITED_STATES)))
        assertTrue(r.matches(ctx(platform = Platform.ANDROID)))
        assertTrue(r.matches(ctx(version = "99.99.99")))
    }

    @Test
    fun `Given rule with single platform, When matching, Then only that platform matches`() {
        val r = rule(platforms = platformIds(Platform.IOS))

        assertTrue(r.matches(ctx(platform = Platform.IOS)))
        assertFalse(r.matches(ctx(platform = Platform.ANDROID)))
    }

    @Test
    fun `Given rule with multiple platforms, When matching, Then any of those platforms match`() {
        val r = rule(platforms = platformIds(Platform.IOS, Platform.ANDROID))

        assertTrue(r.matches(ctx(platform = Platform.IOS)))
        assertTrue(r.matches(ctx(platform = Platform.ANDROID)))
    }

    @Test
    fun `Given rule with single locale, When matching, Then only that locale matches`() {
        val r = rule(locales = localeIds(AppLocale.UNITED_STATES))

        assertTrue(r.matches(ctx(locale = AppLocale.UNITED_STATES)))
        assertFalse(r.matches(ctx(locale = AppLocale.MEXICO)))
        assertFalse(r.matches(ctx(locale = AppLocale.INDIA)))
    }

    @Test
    fun `Given rule with multiple locales, When matching, Then any of those locales match`() {
        val r = rule(locales = localeIds(AppLocale.UNITED_STATES, AppLocale.CANADA))

        assertTrue(r.matches(ctx(locale = AppLocale.UNITED_STATES)))
        assertTrue(r.matches(ctx(locale = AppLocale.CANADA)))
        assertFalse(r.matches(ctx(locale = AppLocale.MEXICO)))
    }

    @Test
    fun `Given rule with version range, When matching, Then only versions in range match`() {
        val r = rule(
            versionRange = FullyBound(
                min = Version(1, 0, 0),
                max = Version(2, 0, 0),
            ),
        )

        assertFalse(r.matches(ctx(version = "0.9.9")))
        assertTrue(r.matches(ctx(version = "1.0.0")))
        assertTrue(r.matches(ctx(version = "1.5.0")))
        assertTrue(r.matches(ctx(version = "2.0.0")))
        assertFalse(r.matches(ctx(version = "2.0.1")))
    }

    @Test
    fun `Given rule with combined constraints, When matching, Then all constraints must match`() {
        val r = rule(
            locales = localeIds(AppLocale.UNITED_STATES, AppLocale.CANADA),
            platforms = platformIds(Platform.IOS),
            versionRange = FullyBound(
                min = Version(2, 0, 0),
                max = Version(3, 0, 0),
            ),
        )

        // All constraints match
        assertTrue(r.matches(ctx(locale = AppLocale.UNITED_STATES, platform = Platform.IOS, version = "2.5.0")))

        // Wrong locale
        assertFalse(r.matches(ctx(locale = AppLocale.MEXICO, platform = Platform.IOS, version = "2.5.0")))

        // Wrong platform
        assertFalse(r.matches(ctx(locale = AppLocale.UNITED_STATES, platform = Platform.ANDROID, version = "2.5.0")))

        // Wrong version
        assertFalse(r.matches(ctx(locale = AppLocale.UNITED_STATES, platform = Platform.IOS, version = "1.5.0")))
    }

    @Test
    fun `Given rules with different specificity, When calculating specificity, Then more specific rules have higher scores`() {
        val ruleEmpty = rule()

        val rulePlatform = rule(platforms = platformIds(Platform.IOS))

        val ruleLocale = rule(locales = localeIds(AppLocale.UNITED_STATES))

        val ruleVersion = rule(
            versionRange = FullyBound(Version(1, 0, 0), Version(2, 0, 0)),
        )

        val rulePlatformAndLocale = rule(
            locales = localeIds(AppLocale.UNITED_STATES),
            platforms = platformIds(Platform.IOS),
        )

        val ruleAll = rule(
            locales = localeIds(AppLocale.UNITED_STATES, AppLocale.CANADA),
            platforms = platformIds(Platform.IOS, Platform.ANDROID),
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
        val ruleOnePlatform = rule(platforms = platformIds(Platform.IOS))

        val ruleTwoPlatforms = rule(platforms = platformIds(Platform.IOS, Platform.ANDROID))

        val ruleOneLocale = rule(locales = localeIds(AppLocale.UNITED_STATES))

        val ruleTwoLocales = rule(locales = localeIds(AppLocale.UNITED_STATES, AppLocale.CANADA))

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
        val ruleA = rule(
            locales = localeIds(AppLocale.UNITED_STATES),
            platforms = platformIds(Platform.IOS),
            note = "Rule A",
        )

        val ruleB = rule(
            locales = localeIds(AppLocale.UNITED_STATES),
            platforms = platformIds(Platform.IOS),
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
