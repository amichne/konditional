package io.amichne.konditional.fixtures

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Feature
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
enum class TestJsonObjectFeatures(
    override val key: String,
) : Feature.OfJsonObject<ApiConfig, Context, Taxonomy.Core> {
    /** Primary API configuration */
    PRIMARY_API("primary_api");

    override val module: Taxonomy.Core = Taxonomy.Core
}

enum class TestThemeFeatures(
    override val key: String,
) : Feature.OfJsonObject<ThemeConfig, Context, Taxonomy.Core> {
    /** Application theme configuration */
    APP_THEME("app_theme");

    override val module: Taxonomy.Core = Taxonomy.Core
}

enum class TestListFeatures(
    override val key: String,
) : Feature.OfJsonObject<List<String>, Context, Taxonomy.Core> {
    /** Enabled features list */
    ENABLED_FEATURES("enabled_features");

    override val module: Taxonomy.Core = Taxonomy.Core
}

enum class TestMapFeatures(
    override val key: String,
) : Feature.OfJsonObject<Map<String, String>, Context, Taxonomy.Core> {
    /** Feature toggles map */
    FEATURE_TOGGLES("feature_toggles");

    override val module: Taxonomy.Core = Taxonomy.Core
}

// ========== Custom Wrapper Type ==========

/** Log level enum for custom wrapper testing */
enum class LogLevel {
    DEBUG, INFO, ERROR
}

/** Custom wrapper feature flags */
enum class TestCustomWrapperFeatures(
    override val key: String,
) : Feature.OfCustom<LogLevel, String, Context, Taxonomy.Core> {
    /** Application log level */
    APP_LOG_LEVEL("app_log_level");

    override val module: Taxonomy.Core = Taxonomy.Core
}
