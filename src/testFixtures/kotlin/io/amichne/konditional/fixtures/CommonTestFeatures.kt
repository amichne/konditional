package io.amichne.konditional.fixtures

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FeatureContainer
import io.amichne.konditional.core.Taxonomy

/**
 * Common test features demonstrating mixed-type capabilities of FeatureContainer.
 *
 * This consolidated fixture showcases how FeatureContainer can hold Boolean, String,
 * Int, and Double features together in a single object, providing:
 * - Complete enumeration via [allFeatures]
 * - Ergonomic delegation for all feature types
 * - Type safety with full compile-time checking
 * - Single module declaration for the entire container
 *
 * All features use the Core taxonomy and are available across all tests.
 *
 * **Naming convention:**
 * - Feature properties use camelCase following Kotlin best practices
 * - Feature keys match property names automatically for consistency and discoverability
 */
object CommonTestFeatures : FeatureContainer<Taxonomy.Core>(Taxonomy.Core) {

    // Boolean features
    /** General-purpose test feature flag */
    val testFeature by boolean<Context> { }

    /** Always-enabled feature for testing true state */
    val alwaysTrue by boolean<Context> { }

    /** Feature flag for testing enabled state */
    val enabledFeature by boolean<Context> { }

    /** Feature flag for testing disabled state */
    val disabledFeature by boolean<Context> { }

    /** Feature flag for testing rollout percentages */
    val rolloutFeature by boolean<Context> { }

    /** Feature flag for testing platform targeting */
    val platformFeature by boolean<Context> { }

    /** Feature flag for testing locale targeting */
    val localeFeature by boolean<Context> { }

    /** Feature flag for testing version ranges */
    val versionFeature by boolean<Context> { }

    // String features
    /** API endpoint configuration */
    val apiEndpoint by string<Context> { }

    /** Theme configuration */
    val theme by string<Context> { }

    /** Welcome message configuration */
    val welcomeMessage by string<Context> { }

    /** General-purpose test string flag */
    val testString by string<Context> { }

    /** Registered flag for testing found scenarios */
    val registeredFlag by string<Context> { }

    /** Unregistered flag for testing not-found scenarios */
    val unregisteredFlag by string<Context> { }

    // Integer features
    /** Maximum connections configuration */
    val maxConnections by int<Context> { }

    /** Timeout configuration in milliseconds */
    val timeout by int<Context> { }

    /** Retry count configuration */
    val retryCount by int<Context> { }

    // Double features
    /** Threshold configuration */
    val threshold by double<Context> { }

    /** Rate limit configuration */
    val rateLimit by double<Context> { }
}
