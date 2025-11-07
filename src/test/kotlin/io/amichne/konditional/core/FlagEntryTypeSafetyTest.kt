package io.amichne.konditional.core

import io.amichne.konditional.builders.ConfigBuilder.Companion.config
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.evaluate
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.instance.Konfig
import io.amichne.konditional.core.internal.SingletonFlagRegistry
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy
import io.amichne.konditional.rules.versions.Unbounded
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for FlagEntry type safety wrapper.
 * Validates that the FlagEntry wrapper maintains type safety and eliminates unsafe casts.
 */
class FlagEntryTypeSafetyTest {

    private fun ctx(
        idHex: String,
        locale: AppLocale = AppLocale.EN_US,
        platform: Platform = Platform.IOS,
        version: String = "1.0.0",
    ) = Context(locale, platform, Version.parse(version), StableId.of(idHex))

    enum class BoolFlags(override val key: String) : Conditional<EncodableValue.BooleanEncodeable, Context> by Conditional(key) {
        FEATURE_A("feature_a"),
        FEATURE_B("feature_b"),
    }

    enum class StringFlags(override val key: String) : Conditional<String, Context> by Conditional(key) {
        CONFIG_A("config_a"),
        CONFIG_B("config_b"),
    }

    enum class IntFlags(override val key: String) : Conditional<Int, Context> by Conditional(key) {
        TIMEOUT("timeout"),
    }

    @Test
    fun `Given FlagDefinition, When created, Then maintains type information correctly`() {
        val rule = Rule<Context>(
            rollout = io.amichne.konditional.context.Rollout.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val flag = FlagDefinition(
            conditional = BoolFlags.FEATURE_A,
            values = listOf(rule.targetedBy(true)),
            defaultValue = false,
        )

        assertNotNull(flag)
        assertEquals(BoolFlags.FEATURE_A.key, flag.key)
        assertEquals(false, flag.defaultValue)
    }

    @Test
    fun `Given ContextualFeatureFlag, When evaluating, Then returns correct value type`() {
        val rule = Rule<Context>(
            rollout = io.amichne.konditional.context.Rollout.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val boolFlag: FeatureFlag<Boolean, Context> = FlagDefinition(
            conditional = BoolFlags.FEATURE_A,
            values = listOf(rule.targetedBy(true)),
            defaultValue = false,
        )

        val boolResult = boolFlag.evaluate(ctx("11111111111111111111111111111111", locale = AppLocale.EN_US))

        assertTrue(boolResult is Boolean)
        assertEquals(true, boolResult)
    }

    @Test
    fun `Given ContextualFeatureFlag with different value types, When evaluating, Then each returns correct type`() {
        val boolRule = Rule<Context>(
            rollout = io.amichne.konditional.context.Rollout.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val stringRule = Rule<Context>(
            rollout = io.amichne.konditional.context.Rollout.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val intRule = Rule<Context>(
            rollout = io.amichne.konditional.context.Rollout.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val boolFlag: FeatureFlag<Boolean, Context> = FlagDefinition(
            conditional = BoolFlags.FEATURE_A,
            values = listOf(boolRule.targetedBy(true)),
            defaultValue = false,
        )

        val stringFlag: FeatureFlag<String, Context> = FlagDefinition(
            conditional = StringFlags.CONFIG_A,
            values = listOf(stringRule.targetedBy("value")),
            defaultValue = "default",
        )

        val intFlag: FeatureFlag<Int, Context> = FlagDefinition(
            conditional = IntFlags.TIMEOUT,
            values = listOf(intRule.targetedBy(30)),
            defaultValue = 10,
        )

        val context = ctx("22222222222222222222222222222222")

        val boolResult = boolFlag.evaluate(context)
        val stringResult = stringFlag.evaluate(context)
        val intResult = intFlag.evaluate(context)

        assertTrue(boolResult is Boolean)
        assertTrue(stringResult is String)
        assertTrue(intResult is Int)

        assertEquals(true, boolResult)
        assertEquals("value", stringResult)
        assertEquals(30, intResult)
    }

    @Test
    fun `Given Snapshot with ContextualFeatureFlag instances, When loading, Then all flags are accessible`() {
        val boolRule = Rule<Context>(
            rollout = io.amichne.konditional.context.Rollout.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val stringRule = Rule<Context>(
            rollout = io.amichne.konditional.context.Rollout.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val boolFlag = FlagDefinition(
            conditional = BoolFlags.FEATURE_A,
            values = listOf(boolRule.targetedBy(true)),
            defaultValue = false,
        )

        val stringFlag = FlagDefinition(
            conditional = StringFlags.CONFIG_A,
            values = listOf(stringRule.targetedBy("test")),
            defaultValue = "default",
        )

        val konfig = Konfig(
            mapOf(
                BoolFlags.FEATURE_A to boolFlag,
                StringFlags.CONFIG_A to stringFlag,
            )
        )

        SingletonFlagRegistry.load(konfig)

        val context = ctx("33333333333333333333333333333333")
        val boolResult = context.evaluate(BoolFlags.FEATURE_A)
        val stringResult = context.evaluate(StringFlags.CONFIG_A)

        assertTrue(boolResult is Boolean)
        assertTrue(stringResult is String)
        assertEquals(true, boolResult)
        assertEquals("test", stringResult)
    }

    @Test
    fun `Given config with multiple flag types, When loaded, Then ContextualFeatureFlag maintains type safety`() {
        config {
            BoolFlags.FEATURE_A with {
                default(false)
                rule {
                    platforms(Platform.IOS)
                } implies true
            }
            BoolFlags.FEATURE_B with {
                default(true)
            }
            StringFlags.CONFIG_A with {
                default("default")
                rule {
                    platforms(Platform.ANDROID)
                } implies "android-value"
            }
            StringFlags.CONFIG_B with {
                default("config-b-default")
                rule {
                    locales(AppLocale.EN_US)
                } implies "en-us-value"
            }
            IntFlags.TIMEOUT with {
                default(10)
                rule {
                    versions {
                        min(2, 0)
                    }
                } implies 30
            }
        }

        val iosCtx = ctx("44444444444444444444444444444444", platform = Platform.IOS)
        val androidCtx = ctx("55555555555555555555555555555555", platform = Platform.ANDROID)
        val newVersionCtx = ctx("66666666666666666666666666666666", version = "2.5.0")

        // Boolean flags
        assertTrue(iosCtx.evaluate(BoolFlags.FEATURE_A))
        assertTrue(iosCtx.evaluate(BoolFlags.FEATURE_B))

        // String flags
        assertEquals("default", iosCtx.evaluate(StringFlags.CONFIG_A))
        assertEquals("android-value", androidCtx.evaluate(StringFlags.CONFIG_A))
        assertEquals("en-us-value", iosCtx.evaluate(StringFlags.CONFIG_B))

        // Int flags
        assertEquals(10, iosCtx.evaluate(IntFlags.TIMEOUT))
        assertEquals(30, newVersionCtx.evaluate(IntFlags.TIMEOUT))
    }

    @Test
    fun `Given ContextualFeatureFlag in map, When retrieving by key, Then type information is preserved`() {
        config {
            BoolFlags.FEATURE_A with {
                default(false)
                rule {
                } implies true
            }
            StringFlags.CONFIG_A with {
                default("default")
                rule {
                } implies "enabled"
            }
        }

        val context = ctx("77777777777777777777777777777777")

        // The evaluate() method internally retrieves FeatureFlag from the map
        // and casts it, maintaining type safety
        val boolResult = context.evaluate(BoolFlags.FEATURE_A)
        val stringResult = context.evaluate(StringFlags.CONFIG_A)

        // Type safety is maintained - we get the correct types back
        assertTrue(boolResult is Boolean)
        assertTrue(stringResult is String)
    }

    @Test
    fun `Given mixed context and value types, When using ContextualFeatureFlag, Then maintains both type parameters`() {
        data class CustomContext(
            override val locale: AppLocale,
            override val platform: Platform,
            override val appVersion: Version,
            override val stableId: StableId,
            val customField: String,
        ) : Context

        data class CustomIntFlag(override val key: String = "custom_int") : Conditional<Int, CustomContext> by Conditional(key)

        val customIntFlag = CustomIntFlag()

        val rule = Rule<CustomContext>(
            rollout = io.amichne.konditional.context.Rollout.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val flag: FeatureFlag<Int, CustomContext> = FlagDefinition(
            conditional = customIntFlag,
            values = listOf(rule.targetedBy(42)),
            defaultValue = 0,
        )

        val customCtx = CustomContext(
            locale = AppLocale.EN_US,
            platform = Platform.IOS,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("88888888888888888888888888888888"),
            customField = "test",
        )

        val result = flag.evaluate(customCtx)

        assertTrue(result is Int)
        assertEquals(42, result)
    }

    @Test
    fun `Given FlagEntry wrapper, When used in Flags singleton, Then no unchecked cast warnings at call site`() {
        // This test validates that the FlagEntry wrapper eliminates the need for
        // @Suppress("UNCHECKED_CAST") annotations at call sites

        config {
            BoolFlags.FEATURE_A with {
                default(false)
                rule {
                    platforms(Platform.IOS)
                } implies true
            }
        }

        val context = ctx("99999999999999999999999999999999", platform = Platform.IOS)

        // This call should not require any @Suppress annotation
        // The FlagEntry wrapper maintains type safety internally
        val result: Boolean = context.evaluate(BoolFlags.FEATURE_A)

        assertEquals(true, result)
    }
}
