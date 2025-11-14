package io.amichne.konditional.fixtures

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.BooleanFeature
import io.amichne.konditional.core.FeatureContainer
import io.amichne.konditional.core.IntFeature
import io.amichne.konditional.core.StringFeature
import io.amichne.konditional.core.Taxonomy

/**
 * Common test features for basic types (Boolean, String, Int, Double).
 *
 * These features use the Core module and are available for use across all tests.
 * They provide a standardized set of feature flags for testing common scenarios.
 */

object CommonTestFeatures : FeatureContainer<Taxonomy.Core>(Taxonomy.Core) {
    // This object can be used to group common test features if needed

    val ALWAYS_TRUE by boolean<Context> {

    }
}

/**
 * Boolean feature flags for testing.
 *
 * Use these for tests involving boolean feature toggles.
 */
enum class TestBooleanFeatures(
    override val key: String,
) : BooleanFeature<Context, Taxonomy.Core> {
    /** General-purpose test feature flag */
    TEST_FEATURE("test_feature"),

    /** Feature flag for testing enabled state */
    ENABLED_FEATURE("enabled_feature"),

    /** Feature flag for testing disabled state */
    DISABLED_FEATURE("disabled_feature"),

    /** Feature flag for testing rollout percentages */
    ROLLOUT_FEATURE("rollout_feature"),

    /** Feature flag for testing platform targeting */
    PLATFORM_FEATURE("platform_feature"),

    /** Feature flag for testing locale targeting */
    LOCALE_FEATURE("locale_feature"),

    /** Feature flag for testing version ranges */
    VERSION_FEATURE("version_feature");

    override val module: Taxonomy.Core = Taxonomy.Core
}

/**
 * String feature flags for testing.
 *
 * Use these for tests involving string configuration values.
 */
enum class TestStringFeatures(
    override val key: String,
) : StringFeature<Context, Taxonomy.Core> {
    /** API endpoint configuration */
    API_ENDPOINT("api_endpoint"),

    /** Theme configuration */
    THEME("theme"),

    /** Welcome message configuration */
    WELCOME_MESSAGE("welcome_message"),

    /** General-purpose test string flag */
    TEST_STRING("test_string"),

    /** Registered flag for testing found scenarios */
    REGISTERED_FLAG("registered_flag"),

    /** Unregistered flag for testing not-found scenarios */
    UNREGISTERED_FLAG("unregistered_flag");

    override val module: Taxonomy.Core = Taxonomy.Core
}

/**
 * Integer feature flags for testing.
 *
 * Use these for tests involving numeric configuration values.
 */
enum class TestIntFeatures(
    override val key: String,
) : IntFeature<Context, Taxonomy.Core> {
    /** Maximum connections configuration */
    MAX_CONNECTIONS("max_connections"),

    /** Timeout configuration */
    TIMEOUT("timeout"),

    /** Retry count configuration */
    RETRY_COUNT("retry_count");

    override val module: Taxonomy.Core = Taxonomy.Core
}

/**
 * Double feature flags for testing.
 *
 * Use these for tests involving decimal configuration values.
 */
data object TestDoubleFeatures : FeatureContainer<Taxonomy.Core>(Taxonomy.Core) {
    /** Threshold configuration */
    val  THRESHOLD by double("threshold"){

    }

    /** Rate limit configuration */
    RATE_LIMIT("rate_limit");

    override val module: Taxonomy.Core = Taxonomy.Core
}
