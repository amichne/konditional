package io.amichne.konditional.fixtures

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FeatureContainer
import io.amichne.konditional.core.Taxonomy

/**
 * Common test features for basic types (Boolean, String, Int, Double).
 *
 * These features use the Core module and are available for use across all tests.
 * They provide a standardized set of feature flags for testing common scenarios.
 */

object CommonTestFeatures : FeatureContainer<Taxonomy.Core>(Taxonomy.Core) {
    // This object can be used to group common test features if needed

    val always_true by boolean<Context> { }
}

/**
 * Boolean feature flags for testing.
 *
 * Use these for tests involving boolean feature toggles.
 */
object TestBooleanFeatures : FeatureContainer<Taxonomy.Core>(Taxonomy.Core) {
    /** General-purpose test feature flag */
    val test_feature by boolean<Context> { }

    /** Feature flag for testing enabled state */
    val enabled_feature by boolean<Context> { }

    /** Feature flag for testing disabled state */
    val disabled_feature by boolean<Context> { }

    /** Feature flag for testing rollout percentages */
    val rollout_feature by boolean<Context> { }

    /** Feature flag for testing platform targeting */
    val platform_feature by boolean<Context> { }

    /** Feature flag for testing locale targeting */
    val locale_feature by boolean<Context> { }

    /** Feature flag for testing version ranges */
    val version_feature by boolean<Context> { }
}

/**
 * String feature flags for testing.
 *
 * Use these for tests involving string configuration values.
 */
object TestStringFeatures : FeatureContainer<Taxonomy.Core>(Taxonomy.Core) {
    /** API endpoint configuration */
    val api_endpoint by string<Context> { }

    /** Theme configuration */
    val theme by string<Context> { }

    /** Welcome message configuration */
    val welcome_message by string<Context> { }

    /** General-purpose test string flag */
    val test_string by string<Context> { }

    /** Registered flag for testing found scenarios */
    val registered_flag by string<Context> { }

    /** Unregistered flag for testing not-found scenarios */
    val unregistered_flag by string<Context> { }
}

/**
 * Integer feature flags for testing.
 *
 * Use these for tests involving numeric configuration values.
 */
object TestIntFeatures : FeatureContainer<Taxonomy.Core>(Taxonomy.Core) {
    /** Maximum connections configuration */
    val max_connections by int<Context> { }

    /** Timeout configuration */
    val timeout by int<Context> { }

    /** Retry count configuration */
    val retry_count by int<Context> { }
}

/**
 * Double feature flags for testing.
 *
 * Use these for tests involving decimal configuration values.
 */
object TestDoubleFeatures : FeatureContainer<Taxonomy.Core>(Taxonomy.Core) {
    /** Threshold configuration */
    val threshold by double<Context> { }

    /** Rate limit configuration */
    val rate_limit by double<Context> { }
}
