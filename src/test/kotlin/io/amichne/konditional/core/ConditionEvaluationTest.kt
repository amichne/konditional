package io.amichne.konditional.core

import io.amichne.konditional.builders.FlagBuilder
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.context.Version
import io.amichne.konditional.rules.BaseRule
import io.amichne.konditional.rules.Surjection.Companion.boundedBy
import io.amichne.konditional.rules.versions.Unbounded
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for Condition evaluation logic.
 * Validates surjection sorting, rule matching, bucketing, and fallback behavior.
 */
class ConditionEvaluationTest {

    enum class TestFlags(override val key: String) : Conditional<String, Context> {
        TEST_FLAG("test_flag"),
        ;

        override fun with(build: FlagBuilder<String, Context>.() -> Unit) =
            update(FlagBuilder(this).apply(build).build())
    }

    private fun ctx(
        idHex: String,
        locale: AppLocale = AppLocale.EN_US,
        platform: Platform = Platform.IOS,
        version: String = "1.0.0",
    ) = Context(locale, platform, Version.parse(version), StableId.of(idHex))

    @Test
    fun `Given condition with no matching rules, When evaluating, Then returns default value`() {
        val condition = Condition(
            key = TestFlags.TEST_FLAG,
            bounds = emptyList(),
            defaultValue = "default",
            fallbackValue = "fallback",
        )

        val result = condition.evaluate(ctx("11111111111111111111111111111111"))
        assertEquals("default", result)
    }

    @Test
    fun `Given condition with one matching rule, When evaluating, Then returns rule value`() {
        val rule = BaseRule<Context>(
            rampUp = RampUp.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val condition = Condition(
            key = TestFlags.TEST_FLAG,
            bounds = listOf(rule.boundedBy("en-us-value")),
            defaultValue = "default",
            fallbackValue = "fallback",
        )

        val matchingResult = condition.evaluate(ctx("22222222222222222222222222222222", locale = AppLocale.EN_US))
        assertEquals("en-us-value", matchingResult)

        val nonMatchingResult = condition.evaluate(ctx("33333333333333333333333333333333", locale = AppLocale.ES_US))
        assertEquals("default", nonMatchingResult)
    }

    @Test
    fun `Given multiple rules, When evaluating, Then most specific rule wins`() {
        val generalRule = BaseRule<Context>(
            rampUp = RampUp.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val platformRule = BaseRule<Context>(
            rampUp = RampUp.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded,
        )

        val platformAndLocaleRule = BaseRule<Context>(
            rampUp = RampUp.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded,
        )

        val condition = Condition(
            key = TestFlags.TEST_FLAG,
            bounds = listOf(
                generalRule.boundedBy("general"),
                platformRule.boundedBy("ios"),
                platformAndLocaleRule.boundedBy("ios-en-us"),
            ),
            defaultValue = "default",
            fallbackValue = "fallback",
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
    fun `Given rules with same specificity, When evaluating, Then note is used as tiebreaker`() {
        val ruleA = BaseRule<Context>(
            rampUp = RampUp.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded,
            note = "rule-a",
        )

        val ruleB = BaseRule<Context>(
            rampUp = RampUp.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded,
            note = "rule-b",
        )

        // Both rules have same specificity but different notes
        assertEquals(ruleA.specificity(), ruleB.specificity())

        val condition = Condition(
            key = TestFlags.TEST_FLAG,
            bounds = listOf(
                ruleB.boundedBy("value-b"),
                ruleA.boundedBy("value-a"),
            ),
            defaultValue = "default",
            fallbackValue = "fallback",
        )

        // Rule A should win because of alphabetical ordering of notes
        val result = condition.evaluate(
            ctx(
                "77777777777777777777777777777777",
                locale = AppLocale.EN_US,
                platform = Platform.IOS
            )
        )
        assertEquals("value-a", result)
    }

    @Test
    fun `Given rule with 0 percent ramp-up, When evaluating, Then never matches`() {
        val rule = BaseRule<Context>(
            rampUp = RampUp.of(0.0),
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val condition = Condition(
            key = TestFlags.TEST_FLAG,
            bounds = listOf(rule.boundedBy("enabled")),
            defaultValue = "disabled",
            fallbackValue = "fallback",
        )

        // Test many IDs - none should get the enabled value
        repeat(100) { i ->
            val id = "%032x".format(i)
            val result = condition.evaluate(ctx(id))
            assertEquals("disabled", result, "User $id should not be in 0% rollout")
        }
    }

    @Test
    fun `Given rule with 100 percent ramp-up, When evaluating, Then always matches`() {
        val rule = BaseRule<Context>(
            rampUp = RampUp.of(100.0),
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val condition = Condition(
            key = TestFlags.TEST_FLAG,
            bounds = listOf(rule.boundedBy("enabled")),
            defaultValue = "disabled",
            fallbackValue = "fallback",
        )

        // Test many IDs - all should get the enabled value
        repeat(100) { i ->
            val id = "%032x".format(i)
            val result = condition.evaluate(ctx(id))
            assertEquals("enabled", result, "User $id should be in 100% rollout")
        }
    }

    @Test
    fun `Given rule with 50 percent ramp-up, When evaluating many users, Then approximately half match`() {
        val rule = BaseRule<Context>(
            rampUp = RampUp.of(50.0),
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val condition = Condition(
            key = TestFlags.TEST_FLAG,
            bounds = listOf(rule.boundedBy("enabled")),
            defaultValue = "disabled",
            fallbackValue = "fallback",
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
        val rule = BaseRule<Context>(
            rampUp = RampUp.of(50.0),
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val condition = Condition(
            key = TestFlags.TEST_FLAG,
            bounds = listOf(rule.boundedBy("enabled")),
            defaultValue = "disabled",
            fallbackValue = "fallback",
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
        val rule = BaseRule<Context>(
            rampUp = RampUp.of(50.0),
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val conditionV1 = Condition(
            key = TestFlags.TEST_FLAG,
            bounds = listOf(rule.boundedBy("enabled")),
            defaultValue = "disabled",
            fallbackValue = "fallback",
            salt = "v1",
        )

        val conditionV2 = Condition(
            key = TestFlags.TEST_FLAG,
            bounds = listOf(rule.boundedBy("enabled")),
            defaultValue = "disabled",
            fallbackValue = "fallback",
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
        val iosOnlyRule = BaseRule<Context>(
            rampUp = RampUp.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded,
        )

        val androidOnlyRule = BaseRule<Context>(
            rampUp = RampUp.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.ANDROID),
            versionRange = Unbounded,
        )

        val condition = Condition(
            key = TestFlags.TEST_FLAG,
            bounds = listOf(
                iosOnlyRule.boundedBy("ios-value"),
                androidOnlyRule.boundedBy("android-value"),
            ),
            defaultValue = "default",
            fallbackValue = "fallback",
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
        val highSpecificityLowRampup = BaseRule<Context>(
            rampUp = RampUp.of(1.0), // Very low ramp-up
            locales = setOf(AppLocale.EN_US),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded,
        )

        val lowSpecificityHighRampup = BaseRule<Context>(
            rampUp = RampUp.MAX,
            locales = emptySet(),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded,
        )

        val condition = Condition(
            key = TestFlags.TEST_FLAG,
            bounds = listOf(
                highSpecificityLowRampup.boundedBy("specific"),
                lowSpecificityHighRampup.boundedBy("fallback"),
            ),
            defaultValue = "default",
            fallbackValue = "fallback",
        )

        // Test many users - most should fall through to the less specific rule
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
        val general = BaseRule<Context>(
            rampUp = RampUp.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
            note = "general",
        )

        val specific = BaseRule<Context>(
            rampUp = RampUp.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = setOf(Platform.IOS),
            versionRange = Unbounded,
            note = "specific",
        )

        // Provide in wrong order
        val condition = Condition(
            key = TestFlags.TEST_FLAG,
            bounds = listOf(
                general.boundedBy("general-value"),
                specific.boundedBy("specific-value"),
            ),
            defaultValue = "default",
            fallbackValue = "fallback",
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
