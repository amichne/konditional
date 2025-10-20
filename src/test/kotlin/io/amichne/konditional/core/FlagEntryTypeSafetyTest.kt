package io.amichne.konditional.core

import io.amichne.konditional.builders.ConfigBuilder.Companion.config
import io.amichne.konditional.builders.FlagBuilder
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Flags.evaluate
import io.amichne.konditional.rules.BaseRule
import io.amichne.konditional.rules.Surjection.Companion.boundedBy
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

    enum class BoolFlags(override val key: String) : Conditional<Boolean, Context> {
        FEATURE_A("feature_a"),
        FEATURE_B("feature_b"),
        ;

        override fun with(build: FlagBuilder<Boolean, Context>.() -> Unit) =
            update(FlagBuilder(this).apply(build).build())
    }

    enum class StringFlags(override val key: String) : Conditional<String, Context> {
        CONFIG_A("config_a"),
        CONFIG_B("config_b"),
        ;

        override fun with(build: FlagBuilder<String, Context>.() -> Unit) =
            update(FlagBuilder(this).apply(build).build())
    }

    enum class IntFlags(override val key: String) : Conditional<Int, Context> {
        TIMEOUT("timeout"),
        ;

        override fun with(build: FlagBuilder<Int, Context>.() -> Unit) =
            update(FlagBuilder(this).apply(build).build())
    }

    @Test
    fun `Given FlagEntry, When created, Then wraps condition correctly`() {
        val rule = BaseRule<Context>(
            rampUp = io.amichne.konditional.context.RampUp.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val condition = Condition(
            key = BoolFlags.FEATURE_A,
            bounds = listOf(rule.boundedBy(true)),
            defaultValue = false,
            fallbackValue = false,
        )

        val entry = Flags.FlagEntry(condition)

        assertNotNull(entry)
        assertEquals(condition, entry.condition)
    }

    @Test
    fun `Given FlagEntry, When evaluating, Then returns correct value type`() {
        val rule = BaseRule<Context>(
            rampUp = io.amichne.konditional.context.RampUp.MAX,
            locales = setOf(AppLocale.EN_US),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val boolCondition = Condition(
            key = BoolFlags.FEATURE_A,
            bounds = listOf(rule.boundedBy(true)),
            defaultValue = false,
            fallbackValue = false,
        )

        val boolEntry = Flags.FlagEntry(boolCondition)
        val boolResult = boolEntry.evaluate(ctx("11111111111111111111111111111111", locale = AppLocale.EN_US))

        assertTrue(boolResult is Boolean)
        assertEquals(true, boolResult)
    }

    @Test
    fun `Given FlagEntry with different value types, When evaluating, Then each returns correct type`() {
        val boolRule = BaseRule<Context>(
            rampUp = io.amichne.konditional.context.RampUp.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val stringRule = BaseRule<Context>(
            rampUp = io.amichne.konditional.context.RampUp.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val intRule = BaseRule<Context>(
            rampUp = io.amichne.konditional.context.RampUp.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val boolCondition = Condition(
            key = BoolFlags.FEATURE_A,
            bounds = listOf(boolRule.boundedBy(true)),
            defaultValue = false,
            fallbackValue = false,
        )

        val stringCondition = Condition(
            key = StringFlags.CONFIG_A,
            bounds = listOf(stringRule.boundedBy("value")),
            defaultValue = "default",
            fallbackValue = "fallback",
        )

        val intCondition = Condition(
            key = IntFlags.TIMEOUT,
            bounds = listOf(intRule.boundedBy(30)),
            defaultValue = 10,
            fallbackValue = 5,
        )

        val context = ctx("22222222222222222222222222222222")

        val boolEntry = Flags.FlagEntry(boolCondition)
        val stringEntry = Flags.FlagEntry(stringCondition)
        val intEntry = Flags.FlagEntry(intCondition)

        val boolResult = boolEntry.evaluate(context)
        val stringResult = stringEntry.evaluate(context)
        val intResult = intEntry.evaluate(context)

        assertTrue(boolResult is Boolean)
        assertTrue(stringResult is String)
        assertTrue(intResult is Int)

        assertEquals(true, boolResult)
        assertEquals("value", stringResult)
        assertEquals(30, intResult)
    }

    @Test
    fun `Given Snapshot with FlagEntry instances, When loading, Then all entries are accessible`() {
        val boolRule = BaseRule<Context>(
            rampUp = io.amichne.konditional.context.RampUp.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val stringRule = BaseRule<Context>(
            rampUp = io.amichne.konditional.context.RampUp.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val boolCondition = Condition(
            key = BoolFlags.FEATURE_A,
            bounds = listOf(boolRule.boundedBy(true)),
            defaultValue = false,
            fallbackValue = false,
        )

        val stringCondition = Condition(
            key = StringFlags.CONFIG_A,
            bounds = listOf(stringRule.boundedBy("test")),
            defaultValue = "default",
            fallbackValue = "fallback",
        )

        val snapshot = Flags.Snapshot(
            mapOf(
                BoolFlags.FEATURE_A to Flags.FlagEntry(boolCondition),
                StringFlags.CONFIG_A to Flags.FlagEntry(stringCondition),
            )
        )

        Flags.load(snapshot)

        val context = ctx("33333333333333333333333333333333")
        val boolResult = context.evaluate(BoolFlags.FEATURE_A)
        val stringResult = context.evaluate(StringFlags.CONFIG_A)

        assertTrue(boolResult is Boolean)
        assertTrue(stringResult is String)
        assertEquals(true, boolResult)
        assertEquals("test", stringResult)
    }

    @Test
    fun `Given config with multiple flag types, When loaded, Then FlagEntry maintains type safety`() {
        config {
            BoolFlags.FEATURE_A with {
                default(false)
                boundary {
                    platforms(Platform.IOS)
                } implies true
            }
            BoolFlags.FEATURE_B with {
                default(true)
            }
            StringFlags.CONFIG_A with {
                default("default")
                boundary {
                    platforms(Platform.ANDROID)
                } implies "android-value"
            }
            StringFlags.CONFIG_B with {
                default("config-b-default")
                boundary {
                    locales(AppLocale.EN_US)
                } implies "en-us-value"
            }
            IntFlags.TIMEOUT with {
                default(10)
                boundary {
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
    fun `Given FlagEntry in map, When retrieving by key, Then type information is preserved through wrapper`() {
        config {
            BoolFlags.FEATURE_A with {
                default(false)
                boundary {
                } implies true
            }
            StringFlags.CONFIG_A with {
                default("default")
                boundary {
                } implies "enabled"
            }
        }

        val context = ctx("77777777777777777777777777777777")

        // The evaluate() method internally retrieves FlagEntry from the map
        // and casts it, but the wrapper ensures type safety
        val boolResult = context.evaluate(BoolFlags.FEATURE_A)
        val stringResult = context.evaluate(StringFlags.CONFIG_A)

        // Type safety is maintained - we get the correct types back
        assertTrue(boolResult is Boolean)
        assertTrue(stringResult is String)
    }

    @Test
    fun `Given mixed context and value types, When using FlagEntry, Then maintains both type parameters`() {
        data class CustomContext(
            override val locale: AppLocale,
            override val platform: Platform,
            override val appVersion: Version,
            override val stableId: StableId,
            val customField: String,
        ) : Context

        data class CustomIntFlag(override val key: String = "custom_int") : Conditional<Int, CustomContext> {
            override fun with(build: FlagBuilder<Int, CustomContext>.() -> Unit) =
                update(FlagBuilder(this).apply(build).build())
        }

        val customIntFlag = CustomIntFlag()

        val rule = BaseRule<CustomContext>(
            rampUp = io.amichne.konditional.context.RampUp.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded,
        )

        val condition = Condition(
            key = customIntFlag,
            bounds = listOf(rule.boundedBy(42)),
            defaultValue = 0,
            fallbackValue = -1,
        )

        val entry = Flags.FlagEntry(condition)

        val customCtx = CustomContext(
            locale = AppLocale.EN_US,
            platform = Platform.IOS,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("88888888888888888888888888888888"),
            customField = "test",
        )

        val result = entry.evaluate(customCtx)

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
                boundary {
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
