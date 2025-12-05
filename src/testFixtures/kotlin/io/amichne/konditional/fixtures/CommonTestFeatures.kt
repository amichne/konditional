package io.amichne.konditional.fixtures

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.fixtures.CommonTestFeatures.allFeatures

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
 * All features use the Global namespace and are available across all tests.
 *
 * **Naming convention:**
 * - Feature properties use camelCase following Kotlin best practices
 * - Feature keys match property names automatically for consistency and discoverability
 */
object CommonTestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {

    // Boolean features
    /** General-purpose test feature flag */
    val testFeature by boolean<`Context<T : Namespace>`>(default = false)

    /** Always-enabled feature for testing true state */
    val alwaysTrue by boolean<`Context<T : Namespace>`>(default = true)

    /** Feature flag for testing enabled state */
    val enabledFeature by boolean<`Context<T : Namespace>`>(default = true)

    /** Feature flag for testing disabled state */
    val disabledFeature by boolean<`Context<T : Namespace>`>(default = false)

    /** Feature flag for testing rampUp percentages */
    val rolloutFeature by boolean<`Context<T : Namespace>`>(default = false)

    /** Feature flag for testing platform targeting */
    val platformFeature by boolean<`Context<T : Namespace>`>(default = false)

    /** Feature flag for testing locale targeting */
    val localeFeature by boolean<`Context<T : Namespace>`>(default = false)

    /** Feature flag for testing version ranges */
    val versionFeature by boolean<`Context<T : Namespace>`>(default = false)

    // String features
    /** API endpoint configuration */
    val apiEndpoint by string<`Context<T : Namespace>`>(default = "https://api.example.com")

    /** Theme configuration */
    val theme by string<`Context<T : Namespace>`>(default = "light")

    /** Welcome message configuration */
    val welcomeMessage by string<`Context<T : Namespace>`>(default = "Welcome!")

    /** General-purpose test string flag */
    val testString by string<Context>(default = "default")

    /** Registered flag for testing found scenarios */
    val registeredFlag by string<`Context<T : Namespace>`>(default = "test")

    /** Unregistered flag for testing not-found scenarios */
    val unregisteredFlag by string<`Context<T : Namespace>`>(default = "")

    // Integer features
    /** Maximum connections configuration */
    val maxConnections by integer<`Context<T : Namespace>`>(default = 100)

    /** Timeout configuration in milliseconds */
    val timeout by integer<`Context<T : Namespace>`>(default = 5000)

    /** Retry count configuration */
    val retryCount by integer<`Context<T : Namespace>`>(default = 3)

    // Double features
    /** Threshold configuration */
    val threshold by double<`Context<T : Namespace>`>(default = 0.5)

    /** Rate limit configuration */
    val rateLimit by double<Context>(default = 100.0)
}
