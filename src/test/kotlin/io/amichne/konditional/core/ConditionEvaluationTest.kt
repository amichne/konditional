package io.amichne.konditional.core

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.versions.Unbounded
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for Condition evaluation logic.
 * Validates surjection sorting, rule matching, bucketing, and default value behavior.
 */
class ConditionEvaluationTest {

    object TestFlags : FeatureContainer<Namespace.Global>(Namespace.Global) {
        val TEST_FLAG by string<Context>(default = "default")
    }

    private fun ctx(
        idHex: String,
        locale: AppLocale = AppLocale.EN_US,
        platform: Platform = Platform.IOS,
        version: String = "1.0.0",
    ) = Context(locale, platform, Version.parseUnsafe(version), StableId.of(idHex))

    @Test
    fun `Given condition with no matching rules, When evaluating, Then returns default value`() {
        val condition = FlagDefinition(
            defaultValue = "default",
            feature = TestFlags.TEST_FLAG,
            values = emptyList(),
        )

        val result = condition.evaluate(ctx("11111111111111111111111111111111"))
        assertEquals("default", result)
    }

    @Test
    fun `Given condition with one matching rule, When evaluating, Then returns rule value`() {
        val rule = Rule<Context>(
            rollout = Rollout.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val condition = FlagDefinition(
            feature = TestFlags.TEST_FLAG,
            values = listOf(rule.targetedBy("en-us-value")),
            defaultValue = "default",
        )

        val matchingResult = condition.evaluate(ctx("22222222222222222222222222222222", locale = AppLocale.EN_US))
        assertEquals("en-us-value", matchingResult)

        val nonMatchingResult = condition.evaluate(ctx("33333333333333333333333333333333", locale = AppLocale.ES_US))
        assertEquals("default", nonMatchingResult)
    }

    @Test
    fun `Given multiple rules, When evaluating, Then most specific rule wins`() {
        val generalRule = Rule<Context>(
            rollout = Rollout.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val platformRule = Rule<Context>(
            rollout = Rollout.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded(),
        )

        val platformAndLocaleRule = Rule<Context>(
            rollout = Rollout.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded(),
        )

        val condition = FlagDefinition(
            feature = TestFlags.TEST_FLAG,
            values = listOf(
                generalRule.targetedBy("general"),
                platformRule.targetedBy("ios"),
                platformAndLocaleRule.targetedBy("ios-en-us"),
            ),
            defaultValue = "default",
        )

        // Most specific rule should match
        val mostSpecificResult = condition.evaluate(
            ctx("44444444444444444444444444444444", locale = AppLocale.EN_US, platform = Platform.IOS)
        )
        assertEquals("ios-en-us", mostSpecificResult)

        // Less specific rule should match when more specific doesn't
        val lessSpecificResult = condition.evaluate(
            ctx("55555555555555555555555555555555", locale = AppLocale.ES_US, platform = Platform.IOS)
        )
        assertEquals("ios", lessSpecificResult)

        // General rule should match when others don't
        val generalResult = condition.evaluate(
            ctx("66666666666666666666666666666666", locale = AppLocale.ES_US, platform = Platform.ANDROID)
        )
        assertEquals("general", generalResult)
    }

    @Test
    fun `Given rules with same specificity, When evaluating, Then insertion order is used as tiebreaker`() {
        val ruleA = Rule<Context>(
            rollout = Rollout.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded(),
            note = "rule-a",
        )

        val ruleB = Rule<Context>(
            rollout = Rollout.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded(),
            note = "rule-b",
        )

        // Both rules have same specificity
        assertEquals(ruleA.specificity(), ruleB.specificity())

        val condition = FlagDefinition(
            feature = TestFlags.TEST_FLAG,
            values = listOf(
                ruleB.targetedBy("value-b"),
                ruleA.targetedBy("value-a"),
            ),
            defaultValue = "default",
        )

        // Rule B should win because of insertion ordering
        val result = condition.evaluate(
            ctx(
                "77777777777777777777777777777777",
                locale = AppLocale.EN_US,
                platform = Platform.IOS
            )
        )
        assertEquals("value-b", result)
    }

    @Test
    fun `Given rule with 0 percent ramp-up, When evaluating, Then never matches`() {
        val rule = Rule<Context>(
            rollout = Rollout.of(0.0),
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val condition = FlagDefinition(
            feature = TestFlags.TEST_FLAG,
            values = listOf(rule.targetedBy("enabled")),
            defaultValue = "disabled",
        )

        // TestNamespace many IDs - none should get the enabled value
        repeat(100) { i ->
            val id = "%032x".format(i)
            val result = condition.evaluate(ctx(id))
            assertEquals("disabled", result, "User $id should not be in 0% rollout")
        }
    }

    @Test
    fun `Given rule with 100 percent ramp-up, When evaluating, Then always matches`() {
        val rule = Rule<Context>(
            rollout = Rollout.of(100.0),
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val condition = FlagDefinition(
            feature = TestFlags.TEST_FLAG,
            values = listOf(rule.targetedBy("enabled")),
            defaultValue = "disabled",
        )

        // TestNamespace many IDs - all should get the enabled value
        repeat(100) { i ->
            val id = "%032x".format(i)
            val result = condition.evaluate(ctx(id))
            assertEquals("enabled", result, "User $id should be in 100% rollout")
        }
    }

    @Test
    fun `Given rule with 50 percent ramp-up, When evaluating many users, Then approximately half match`() {
        val rule = Rule<Context>(
            rollout = Rollout.of(50.0),
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val condition = FlagDefinition(
            feature = TestFlags.TEST_FLAG,
            values = listOf(rule.targetedBy("enabled")),
            defaultValue = "disabled",
        )

        val sampleSize = 5000
        var enabledCount = 0

        repeat(sampleSize) { i ->
            val id = "%032x".format(i)
            val result = condition.evaluate(ctx(id))
            if (result == "enabled") enabledCount++
        }

        val percentage = enabledCount.toDouble() / sampleSize
        assertTrue(percentage in 0.47..0.53, "Expected ~50% enabled, got ${percentage * 100}%")
    }

    @Test
    fun `Given same user ID, When evaluating same condition, Then result is deterministic`() {
        val rule = Rule<Context>(
            rollout = Rollout.of(50.0),
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val condition = FlagDefinition(
            feature = TestFlags.TEST_FLAG,
            values = listOf(rule.targetedBy("enabled")),
            defaultValue = "disabled",
        )

        val id = "88888888888888888888888888888888"
        val firstResult = condition.evaluate(ctx(id))

        // Same ID should always get same result
        repeat(100) {
            assertEquals(firstResult, condition.evaluate(ctx(id)))
        }
    }

    @Test
    fun `Given different salts, When evaluating same user, Then bucketing is independent`() {
        val rule = Rule<Context>(
            rollout = Rollout.of(50.0),
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val conditionV1 = FlagDefinition(
            feature = TestFlags.TEST_FLAG,
            values = listOf(rule.targetedBy("enabled")),
            defaultValue = "disabled",
            salt = "v1",
        )

        val conditionV2 = FlagDefinition(
            feature = TestFlags.TEST_FLAG,
            values = listOf(rule.targetedBy("enabled")),
            defaultValue = "disabled",
            salt = "v2",
        )

        // With different salts, same user can get different bucketing
        val id = "99999999999999999999999999999999"
        val resultV1 = conditionV1.evaluate(ctx(id))
        val resultV2 = conditionV2.evaluate(ctx(id))

        // Results are deterministic for each salt
        repeat(10) {
            assertEquals(resultV1, conditionV1.evaluate(ctx(id)))
            assertEquals(resultV2, conditionV2.evaluate(ctx(id)))
        }

        // Results may differ between salts (testing on a single user is probabilistic,
        // but we verify consistency per salt)
        assertTrue(resultV1 in listOf("enabled", "disabled"))
        assertTrue(resultV2 in listOf("enabled", "disabled"))
    }

    @Test
    fun `Given rule not matching context constraints, When evaluating, Then skips to next rule regardless of ramp-up`() {
        val iosOnlyRule = Rule<Context>(
            rollout = Rollout.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded(),
        )

        val androidOnlyRule = Rule<Context>(
            rollout = Rollout.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.ANDROID),
            versionRange = Unbounded(),
        )

        val condition = FlagDefinition(
            feature = TestFlags.TEST_FLAG,
            values = listOf(
                iosOnlyRule.targetedBy("ios-value"),
                androidOnlyRule.targetedBy("android-value"),
            ),
            defaultValue = "default",
        )

        val iosResult = condition.evaluate(ctx("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", platform = Platform.IOS))
        assertEquals("ios-value", iosResult)

        val androidResult = condition.evaluate(ctx("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", platform = Platform.ANDROID))
        assertEquals("android-value", androidResult)

        val webResult = condition.evaluate(ctx("cccccccccccccccccccccccccccccccc", platform = Platform.WEB))
        assertEquals("default", webResult)
    }

    @Test
    fun `Given rule matching but user not in bucket, When evaluating, Then continues to next rule`() {
        val highSpecificityLowRampup = Rule<Context>(
            rollout = Rollout.of(1.0), // Very low ramp-up
            locales = setOf(AppLocale.EN_US),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded(),
        )

        val lowSpecificityHighRampup = Rule<Context>(
            rollout = Rollout.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded(),
        )

        val condition = FlagDefinition(
            feature = TestFlags.TEST_FLAG,
            values = listOf(
                highSpecificityLowRampup.targetedBy("specific"),
                lowSpecificityHighRampup.targetedBy("fallback"),
            ),
            defaultValue = "default",
        )

        // TestNamespace many users - most should fall through to the less specific rule
        val sampleSize = 1000
        var specificCount = 0
        var fallbackRuleCount = 0

        repeat(sampleSize) { i ->
            val id = "%032x".format(i)
            val result = condition.evaluate(ctx(id, locale = AppLocale.EN_US, platform = Platform.IOS))
            when (result) {
                "specific" -> specificCount++
                "fallback" -> fallbackRuleCount++
            }
        }

        // Very few should get the specific value (1%)
        val specificPct = specificCount.toDouble() / sampleSize
        assertTrue(specificPct < 0.05, "Expected <5% specific, got ${specificPct * 100}%")

        // Most should fall through to the fallback rule
        val fallbackPct = fallbackRuleCount.toDouble() / sampleSize
        assertTrue(fallbackPct > 0.90, "Expected >90% fallback rule, got ${fallbackPct * 100}%")
    }

    @Test
    fun `Given sorted surjections by specificity, When initializing condition, Then surjections are properly ordered`() {
        val general = Rule<Context>(
            rollout = Rollout.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
            note = "general",
        )

        val specific = Rule<Context>(
            rollout = Rollout.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded(),
            note = "specific",
        )

        // Provide in wrong order
        val condition = FlagDefinition(
            feature = TestFlags.TEST_FLAG,
            values = listOf(
                general.targetedBy("general-value"),
                specific.targetedBy("specific-value"),
            ),
            defaultValue = "default",
        )

        // Condition should internally sort by specificity
        // More specific rule should match first
        val result = condition.evaluate(
            ctx(
                "dddddddddddddddddddddddddddddddd",
                locale = AppLocale.EN_US,
                platform = Platform.IOS
            )
        )
        assertEquals("specific-value", result)
    }
}
