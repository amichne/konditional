package io.amichne.konditional.example

import io.amichne.konditional.builders.ConfigBuilder.Companion.config
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.evaluate
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.types.EncodableValue

/**
 * Example demonstrating JSON object support for HSON-object type representation.
 *
 * HSON-object: Distinct super type of object nodes that represent different
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
    ) {
        /**
         * Encode to Map for JSON serialization.
         */
        fun toMap(): Map<String, Any?> = mapOf(
            "baseUrl" to baseUrl,
            "timeout" to timeout,
            "retries" to retries,
            "useHttps" to useHttps,
            "headers" to headers
        )

        companion object {
            /**
             * Decode from Map after JSON deserialization.
             */
            fun fromMap(map: Map<String, Any?>): ApiConfig = ApiConfig(
                baseUrl = map["baseUrl"] as String,
                timeout = (map["timeout"] as Number).toInt(),
                retries = (map["retries"] as Number).toInt(),
                useHttps = map["useHttps"] as Boolean,
                headers = (map["headers"] as? Map<String, String>) ?: emptyMap()
            )
        }
    }

    /**
     * Theme configuration - another complex object.
     */
    data class ThemeConfig(
        val primaryColor: String,
        val secondaryColor: String,
        val fontFamily: String,
        val fontSize: Int,
        val darkModeEnabled: Boolean
    ) {
        fun toMap(): Map<String, Any?> = mapOf(
            "primaryColor" to primaryColor,
            "secondaryColor" to secondaryColor,
            "fontFamily" to fontFamily,
            "fontSize" to fontSize,
            "darkModeEnabled" to darkModeEnabled
        )

        companion object {
            fun fromMap(map: Map<String, Any?>): ThemeConfig = ThemeConfig(
                primaryColor = map["primaryColor"] as String,
                secondaryColor = map["secondaryColor"] as String,
                fontFamily = map["fontFamily"] as String,
                fontSize = (map["fontSize"] as Number).toInt(),
                darkModeEnabled = map["darkModeEnabled"] as Boolean
            )
        }
    }

    /**
     * Feature set configuration - shows nested objects.
     */
    data class FeatureSet(
        val features: List<String>,
        val limits: Map<String, Int>,
        val metadata: Map<String, Any?>
    ) {
        fun toMap(): Map<String, Any?> = mapOf(
            "features" to features,
            "limits" to limits,
            "metadata" to metadata
        )

        companion object {
            @Suppress("UNCHECKED_CAST")
            fun fromMap(map: Map<String, Any?>): FeatureSet = FeatureSet(
                features = map["features"] as List<String>,
                limits = (map["limits"] as Map<String, Number>).mapValues { it.value.toInt() },
                metadata = map["metadata"] as Map<String, Any?>
            )
        }
    }

    // ========== Conditional Declarations ==========

    /**
     * API configuration conditional.
     * Different API configs for different platforms/environments.
     */
    val API_CONFIG: Conditional.OfJsonObject<ApiConfig, Context> =
        Conditional.jsonObject("api_config")

    /**
     * Theme configuration conditional.
     * Different themes per platform.
     */
    val THEME: Conditional.OfJsonObject<ThemeConfig, Context> =
        Conditional.jsonObject("app_theme")

    /**
     * Feature set conditional.
     * Different feature sets based on context.
     */
    val FEATURES: Conditional.OfJsonObject<FeatureSet, Context> =
        Conditional.jsonObject("feature_set")

    // ========== Helper Functions ==========

    fun ApiConfig.toEncodable(): EncodableValue.JsonObjectEncodeable<ApiConfig> =
        EncodableValue.JsonObjectEncodeable.of(
            value = this,
            encoder = { it.toMap() },
            decoder = { ApiConfig.fromMap(it) }
        )

    fun ThemeConfig.toEncodable(): EncodableValue.JsonObjectEncodeable<ThemeConfig> =
        EncodableValue.JsonObjectEncodeable.of(
            value = this,
            encoder = { it.toMap() },
            decoder = { ThemeConfig.fromMap(it) }
        )

    fun FeatureSet.toEncodable(): EncodableValue.JsonObjectEncodeable<FeatureSet> =
        EncodableValue.JsonObjectEncodeable.of(
            value = this,
            encoder = { it.toMap() },
            decoder = { FeatureSet.fromMap(it) }
        )

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
        config {
            API_CONFIG with {
                default(prodApi.toEncodable().value)

                // Different API config for WEB platform
                rule {
                    platforms(Platform.WEB)
                } implies devApi.toEncodable().value
            }

            THEME with {
                default(lightTheme.toEncodable().value)

                // Dark theme for specific locales
                rule {
                    locales(AppLocale.EN_US)
                } implies darkTheme.toEncodable().value
            }

            FEATURES with {
                default(basicFeatures.toEncodable().value)

                // Premium features for iOS v2.0+
                rule {
                    platforms(Platform.IOS)
                    versions {
                        min(2, 0)
                    }
                } implies premiumFeatures.toEncodable().value
            }
        }

        // Evaluate
        val iosContext = Context(
            locale = AppLocale.EN_US,
            platform = Platform.IOS,
            appVersion = Version(2, 5, 0),
            stableId = StableId.of("11111111111111111111111111111111")
        )

        val webContext = Context(
            locale = AppLocale.EN_GB,
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
