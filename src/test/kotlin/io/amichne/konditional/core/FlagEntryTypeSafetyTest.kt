package io.amichne.konditional.core

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.RampUp.Companion.MAX
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.fixtures.utilities.localeIds
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

    private object Features : Namespace.TestNamespaceFacade("flag-entry-type-safety") {
        val featureA by boolean<Context>(default = false) {
            rule(true) {
                platforms(Platform.IOS)
            }
        }
        val featureB by boolean<Context>(default = true)
        val configA by string<Context>(default = "default") {
            rule("android-value") {
                platforms(Platform.ANDROID)
            }
        }

        val configB by string<Context>(default = "config-b-default") {
            rule("en-us-value") {
                locales(AppLocale.UNITED_STATES)
            }
        }
        val timeout by integer<Context>(default = 10) {
            rule(30) {
                versions {
                    min(2, 0)
                }
            }
        }
    }

    @Test
    fun `Given FlagDefinition, When created, Then maintains type information correctly`() {
        val rule = Rule<Context>(
            rampUp = MAX,
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
            rampUp = MAX,
            locales = localeIds(AppLocale.UNITED_STATES),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val boolFlag: FlagDefinition<Boolean, Context, Features> = FlagDefinition(
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
            rampUp = MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val stringRule = Rule<Context>(
            rampUp = MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val intRule = Rule<Context>(
            rampUp = MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val boolFlag: FlagDefinition<Boolean, Context, Features> = FlagDefinition(
            feature = Features.featureA,
            values = listOf(boolRule.targetedBy(true)),
            defaultValue = false,
        )

        val stringFlag: FlagDefinition<String, Context, Features> = FlagDefinition(
            feature = Features.configA,
            values = listOf(stringRule.targetedBy("value")),
            defaultValue = "default",
        )

        val intFlag: FlagDefinition<Int, Context, Features> = FlagDefinition(
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
            rampUp = MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )

        val stringRule = Rule<Context>(
            rampUp = MAX,
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

        Features.load(configuration)

        val context = ctx("33333333333333333333333333333333")
        val boolResult = Features.featureA.evaluate(context)
        val stringResult = Features.configA.evaluate(context)

        assertEquals(true, boolResult)
        assertEquals("test", stringResult)
    }
}
