package io.amichne.konditional.example

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Feature
import io.amichne.konditional.core.Feature.Companion.jsonObject
import io.amichne.konditional.core.FeatureModule
import io.amichne.konditional.core.config
import io.amichne.konditional.core.id.StableId

/**
 * Example demonstrating JSON object support for HSON-object type representation.
 *
 * JSON-object: Distinct super type of object nodes that represent different
 * values given specific conditions.
 */
object JsonObjectExample {

    // ========== Domain Types ==========

    /**
     * API configuration - complex object with multiple fields.
     */
    data class ApiConfig(
        val baseUrl: String,
        val timeout: Int,
        val retries: Int,
        val useHttps: Boolean,
        val headers: Map<String, String> = emptyMap()
    )

    /**
     * Theme configuration - another complex object.
     */
    data class ThemeConfig(
        val primaryColor: String,
        val secondaryColor: String,
        val fontFamily: String,
        val fontSize: Int,
        val darkModeEnabled: Boolean
    )

    /**
     * Feature set configuration - shows nested objects.
     */
    data class FeatureSet(
        val features: List<String>,
        val limits: Map<String, Int>,
        val metadata: Map<String, Any?>
    )

    // ========== Conditional Declarations ==========

    /**
     * API configuration conditional.
     * Different API configs for different platforms/environments.
     */
    val API_CONFIG: Feature.OfJsonObject<ApiConfig, Context, FeatureModule.Team.Recommendations> =
        jsonObject("api_config", FeatureModule.Team.Recommendations)

    /**
     * Theme configuration conditional.
     * Different themes per platform.
     */
    val THEME: Feature.OfJsonObject<ThemeConfig, Context, FeatureModule.Team.Recommendations> =
        jsonObject("app_theme", FeatureModule.Team.Recommendations)

    /**
     * Feature set conditional.
     * Different feature sets based on context.
     */
    val FEATURES: Feature.OfJsonObject<FeatureSet, Context, FeatureModule.Team.Recommendations> =
        jsonObject("feature_set", FeatureModule.Team.Recommendations)

    // ========== Usage Example ==========

    fun demonstrateUsage() {
        // Production API config
        val prodApi = ApiConfig(
            baseUrl = "https://api.prod.example.com",
            timeout = 30,
            retries = 3,
            useHttps = true,
            headers = mapOf("X-Environment" to "production")
        )

        // Development API config
        val devApi = ApiConfig(
            baseUrl = "https://api.dev.example.com",
            timeout = 60,
            retries = 1,
            useHttps = true,
            headers = mapOf("X-Environment" to "development")
        )

        // Light theme
        val lightTheme = ThemeConfig(
            primaryColor = "#FFFFFF",
            secondaryColor = "#F0F0F0",
            fontFamily = "Roboto",
            fontSize = 14,
            darkModeEnabled = false
        )

        // Dark theme
        val darkTheme = ThemeConfig(
            primaryColor = "#1E1E1E",
            secondaryColor = "#2D2D2D",
            fontFamily = "Roboto",
            fontSize = 14,
            darkModeEnabled = true
        )

        // Basic feature set
        val basicFeatures = FeatureSet(
            features = listOf("core", "basic"),
            limits = mapOf("maxUsers" to 10, "maxStorage" to 100),
            metadata = mapOf("tier" to "free")
        )

        // Premium feature set
        val premiumFeatures = FeatureSet(
            features = listOf("core", "basic", "advanced", "analytics"),
            limits = mapOf("maxUsers" to 1000, "maxStorage" to 10000),
            metadata = mapOf("tier" to "premium", "priority" to true)
        )

        // Configure with HSON-object type representation
        // Each condition produces a distinct object node
        FeatureModule.Team.Recommendations.config {
            API_CONFIG with {
                default(prodApi)

                // Different API config for WEB platform
                rule {
                    platforms(Platform.WEB)
                } implies devApi
            }

            THEME with {
                default(lightTheme)

                // Dark theme for specific locales
                rule {
                    locales(AppLocale.EN_US)
                } implies darkTheme
            }

            FEATURES with {
                default(basicFeatures)

                // Premium features for iOS v2.0+
                rule {
                    platforms(Platform.IOS)
                    versions {
                        min(2, 0)
                    }
                } implies premiumFeatures
            }
        }

        // Evaluate
        Context(
            locale = AppLocale.EN_US,
            platform = Platform.IOS,
            appVersion = Version(2, 5, 0),
            stableId = StableId.of("11111111111111111111111111111111")
        )

        Context(
            locale = AppLocale.EN_CA,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("22222222222222222222222222222222")
        )

        // In practice, evaluation would return the domain objects
        // This demonstrates HSON-object type representation:
        // - Same conditional produces different object structures based on rules
        // - Each rule "implies" a distinct object node
        // - Type-safe throughout the evaluation chain

        println("JSON object example configured successfully")
        println("iOS context would get: prod API, dark theme, premium features")
        println("Web context would get: dev API, light theme, basic features")
    }
}
