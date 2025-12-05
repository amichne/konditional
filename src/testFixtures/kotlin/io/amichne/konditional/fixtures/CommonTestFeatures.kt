package io.amichne.konditional.fixtures

import io.amichne.konditional.fixtures.core.TestKontext
import io.amichne.konditional.fixtures.core.TestNamespace
import io.amichne.konditional.kontext.DoublyAware
import io.amichne.konditional.kontext.Konstrained

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
abstract class CommonTestFeatures : DoublyAware<TestKontext, TestNamespace> {
    // Boolean features
    /** General-purpose test feature flag */
    val testFeature by boolean(default = false)

    /** Always-enabled feature for testing true state */
    val alwaysTrue by boolean(default = true)

    /** Feature flag for testing enabled state */
    val enabledFeature by boolean(default = true)

    /** Feature flag for testing disabled state */
    val disabledFeature by boolean(default = false)

    /** Feature flag for testing rampUp percentages */
    val rolloutFeature by boolean(default = false)

    /** Feature flag for testing platform targeting */
    val platformFeature by boolean(default = false)

    /** Feature flag for testing locale targeting */
    val localeFeature by boolean(default = false)

    /** Feature flag for testing version ranges */
    val versionFeature by boolean(default = false)

    // String features
    /** API endpoint configuration */
    val apiEndpoint by string(default = "https://api.example.com")

    /** Theme configuration */
    val theme by string(default = "light")

    /** Welcome message configuration */
    val welcomeMessage by string(default = "Welcome!")

    /** General-purpose test string flag */
    val testString by string(default = "default")

    /** Registered flag for testing found scenarios */
    val registeredFlag by string(default = "test")

    /** Unregistered flag for testing not-found scenarios */
    val unregisteredFlag by string(default = "")

    // Integer features
    /** Maximum connections configuration */
    val maxConnections by integer(default = 100)

    /** Timeout configuration in milliseconds */
    val timeout by integer(default = 5000)

    /** Retry count configuration */
    val retryCount by integer(default = 3)

    // Double features
    /** Threshold configuration */
    val threshold by double(default = 0.5)

    /** Rate limit configuration */
    val rateLimit by double(default = 100.0)
}

data class Sample : CommonTestFeatures() {
    override fun factory(): TestKontext = Konstrained.kontext()

}
