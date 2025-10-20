package io.amichne.konditional.core

import io.amichne.konditional.builders.ConfigBuilder.Companion.config
import io.amichne.konditional.builders.FlagBuilder
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Flags.evaluate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringFlagsTest {
    // Define a simple enum for string-valued flags
    enum class StringFeatureFlags(
        override val key: String,
    ) : Conditional<String, Context> {
        API_ENDPOINT("api_endpoint"),
        THEME("theme"),
        WELCOME_MESSAGE("welcome_message"),
        ;

        override fun with(build: FlagBuilder<String, Context>.() -> Unit) = update(FlagBuilder(this).apply(build).build())
    }

    private fun ctx(
        idHex: String,
        locale: AppLocale = AppLocale.EN_US,
        platform: Platform = Platform.IOS,
        version: String = "7.12.3",
    ) = Context(locale, platform, Version.parse(version), StableId.of(idHex))

    @Test
    fun `Given platform targeting, When evaluating string flag, Then correct theme is returned`() {
        config {
            StringFeatureFlags.THEME with {
                default("light")
                boundary {
                    platforms(Platform.ANDROID)
                } implies "material"
                boundary {
                    platforms(Platform.IOS)
                } implies "cupertino"
            }
        }

        // Android should get material theme
        val androidResult =
            ctx(
                "11111111111111111111111111111111",
                platform = Platform.ANDROID,
            ).evaluate(StringFeatureFlags.THEME)
        assertEquals("material", androidResult)

        // iOS should get cupertino theme
        val iosResult =
            ctx(
                "22222222222222222222222222222222",
                platform = Platform.IOS,
            ).evaluate(StringFeatureFlags.THEME)
        assertEquals("cupertino", iosResult)
    }

    @Test
    fun `Given locale targeting, When evaluating string flag, Then correct message is returned`() {
        config {
            StringFeatureFlags.WELCOME_MESSAGE with {
                default("Welcome!")
                boundary {
                    locales(AppLocale.ES_US)
                } implies "¡Bienvenido!"
                boundary {
                    locales(AppLocale.EN_CA)
                } implies "Welcome, eh!"
                boundary {
                    locales(AppLocale.HI_IN)
                } implies "स्वागत है!"
            }
        }

        assertEquals(
            "Welcome!",
            ctx(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa01",
                locale = AppLocale.EN_US,
            ).evaluate(StringFeatureFlags.WELCOME_MESSAGE),
        )
        assertEquals(
            "¡Bienvenido!",
            ctx(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa02",
                locale = AppLocale.ES_US,
            ).evaluate(StringFeatureFlags.WELCOME_MESSAGE),
        )
        assertEquals(
            "Welcome, eh!",
            ctx(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa03",
                locale = AppLocale.EN_CA,
            ).evaluate(StringFeatureFlags.WELCOME_MESSAGE),
        )
        assertEquals(
            "स्वागत है!",
            ctx(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa04",
                locale = AppLocale.HI_IN,
            ).evaluate(StringFeatureFlags.WELCOME_MESSAGE),
        )
    }

    @Test
    fun `Given version targeting, When evaluating string flag, Then correct endpoint is returned`() {
        config {
            StringFeatureFlags.API_ENDPOINT with {
                default("https://api.example.com/v1")
                boundary {
                    versions {
                        min(8, 0)
                        max(8, 99, 99)
                    }
                } implies "https://api.example.com/v2"
                boundary {
                    versions {
                        min(9, 0)
                    }
                } implies "https://api.example.com/v3"
            }
        }

        // Version 7.x should use v1
        assertEquals(
            "https://api.example.com/v1",
            ctx("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb01", version = "7.5.0").evaluate(StringFeatureFlags.API_ENDPOINT),
        )

        // Version 8.x should use v2
        assertEquals(
            "https://api.example.com/v2",
            ctx("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb02", version = "8.2.0").evaluate(StringFeatureFlags.API_ENDPOINT),
        )

        // Version 9.x should use v3
        assertEquals(
            "https://api.example.com/v3",
            ctx("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb03", version = "9.1.0").evaluate(StringFeatureFlags.API_ENDPOINT),
        )
    }

    @Test
    fun `Given coverage rollout, When evaluating string flag, Then distribution is correct`() {
        config {
            StringFeatureFlags.API_ENDPOINT with {
                default("https://api-old.example.com")
                boundary {
                    // 30% of users get the new endpoint
                    rampUp = RampUp.of(30.0)
                } implies "https://api-new.example.com"
            }
        }

        // Sample many users to verify coverage distribution
        val samples = 5000
        var newEndpointCount = 0
        var oldEndpointCount = 0

        for (i in 0 until samples) {
            val id = "%032x".format(i)
            val result = ctx(id).evaluate(StringFeatureFlags.API_ENDPOINT)
            when (result) {
                "https://api-new.example.com" -> newEndpointCount++
                "https://api-old.example.com" -> oldEndpointCount++
            }
        }

        // Should have roughly 30% new, 70% old (with some tolerance)
        val newPct = newEndpointCount.toDouble() / samples
        assertTrue(newPct in 0.27..0.33, "Expected ~30% new endpoint, got ${newPct * 100}%")

        val oldPct = oldEndpointCount.toDouble() / samples
        assertTrue(oldPct in 0.67..0.73, "Expected ~70% old endpoint, got ${oldPct * 100}%")
    }

    @Test
    fun `Given complex targeting, When evaluating string flag, Then correct distribution is returned`() {
        config {
            StringFeatureFlags.API_ENDPOINT with {
                default("https://api.example.com/stable")

                // Beta API for iOS 9.0+ users at 25% rollout
                boundary {
                    platforms(Platform.IOS)
                    versions {
                        min(9, 0)
                    }
                    rampUp = RampUp.of(25.0)
                } implies "https://api.example.com/beta"

                // Canary API for all Android 10.0+ users
                boundary {
                    platforms(Platform.ANDROID)
                    versions {
                        min(10, 0)
                    }
                } implies "https://api.example.com/canary"
            }
        }

        // iOS 8.x users should get stable
        assertEquals(
            "https://api.example.com/stable",
            ctx("cccccccccccccccccccccccccccccc01", platform = Platform.IOS, version = "8.5.0").evaluate(
                StringFeatureFlags.API_ENDPOINT,
            ),
        )

        // Android 10.0+ users should always get canary (100% coverage)
        assertEquals(
            "https://api.example.com/canary",
            ctx(
                "cccccccccccccccccccccccccccccc02",
                platform = Platform.ANDROID,
                version = "10.0.0",
            ).evaluate(StringFeatureFlags.API_ENDPOINT),
        )

        // iOS 9.0+ users get beta or stable based on 25% coverage
        // Test a large sample to verify distribution
        val sampleSize = 2000
        var betaCount = 0
        var stableCount = 0

        for (i in 0 until sampleSize) {
            val id = "%032x".format(i)
            val result =
                ctx(id, platform = Platform.IOS, version = "9.2.0")
                    .evaluate(StringFeatureFlags.API_ENDPOINT)
            when (result) {
                "https://api.example.com/beta" -> betaCount++
                "https://api.example.com/stable" -> stableCount++
            }
        }

        val betaPct = betaCount.toDouble() / sampleSize
        assertTrue(betaPct in 0.22..0.28, "Expected ~25% beta for iOS 9.0+, got ${betaPct * 100}%")
    }

    @Test
    fun `Given same Id, When evaluating string flag multiple times, Then result is deterministic`() {
        config {
            StringFeatureFlags.THEME with {
                default("light")
                boundary {
                } implies "dark"
            }
        }

        val id = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1"
        val firstResult = ctx(id).evaluate(StringFeatureFlags.THEME)

        // Same user should always get the same result
        repeat(100) {
            val result = ctx(id).evaluate(StringFeatureFlags.THEME)
            assertEquals(firstResult, result, "User should get consistent results")
        }
    }

    @Test
    fun `Given same Id, When evaluating different string flags, Then results are independent and deterministic`() {
        config {
            StringFeatureFlags.THEME with {
                default("light")
                boundary {
                } implies "dark"
            }
            StringFeatureFlags.API_ENDPOINT with {
                default("https://api.example.com/v1")
                boundary {
                } implies "https://api.example.com/v2"
            }
        }

        val id = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"

        // Different flags can have different values for the same user (independent bucketing)
        val themeResult = ctx(id).evaluate(StringFeatureFlags.THEME)
        val apiResult = ctx(id).evaluate(StringFeatureFlags.API_ENDPOINT)

        // Just verify both return values (they can differ since bucketing is independent)
        assertTrue(themeResult in listOf("light", "dark"))
        assertTrue(apiResult in listOf("https://api.example.com/v1", "https://api.example.com/v2"))

        // But each should be deterministic for the same user
        repeat(10) {
            assertEquals(themeResult, ctx(id).evaluate(StringFeatureFlags.THEME))
            assertEquals(apiResult, ctx(id).evaluate(StringFeatureFlags.API_ENDPOINT))
        }
    }
}
