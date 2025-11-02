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

/**
 * Tests for Conditional extensibility with custom value types.
 * Validates that the feature flag system can work with any value type beyond Boolean and String.
 */
class ConditionalExtensibilityTest {

    private fun ctx(
        idHex: String,
        locale: AppLocale = AppLocale.EN_US,
        platform: Platform = Platform.IOS,
        version: String = "1.0.0",
    ) = Context(locale, platform, Version.parse(version), StableId.of(idHex))

    // Custom value type: API configuration
    data class ApiConfig(
        val baseUrl: String,
        val timeout: Int,
        val retries: Int,
        val useHttps: Boolean,
    )

    enum class ApiConfigFlags(
        override val key: String,
    ) : Conditional<ApiConfig, Context> {
        PRIMARY_API("primary_api"),
        BACKUP_API("backup_api"),
        ;

        override fun with(build: FlagBuilder<ApiConfig, Context>.() -> Unit) =
            update(FlagBuilder(this).apply(build).build())
    }

    // Custom value type: Theme configuration
    data class ThemeConfig(
        val primaryColor: String,
        val secondaryColor: String,
        val fontFamily: String,
        val darkModeEnabled: Boolean,
    )

    enum class ThemeFlags(
        override val key: String,
    ) : Conditional<ThemeConfig, Context> {
        APP_THEME("app_theme"),
        ;

        override fun with(build: FlagBuilder<ThemeConfig, Context>.() -> Unit) =
            update(FlagBuilder(this).apply(build).build())
    }

    // Custom value type: List of strings
    enum class ListFlags(
        override val key: String,
    ) : Conditional<List<String>, Context> {
        ENABLED_FEATURES("enabled_features"),
        BETA_MODULES("beta_modules"),
        ;

        override fun with(build: FlagBuilder<List<String>, Context>.() -> Unit) =
            update(FlagBuilder(this).apply(build).build())
    }

    // Custom value type: Integer
    enum class IntFlags(
        override val key: String,
    ) : Conditional<Int, Context> {
        MAX_CONNECTIONS("max_connections"),
        CACHE_SIZE_MB("cache_size_mb"),
        ;

        override fun with(build: FlagBuilder<Int, Context>.() -> Unit) =
            update(FlagBuilder(this).apply(build).build())
    }

    // Custom value type: Enum
    enum class LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    enum class LogConfigFlags(
        override val key: String,
    ) : Conditional<LogLevel, Context> {
        APP_LOG_LEVEL("app_log_level"),
        ;

        override fun with(build: FlagBuilder<LogLevel, Context>.() -> Unit) =
            update(FlagBuilder(this).apply(build).build())
    }

    // Custom value type: Map
    enum class MapFlags(
        override val key: String,
    ) : Conditional<Map<String, String>, Context> {
        FEATURE_TOGGLES("feature_toggles"),
        ;

        override fun with(build: FlagBuilder<Map<String, String>, Context>.() -> Unit) =
            update(FlagBuilder(this).apply(build).build())
    }

    @Test
    fun `Given custom data class value type, When evaluating, Then correct instance is returned`() {
        val prodConfig = ApiConfig(
            baseUrl = "https://api.prod.example.com",
            timeout = 30,
            retries = 3,
            useHttps = true,
        )

        val devConfig = ApiConfig(
            baseUrl = "http://api.dev.example.com",
            timeout = 60,
            retries = 1,
            useHttps = false,
        )

        config {
            ApiConfigFlags.PRIMARY_API with {
                default(prodConfig)
                rule {
                    platforms(Platform.WEB)
                } implies devConfig
            }
        }

        val iosResult = ctx("11111111111111111111111111111111", platform = Platform.IOS)
            .evaluate(ApiConfigFlags.PRIMARY_API)
        assertEquals(prodConfig, iosResult)

        val webResult = ctx("22222222222222222222222222222222", platform = Platform.WEB)
            .evaluate(ApiConfigFlags.PRIMARY_API)
        assertEquals(devConfig, webResult)
    }

    @Test
    fun `Given theme configuration value type, When evaluating, Then correct theme is returned`() {
        val lightTheme = ThemeConfig(
            primaryColor = "#FFFFFF",
            secondaryColor = "#F0F0F0",
            fontFamily = "Roboto",
            darkModeEnabled = false,
        )

        val darkTheme = ThemeConfig(
            primaryColor = "#1E1E1E",
            secondaryColor = "#2D2D2D",
            fontFamily = "Roboto",
            darkModeEnabled = true,
        )

        config {
            ThemeFlags.APP_THEME with {
                default(lightTheme)
                rule {
                    locales(AppLocale.EN_US)
                } implies darkTheme
            }
        }

        val result = ctx("33333333333333333333333333333333", locale = AppLocale.EN_US)
            .evaluate(ThemeFlags.APP_THEME)

        assertEquals(darkTheme, result)
        assertTrue(result.darkModeEnabled)
        assertEquals("#1E1E1E", result.primaryColor)
    }

    @Test
    fun `Given List value type, When evaluating, Then correct list is returned`() {
        val defaultFeatures = listOf("core", "basic")
        val premiumFeatures = listOf("core", "basic", "advanced", "analytics")

        config {
            ListFlags.ENABLED_FEATURES with {
                default(defaultFeatures)
                rule {
                    versions {
                        min(2, 0)
                    }
                } implies premiumFeatures
            }
        }

        val v1Result = ctx("44444444444444444444444444444444", version = "1.5.0")
            .evaluate(ListFlags.ENABLED_FEATURES)
        assertEquals(defaultFeatures, v1Result)
        assertEquals(2, v1Result.size)

        val v2Result = ctx("55555555555555555555555555555555", version = "2.5.0")
            .evaluate(ListFlags.ENABLED_FEATURES)
        assertEquals(premiumFeatures, v2Result)
        assertEquals(4, v2Result.size)
    }

    @Test
    fun `Given Int value type, When evaluating, Then correct integer is returned`() {
        config {
            IntFlags.MAX_CONNECTIONS with {
                default(10)
                rule {
                    platforms(Platform.WEB)
                    versions {
                        min(3, 0)
                    }
                } implies 100
                rule {
                    platforms(Platform.WEB)
                } implies 50
            }
        }

        val iosResult = ctx("66666666666666666666666666666666", platform = Platform.IOS)
            .evaluate(IntFlags.MAX_CONNECTIONS)
        assertEquals(10, iosResult)

        val webV2Result = ctx("77777777777777777777777777777777", platform = Platform.WEB, version = "2.0.0")
            .evaluate(IntFlags.MAX_CONNECTIONS)
        assertEquals(50, webV2Result)

        val webV3Result = ctx("88888888888888888888888888888888", platform = Platform.WEB, version = "3.0.0")
            .evaluate(IntFlags.MAX_CONNECTIONS)
        assertEquals(100, webV3Result)
    }

    @Test
    fun `Given Enum value type, When evaluating, Then correct enum is returned`() {
        config {
            LogConfigFlags.APP_LOG_LEVEL with {
                default(LogLevel.INFO)
                rule {
                    platforms(Platform.WEB)
                } implies LogLevel.DEBUG
                rule {
                    versions {
                        max(1, 0)
                    }
                } implies LogLevel.ERROR
            }
        }

        val defaultResult = ctx("99999999999999999999999999999999", platform = Platform.IOS, version = "2.0.0")
            .evaluate(LogConfigFlags.APP_LOG_LEVEL)
        assertEquals(LogLevel.INFO, defaultResult)

        val webResult = ctx("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", platform = Platform.WEB, version = "2.0.0")
            .evaluate(LogConfigFlags.APP_LOG_LEVEL)
        assertEquals(LogLevel.DEBUG, webResult)

        val oldVersionResult = ctx("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", platform = Platform.IOS, version = "0.9.0")
            .evaluate(LogConfigFlags.APP_LOG_LEVEL)
        assertEquals(LogLevel.ERROR, oldVersionResult)
    }

    @Test
    fun `Given Map value type, When evaluating, Then correct map is returned`() {
        val defaultToggles = mapOf(
            "feature1" to "off",
            "feature2" to "off",
        )

        val betaToggles = mapOf(
            "feature1" to "on",
            "feature2" to "on",
            "feature3" to "beta",
        )

        config {
            MapFlags.FEATURE_TOGGLES with {
                default(defaultToggles)
                rule {
                    locales(AppLocale.EN_US, AppLocale.EN_CA)
                } implies betaToggles
            }
        }

        val defaultResult = ctx("cccccccccccccccccccccccccccccccc", locale = AppLocale.ES_US)
            .evaluate(MapFlags.FEATURE_TOGGLES)
        assertEquals(defaultToggles, defaultResult)
        assertEquals(2, defaultResult.size)

        val betaResult = ctx("dddddddddddddddddddddddddddddddd", locale = AppLocale.EN_US)
            .evaluate(MapFlags.FEATURE_TOGGLES)
        assertEquals(betaToggles, betaResult)
        assertEquals(3, betaResult.size)
    }

    @Test
    fun `Given multiple value types, When evaluating all, Then each returns correct type`() {
        val apiConfig = ApiConfig("https://api.example.com", 30, 3, true)
        val theme = ThemeConfig("#FFFFFF", "#F0F0F0", "Arial", false)
        val features = listOf("core")

        config {
            ApiConfigFlags.PRIMARY_API with {
                default(apiConfig)
            }
            ThemeFlags.APP_THEME with {
                default(theme)
            }
            ListFlags.ENABLED_FEATURES with {
                default(features)
            }
            IntFlags.MAX_CONNECTIONS with {
                default(10)
            }
            LogConfigFlags.APP_LOG_LEVEL with {
                default(LogLevel.INFO)
            }
        }

        val context = ctx("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")

        val apiResult = context.evaluate(ApiConfigFlags.PRIMARY_API)
        val themeResult = context.evaluate(ThemeFlags.APP_THEME)
        val featuresResult = context.evaluate(ListFlags.ENABLED_FEATURES)
        val connectionsResult = context.evaluate(IntFlags.MAX_CONNECTIONS)
        val logLevelResult = context.evaluate(LogConfigFlags.APP_LOG_LEVEL)

        // Verify each type is correctly returned
        assertTrue(apiResult is ApiConfig)
        assertTrue(themeResult is ThemeConfig)
        assertTrue(featuresResult is List<*>)
        assertTrue(connectionsResult is Int)
        assertTrue(logLevelResult is LogLevel)

        assertEquals(apiConfig, apiResult)
        assertEquals(theme, themeResult)
        assertEquals(features, featuresResult)
        assertEquals(10, connectionsResult)
        assertEquals(LogLevel.INFO, logLevelResult)
    }

    @Test
    fun `Given complex nested data structure, When evaluating, Then structure is preserved`() {
        data class DeepConfig(
            val level1: String,
            val level2: Map<String, List<Int>>,
            val level3: ApiConfig,
        )

        data class DeepFlag(override val key: String = "nested_config") : Conditional<DeepConfig, Context> {
            override fun with(build: FlagBuilder<DeepConfig, Context>.() -> Unit) =
                update(FlagBuilder(this).apply(build).build())
        }

        val nestedConfigFlag = DeepFlag()

        val complexConfig = DeepConfig(
            level1 = "value",
            level2 = mapOf(
                "key1" to listOf(1, 2, 3),
                "key2" to listOf(4, 5, 6),
            ),
            level3 = ApiConfig("https://nested.example.com", 20, 2, true),
        )

        config {
            nestedConfigFlag with {
                default(complexConfig)
            }
        }

        val result = ctx("ffffffffffffffffffffffffffffffff").evaluate(nestedConfigFlag)

        assertEquals(complexConfig, result)
        assertEquals("value", result.level1)
        assertEquals(listOf(1, 2, 3), result.level2["key1"])
        assertEquals("https://nested.example.com", result.level3.baseUrl)
    }
}
