package io.amichne.konditional.fixtures

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.Taxonomy

/**
 * Test features for validating extensibility with custom value types.
 *
 * These demonstrate JSON objects, custom wrappers, and complex data structures.
 */

// ========== Data Classes for JSON Object Features ==========

/** API configuration data class */
data class ApiConfig(
    val baseUrl: String,
    val timeout: Int,
    val retries: Int,
    val useHttps: Boolean,
)

/** Theme configuration data class */
data class ThemeConfig(
    val primaryColor: String,
    val secondaryColor: String,
    val fontFamily: String,
    val darkModeEnabled: Boolean,
)

// ========== JSON Object Feature Flags ==========

/**
 * JSON object feature flags for testing complex data structures.
 */
object TestJsonEncodeableFeatureFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    private val defaultApiConfig = ApiConfig("", 0, 0, false)

    /** Primary API configuration */
    val primary_api by jsonObject<Context, ApiConfig>(defaultApiConfig, "primary_api")
}

object TestThemeFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    private val defaultThemeConfig = ThemeConfig("", "", "", false)

    /** Application theme configuration */
    val app_theme by jsonObject<Context, ThemeConfig>(defaultThemeConfig, "app_theme")
}

object TestListFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    private val defaultList = emptyList<String>()

    /** Enabled features list */
    val enabled_features by jsonObject<Context, List<String>>(defaultList, "enabled_features")
}

object TestMapFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    private val defaultMap = emptyMap<String, String>()

    /** Feature toggles map */
    val feature_toggles by jsonObject<Context, Map<String, String>>(defaultMap, "feature_toggles")
}

// ========== Custom Wrapper Type ==========

/** Log level enum for custom wrapper testing */
enum class LogLevel {
    DEBUG, INFO, ERROR
}

/** Custom wrapper feature flags */
object TestCustomWrapperFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    /** Application log level */
//    val app_log_level by custom<LogLevel, String, Context>("app_log_level")
}
