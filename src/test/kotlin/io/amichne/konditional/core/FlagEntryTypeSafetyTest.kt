package io.amichne.konditional.core

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Context.Companion.evaluate
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout.Companion.MAX
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace.Global
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.versions.Unbounded
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for FlagEntry type safety wrapper.
 * Validates that the FlagEntry wrapper maintains type safety and eliminates unsafe casts.
 */
class FlagEntryTypeSafetyTest {
    private fun ctx(
        idHex: String,
        locale: AppLocale = AppLocale.UNITED_STATES,
        platform: Platform = Platform.IOS,
        version: String = "1.0.0",
    ) = Context(locale, platform, Version.parseUnsafe(version), StableId.of(idHex))

    private object Features : FeatureContainer<Global>(Namespace.Global) {
        val featureA by boolean<Context>(default = false) {
            rule {
                platforms(Platform.IOS)
            } returns true
        }
        val featureB by boolean<Context>(default = true)
        val configA by string<Context>(default = "default") {
            rule {
                platforms(Platform.ANDROID)
            } returns "android-value"
        }

        val configB by string<Context>(default = "config-b-default") {
            rule {
                locales(AppLocale.UNITED_STATES)
            } returns "en-us-value"
        }
        val timeout by int<Context>(default = 10) {
            rule {
                versions {
                    min(2, 0)
                }
            } returns 30
        }
    }

    @Test
    fun `Given FlagDefinition, When created, Then maintains type information correctly`() {
        val rule = Rule<Context>(
            rollout = MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val flag = FlagDefinition(
            feature = Features.featureA,
            values = listOf(rule.targetedBy(true)),
            defaultValue = false,
        )

        assertNotNull(flag)
        assertEquals(Features.featureA.key, flag.feature.key)
        assertEquals(false, flag.defaultValue)
    }

    @Test
    fun `Given ContextualFlagDefinition, When evaluating, Then returns correct value type`() {
        val rule = Rule<Context>(
            rollout = MAX,
            locales = setOf(AppLocale.UNITED_STATES),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val boolFlag: FlagDefinition<EncodableValue.BooleanEncodeable, Boolean, Context, Namespace.Global> = FlagDefinition(
            feature = Features.featureB,
            values = listOf(rule.targetedBy(true)),
            defaultValue = false,
        )

        val boolResult = boolFlag.evaluate(ctx("11111111111111111111111111111111", locale = AppLocale.UNITED_STATES))


        assertEquals(true, boolResult)
    }

    @Test
    fun `Given ContextualFlagDefinition with different value types, When evaluating, Then each returns correct type`() {
        val boolRule = Rule<Context>(
            rollout = MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val stringRule = Rule<Context>(
            rollout = MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val intRule = Rule<Context>(
            rollout = MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val boolFlag: FlagDefinition<EncodableValue.BooleanEncodeable, Boolean, Context, Namespace.Global> = FlagDefinition(
            feature = Features.featureA,
            values = listOf(boolRule.targetedBy(true)),
            defaultValue = false,
        )

        val stringFlag: FlagDefinition<EncodableValue.StringEncodeable, String, Context, Namespace.Global> = FlagDefinition(
            feature = Features.configA,
            values = listOf(stringRule.targetedBy("value")),
            defaultValue = "default",
        )

        val intFlag: FlagDefinition<EncodableValue.IntEncodeable, Int, Context, Namespace.Global> = FlagDefinition(
            feature = Features.timeout,
            values = listOf(intRule.targetedBy(30)),
            defaultValue = 10,
        )

        val context = ctx("22222222222222222222222222222222")

        val boolResult = boolFlag.evaluate(context)
        val stringResult = stringFlag.evaluate(context)
        val intResult = intFlag.evaluate(context)

        assertEquals(true, boolResult)
        assertEquals("value", stringResult)
        assertEquals(30, intResult)
    }

    @Test
    fun `Given Snapshot with ContextualFlagDefinition instances, When loading, Then all flags are accessible`() {
        val boolRule = Rule<Context>(
            rollout = MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val stringRule = Rule<Context>(
            rollout = MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val boolFlag = FlagDefinition(
            feature = Features.featureA,
            values = listOf(boolRule.targetedBy(true)),
            defaultValue = false,
        )

        val stringFlag = FlagDefinition(
            feature = Features.configA,
            values = listOf(stringRule.targetedBy("test")),
            defaultValue = "default",
        )

        val configuration = Configuration(
            mapOf(
                Features.featureA to boolFlag,
                Features.configA to stringFlag,
            )
        )

        Namespace.Global.load(configuration)

        val context = ctx("33333333333333333333333333333333")
        val boolResult = context.evaluate(Features.featureA)
        val stringResult = context.evaluate(Features.configA)



        assertEquals(true, boolResult)
        assertEquals("test", stringResult)
    }
//
//    @TestNamespace
//    fun `Given config with multiple flag types, When loaded, Then ContextualFlagDefinition maintains type safety`() {
//        Namespace.Global.config {
//        }
//
//        val iosCtx = ctx("44444444444444444444444444444444", platform = Platform.IOS)
//        val androidCtx = ctx("55555555555555555555555555555555", platform = Platform.ANDROID)
//        val newVersionCtx = ctx("66666666666666666666666666666666", version = "2.5.0")
//
//        // Boolean flags
//        assertTrue(iosCtx.evaluate(BoolFlags.FEATURE_A))
//        assertTrue(iosCtx.evaluate(BoolFlags.FEATURE_B))
//
//        // String flags
//        assertEquals("default", iosCtx.evaluate(StringFlags.CONFIG_A))
//        assertEquals("android-value", androidCtx.evaluate(StringFlags.CONFIG_A))
//        assertEquals("en-us-value", iosCtx.evaluate(StringFlags.CONFIG_B))
//
//        // Int flags
//        assertEquals(10, iosCtx.evaluate(IntFlags.TIMEOUT))
//        assertEquals(30, newVersionCtx.evaluate(IntFlags.TIMEOUT))
//    }
//
//    @TestNamespace
//    fun `Given ContextualFlagDefinition in map, When retrieving by key, Then type information is preserved`() {
//        Namespace.Global.config {
//            BoolFlags.FEATURE_A with {
//                default(false)
//                rule {} returns true
//            }
//            StringFlags.CONFIG_A with {
//                default("default")
//                rule {} returns "enabled"
//            }
//        }
//
//        val context = ctx("77777777777777777777777777777777")
//
//        // The evaluate() method internally retrieves FlagDefinition from the map
//        // and casts it, maintaining type safety
//        context.evaluate(BoolFlags.FEATURE_A)
//        context.evaluate(StringFlags.CONFIG_A)
//
//        // Type safety is maintained - we get the correct types back
//    }
//
//    @TestNamespace
//    fun `Given mixed context and value types, When using ContextualFlagDefinition, Then maintains both type parameters`() {
//        data class CustomContext(
//            override val locale: AppLocale,
//            override val platform: Platform,
//            override val appVersion: Version,
//            override val stableId: StableId,
//            val customField: String,
//        ) : Context
//
//        data class CustomIntFlag(override val key: String = "custom_int") :
//            IntFeature<CustomContext, Namespace.Global> {
//            override val module: Namespace.Global = Namespace.Global
//        }
//
//        val customIntFlag = CustomIntFlag()
//
//        val rule = Rule<CustomContext>(
//            rollout { MAX }
//            locales = emptySet(),
//            platforms = emptySet(),
//            versionRange = Unbounded(),
//        )
//
//        val flag: FlagDefinition<EncodableValue.IntEncodeable, Int, CustomContext, Namespace.Global> = FlagDefinition(
//            feature = customIntFlag,
//            values = listOf(rule.targetedBy(42)),
//            defaultValue = 0,
//        )
//
//        val customCtx = CustomContext(
//            locale = AppLocale.UNITED_STATES,
//            platform = Platform.IOS,
//            appVersion = Version(1, 0, 0),
//            stableId = StableId.of("88888888888888888888888888888888"),
//            customField = "test",
//        )
//
//        val result = flag.evaluate(customCtx)
//
//
//        assertEquals(42, result)
//    }
//
//    @TestNamespace
//    fun `Given FlagEntry wrapper, When used in Flags singleton, Then no unchecked cast warnings at call site`() {
//        // This test validates that the FlagEntry wrapper eliminates the need for
//        // @Suppress("UNCHECKED_CAST") annotations at call sites
//
//        Namespace.Global.config {
//            BoolFlags.FEATURE_A with {
//                default(false)
//                rule {
//                    platforms(Platform.IOS)
//                } returns true
//            }
//        }
//
//        val context = ctx("99999999999999999999999999999999", platform = Platform.IOS)
//
//        // This call should not require any @Suppress annotation
//        // The FlagEntry wrapper maintains type safety internally
//        val result: Boolean = context.evaluate(BoolFlags.FEATURE_A)
//
//        assertEquals(true, result)
//    }
}
