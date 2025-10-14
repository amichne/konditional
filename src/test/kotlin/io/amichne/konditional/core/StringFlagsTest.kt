package io.amichne.konditional.core

import io.amichne.konditional.builders.ConfigBuilder.Companion.config
import io.amichne.konditional.builders.FlagBuilder
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Flags.evaluate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringFlagsTest {

    // Define a simple enum for string-valued flags
    enum class StringFeatureFlags(override val key: String) : FeatureFlag<StringFlaggable> {
        API_ENDPOINT("api_endpoint"),
        THEME("theme"),
        WELCOME_MESSAGE("welcome_message");

        override fun withRules(fn: FlagBuilder<StringFlaggable>.() -> Unit) =
            update(FlagBuilder(this).apply(fn).build())
    }

    private fun ctx(
        idHex: String,
        locale: AppLocale = AppLocale.EN_US,
        platform: Platform = Platform.IOS,
        version: String = "7.12.3"
    ) = Context(locale, platform, Version.parse(version), StableId.of(idHex))

    @Test
    fun string_flag_with_platform_targeting() {
        config {
            StringFeatureFlags.THEME withRules {
                default(StringFlaggable("light"))
                rule {
                    platforms(Platform.ANDROID)
                    value(StringFlaggable("material"))
                }
                rule {
                    platforms(Platform.IOS)
                    value(StringFlaggable("cupertino"))
                }
            }
        }

        // Android should get material theme
        val androidResult = ctx(
            "11111111111111111111111111111111",
            platform = Platform.ANDROID
        ).evaluate(StringFeatureFlags.THEME)
        assertEquals("material", androidResult?.value)

        // iOS should get cupertino theme
        val iosResult = ctx(
            "22222222222222222222222222222222",
            platform = Platform.IOS
        ).evaluate(StringFeatureFlags.THEME)
        assertEquals("cupertino", iosResult?.value)
    }

    @Test
    fun string_flag_with_locale_targeting() {
        config {
            StringFeatureFlags.WELCOME_MESSAGE withRules {
                default(StringFlaggable("Welcome!"))
                rule {
                    locales(AppLocale.ES_US)
                    value(StringFlaggable("¡Bienvenido!"))
                }
                rule {
                    locales(AppLocale.EN_CA)
                    value(StringFlaggable("Welcome, eh!"))
                }
                rule {
                    locales(AppLocale.HI_IN)
                    value(StringFlaggable("स्वागत है!"))
                }
            }
        }

        assertEquals("Welcome!", ctx("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa01", locale = AppLocale.EN_US).evaluate(StringFeatureFlags.WELCOME_MESSAGE)?.value)
        assertEquals("¡Bienvenido!", ctx("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa02", locale = AppLocale.ES_US).evaluate(StringFeatureFlags.WELCOME_MESSAGE)?.value)
        assertEquals("Welcome, eh!", ctx("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa03", locale = AppLocale.EN_CA).evaluate(StringFeatureFlags.WELCOME_MESSAGE)?.value)
        assertEquals("स्वागत है!", ctx("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa04", locale = AppLocale.HI_IN).evaluate(StringFeatureFlags.WELCOME_MESSAGE)?.value)
    }

    @Test
    fun string_flag_with_version_targeting() {
        config {
            StringFeatureFlags.API_ENDPOINT withRules {
                default(StringFlaggable("https://api.example.com/v1"))
                rule {
                    versions {
                        atLeast(8, 0)
                        atMost(8, 99, 99)
                    }
                    value(StringFlaggable("https://api.example.com/v2"))
                }
                rule {
                    versions {
                        atLeast(9, 0)
                    }
                    value(StringFlaggable("https://api.example.com/v3"))
                }
            }
        }

        // Version 7.x should use v1
        assertEquals(
            "https://api.example.com/v1",
            ctx("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb01", version = "7.5.0").evaluate(StringFeatureFlags.API_ENDPOINT)?.value
        )

        // Version 8.x should use v2
        assertEquals(
            "https://api.example.com/v2",
            ctx("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb02", version = "8.2.0").evaluate(StringFeatureFlags.API_ENDPOINT)?.value
        )

        // Version 9.x should use v3
        assertEquals(
            "https://api.example.com/v3",
            ctx("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb03", version = "9.1.0").evaluate(StringFeatureFlags.API_ENDPOINT)?.value
        )
    }

    @Test
    fun string_flag_with_coverage_rollout() {
        config {
            StringFeatureFlags.API_ENDPOINT withRules {
                default(StringFlaggable("https://api-old.example.com"))
                rule {
                    // 30% of users get the new endpoint
                    value(StringFlaggable("https://api-new.example.com"), coveragePct = 30.0)
                }
            }
        }

        // Sample many users to verify coverage distribution
        val N = 5000
        var newEndpointCount = 0
        var oldEndpointCount = 0

        for (i in 0 until N) {
            val id = "%032x".format(i)
            val result = ctx(id).evaluate(StringFeatureFlags.API_ENDPOINT)?.value
            when (result) {
                "https://api-new.example.com" -> newEndpointCount++
                "https://api-old.example.com" -> oldEndpointCount++
            }
        }

        // Should have roughly 30% new, 70% old (with some tolerance)
        val newPct = newEndpointCount.toDouble() / N
        assertTrue(newPct in 0.27..0.33, "Expected ~30% new endpoint, got ${newPct * 100}%")

        val oldPct = oldEndpointCount.toDouble() / N
        assertTrue(oldPct in 0.67..0.73, "Expected ~70% old endpoint, got ${oldPct * 100}%")
    }

    @Test
    fun string_flag_with_fallback_coverage() {
        config {
            StringFeatureFlags.THEME withRules {
                // 50% get "blue" theme, 50% get "green" fallback
                default(
                    value = StringFlaggable("blue"),
                    fallback = StringFlaggable("green"),
                    coverage = 50.0
                )
            }
        }

        // Sample many users to verify 50/50 split
        val N = 5000
        var blueCount = 0
        var greenCount = 0

        for (i in 0 until N) {
            val id = "%032x".format(i)
            val result = ctx(id).evaluate(StringFeatureFlags.THEME)?.value
            when (result) {
                "blue" -> blueCount++
                "green" -> greenCount++
            }
        }

        val bluePct = blueCount.toDouble() / N
        val greenPct = greenCount.toDouble() / N

        assertTrue(bluePct in 0.47..0.53, "Expected ~50% blue, got ${bluePct * 100}%")
        assertTrue(greenPct in 0.47..0.53, "Expected ~50% green, got ${greenPct * 100}%")
    }

    @Test
    fun string_flag_with_complex_targeting() {
        config {
            StringFeatureFlags.API_ENDPOINT withRules {
                default(StringFlaggable("https://api.example.com/stable"))

                // Beta API for iOS 9.0+ users at 25% rollout
                rule {
                    platforms(Platform.IOS)
                    versions {
                        atLeast(9, 0)
                    }
                    value(StringFlaggable("https://api.example.com/beta"), coveragePct = 25.0)
                }

                // Canary API for all Android 10.0+ users
                rule {
                    platforms(Platform.ANDROID)
                    versions {
                        atLeast(10, 0)
                    }
                    value(StringFlaggable("https://api.example.com/canary"), coveragePct = 100.0)
                }
            }
        }

        // iOS 8.x users should get stable
        assertEquals(
            "https://api.example.com/stable",
            ctx("cccccccccccccccccccccccccccccc01", platform = Platform.IOS, version = "8.5.0").evaluate(StringFeatureFlags.API_ENDPOINT)?.value
        )

        // Android 10.0+ users should always get canary (100% coverage)
        assertEquals(
            "https://api.example.com/canary",
            ctx("cccccccccccccccccccccccccccccc02", platform = Platform.ANDROID, version = "10.0.0").evaluate(StringFeatureFlags.API_ENDPOINT)?.value
        )

        // iOS 9.0+ users get beta or stable based on 25% coverage
        // Test a large sample to verify distribution
        val N = 2000
        var betaCount = 0
        var stableCount = 0

        for (i in 0 until N) {
            val id = "%032x".format(i)
            val result = ctx(id, platform = Platform.IOS, version = "9.2.0")
                .evaluate(StringFeatureFlags.API_ENDPOINT)?.value
            when (result) {
                "https://api.example.com/beta" -> betaCount++
                "https://api.example.com/stable" -> stableCount++
            }
        }

        val betaPct = betaCount.toDouble() / N
        assertTrue(betaPct in 0.22..0.28, "Expected ~25% beta for iOS 9.0+, got ${betaPct * 100}%")
    }

    @Test
    fun string_flag_determinism_across_evaluations() {
        config {
            StringFeatureFlags.THEME withRules {
                default(StringFlaggable("light"))
                rule {
                    value(StringFlaggable("dark"), coveragePct = 50.0)
                }
            }
        }

        val id = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1"
        val firstResult = ctx(id).evaluate(StringFeatureFlags.THEME)?.value

        // Same user should always get the same result
        repeat(100) {
            val result = ctx(id).evaluate(StringFeatureFlags.THEME)?.value
            assertEquals(firstResult, result, "User should get consistent results")
        }
    }

    @Test
    fun string_flag_independence_across_different_flags() {
        config {
            StringFeatureFlags.THEME withRules {
                default(StringFlaggable("light"))
                rule {
                    value(StringFlaggable("dark"), coveragePct = 50.0)
                }
            }
            StringFeatureFlags.API_ENDPOINT withRules {
                default(StringFlaggable("https://api.example.com/v1"))
                rule {
                    value(StringFlaggable("https://api.example.com/v2"), coveragePct = 50.0)
                }
            }
        }

        val id = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"

        // Different flags can have different values for the same user (independent bucketing)
        val themeResult = ctx(id).evaluate(StringFeatureFlags.THEME)?.value
        val apiResult = ctx(id).evaluate(StringFeatureFlags.API_ENDPOINT)?.value

        // Just verify both return values (they can differ since bucketing is independent)
        assertTrue(themeResult in listOf("light", "dark"))
        assertTrue(apiResult in listOf("https://api.example.com/v1", "https://api.example.com/v2"))

        // But each should be deterministic for the same user
        repeat(10) {
            assertEquals(themeResult, ctx(id).evaluate(StringFeatureFlags.THEME)?.value)
            assertEquals(apiResult, ctx(id).evaluate(StringFeatureFlags.API_ENDPOINT)?.value)
        }
    }
}
