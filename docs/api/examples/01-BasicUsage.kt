package examples

import io.amichne.konditional.builders.ConfigBuilder
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.evaluate
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.FlagRegistry
import io.amichne.konditional.core.id.StableId

/**
 * Example 1: Basic Feature Flag Usage
 *
 * This example demonstrates the simplest usage pattern:
 * - Define a boolean feature flag
 * - Configure it with a default value
 * - Evaluate it in a context
 */
fun main() {
    // 1. Define a feature flag
    val ENABLE_NEW_UI: Conditional<Boolean, Context> =
        Conditional("enable_new_ui")

    // 2. Configure the flag
    ConfigBuilder.config {
        ENABLE_NEW_UI with {
            default(value = false)  // Default: feature is OFF
        }
    }

    // 3. Create an evaluation context
    val context = Context(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("user-123")
    )

    // 4. Evaluate the flag
    val isNewUIEnabled = context.evaluate(ENABLE_NEW_UI)

    println("New UI enabled: $isNewUIEnabled")  // Output: New UI enabled: false
}

/**
 * Example 2: Platform-Specific Feature
 *
 * Enable a feature only on specific platforms.
 */
fun platformSpecificFeature() {
    val IOS_EXCLUSIVE_FEATURE: Conditional<Boolean, Context> =
        Conditional("ios_exclusive")

    ConfigBuilder.config {
        IOS_EXCLUSIVE_FEATURE with {
            default(value = false)

            // Enable only on iOS
            rule {
                platforms(Platform.IOS)
            } implies true
        }
    }

    // iOS user
    val iosContext = Context(
        locale = AppLocale.EN_US,
        platform = Platform.IOS,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("user-123")
    )

    // Android user
    val androidContext = Context(
        locale = AppLocale.EN_US,
        platform = Platform.ANDROID,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("user-456")
    )

    println("iOS user sees feature: ${iosContext.evaluate(IOS_EXCLUSIVE_FEATURE)}")  // true
    println("Android user sees feature: ${androidContext.evaluate(IOS_EXCLUSIVE_FEATURE)}")  // false
}

/**
 * Example 3: Version-Based Rollout
 *
 * Enable a feature for users on a minimum version.
 */
fun versionBasedRollout() {
    val NEW_CHECKOUT: Conditional<Boolean, Context> =
        Conditional("new_checkout")

    ConfigBuilder.config {
        NEW_CHECKOUT with {
            default(value = false)

            // Enable for version 2.0.0 and above
            rule {
                versions {
                    min(Version(2, 0, 0))
                }
            } implies true
        }
    }

    val oldVersion = Context(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 5, 0),
        stableId = StableId.of("user-123")
    )

    val newVersion = Context(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(2, 1, 0),
        stableId = StableId.of("user-456")
    )

    println("v1.5.0 user: ${oldVersion.evaluate(NEW_CHECKOUT)}")  // false
    println("v2.1.0 user: ${newVersion.evaluate(NEW_CHECKOUT)}")  // true
}

/**
 * Example 4: Configuration Flag
 *
 * Use feature flags to configure behavior, not just toggle features.
 */
fun configurationFlag() {
    data class ApiConfig(
        val endpoint: String,
        val timeout: Int
    )

    val API_CONFIG: Conditional<ApiConfig, Context> =
        Conditional("api_config")

    ConfigBuilder.config {
        API_CONFIG with {
            default(
                value = ApiConfig(
                    endpoint = "https://api.example.com",
                    timeout = 5000
                )
            )
        }
    }

    val context = Context(
        locale = AppLocale.EN_US,
        platform = Platform.WEB,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("user-123")
    )

    val config = context.evaluate(API_CONFIG)
    println("API endpoint: ${config.endpoint}")
    println("Timeout: ${config.timeout}ms")
}

/**
 * Example 5: Multiple Rules with Specificity
 *
 * Define multiple rules - more specific rules are evaluated first.
 */
fun multipleRules() {
    val GREETING: Conditional<String, Context> =
        Conditional("greeting")

    ConfigBuilder.config {
        GREETING with {
            default(value = "Hello")

            // Least specific: all iOS users
            rule {
                platforms(Platform.IOS)
            } implies "Hey iOS user!"

            // More specific: iOS + EN_US
            rule {
                platforms(Platform.IOS)
                locales(AppLocale.EN_US)
            } implies "Hey American iOS user!"

            // Most specific: iOS + EN_US + v2+
            rule {
                platforms(Platform.IOS)
                locales(AppLocale.EN_US)
                versions { min(Version(2, 0, 0)) }
            } implies "Hey American iOS user on v2!"
        }
    }

    val context1 = Context(
        locale = AppLocale.EN_US,
        platform = Platform.IOS,
        appVersion = Version(2, 5, 0),
        stableId = StableId.of("user-1")
    )

    val context2 = Context(
        locale = AppLocale.EN_US,
        platform = Platform.IOS,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("user-2")
    )

    val context3 = Context(
        locale = AppLocale.HI_IN,
        platform = Platform.IOS,
        appVersion = Version(1, 0, 0),
        stableId = StableId.of("user-3")
    )

    println(context1.evaluate(GREETING))  // "Hey American iOS user on v2!"
    println(context2.evaluate(GREETING))  // "Hey American iOS user!"
    println(context3.evaluate(GREETING))  // "Hey iOS user!"
}
