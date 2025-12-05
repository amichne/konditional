package io.amichne.konditional.demo

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.FeatureContainer

/**
 * Demo features features showcasing different value types using FeatureContainer delegation.
 *
 * This demonstrates the modern pattern of using FeatureContainer instead of enum-based features:
 * - Complete enumeration via allFeatures()
 * - Zero boilerplate (namespace declared once)
 * - Mixed types in one features
 * - Type-safe delegation with DSL configuration
 */
object DemoFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    // Boolean Features
    val DARK_MODE by boolean<`Context<T : Namespace>`>(false) {
        rule {
            platforms(Platform.IOS, Platform.ANDROID)
            rampUp { 50.0 }
        } returns true
        rule {
            platforms(Platform.IOS)
            rampUp { 75.0 }
        } returns true
    }

    val BETA_FEATURES by boolean<`Context<T : Namespace>`>(false) {
        default(false)
        rule {
            versions {
                max(3)
                min(2)
            }
            rampUp { 25.0 }
        } returns true
        rule {
            versions { max(3) }
            rampUp { 100.0 }
        } returns true
    }
    val ANALYTICS_ENABLED by boolean<`Context<T : Namespace>`>(true) { }

    // String Features
    val WELCOME_MESSAGE by string<`Context<T : Namespace>`>("Hello!") {
        default("Welcome!")
        rule {
            locales(AppLocale.UNITED_STATES, AppLocale.CANADA)
        } returns "Welcome to Konditional Demo!"
        rule {
            locales(AppLocale.UNITED_STATES)
        } returns "Bienvenue dans Konditional Demo!"

    }
    val THEME_COLOR by string<`Context<T : Namespace>`>("blue") {
        default("#3B82F6") // Blue
        rule {
            platforms(Platform.IOS)
            rampUp { 50.0 }
        } returns "#10B981" // Green
        rule {
            platforms(Platform.ANDROID)
        } returns "#8B5CF6" // Purple
        rule {
            platforms(Platform.IOS)
        } returns "#F59E0B" // Amber
    }

    // Integer Features
    val MAX_ITEMS_PER_PAGE by integer<`Context<T : Namespace>`>(10) {
        default(10)
        rule {
            platforms(Platform.IOS)
        } returns 25
        rule {
            platforms(Platform.IOS, Platform.ANDROID)
        } returns 15
    }

    val CACHE_TTL_SECONDS by integer<`Context<T : Namespace>`>(60) {
        default(300) // 5 minutes
        rule {
            versions { min(2) }
        } returns 600 // 10 minutes for v2+
        rule {
            platforms(Platform.IOS)
        } returns 900 // 15 minutes for web

    }

    // Double Features
    val DISCOUNT_PERCENTAGE by double<`Context<T : Namespace>`>(15.0) {
        default(0.0)
        rule {
            platforms(Platform.IOS)
            rampUp { 30.0 }
        } returns 10.0
        rule {
            platforms(Platform.ANDROID)
            rampUp { 20.0 }
        } returns 15.0
        rule {
            versions { min(2, 5) }
            rampUp { 50.0 }
        } returns 20.0

    }
    val API_RATE_LIMIT by double<`Context<T : Namespace>`>(100.5) {
        default(100.0)
        rule {
            platforms(Platform.IOS)
        } returns 200.0
        rule {
            versions { min(3) }
        } returns 500.0

    }
}

/**
 * Enterprise-specific features features demonstrating contextFn extensibility.
 *
 * These features require the EnterpriseContext with additional fields like
 * subscription tier and employee count.
 */
object EnterpriseFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    // Enterprise Boolean Features
    val SSO_ENABLED by boolean<EnterpriseContext>(true) {
        rule {
            extension {
                subscriptionTier == SubscriptionTier.ENTERPRISE ||
                subscriptionTier == SubscriptionTier.PROFESSIONAL
            }
        } returns true

    }
    val ADVANCED_ANALYTICS by boolean<EnterpriseContext>(false) {
        rule {
            extension {
                subscriptionTier == SubscriptionTier.ENTERPRISE &&
                employeeCount > 100
            }
        } returns true
    }
    val CUSTOM_BRANDING by boolean<EnterpriseContext>(true) { }
    val DEDICATED_SUPPORT by boolean<EnterpriseContext>(false) { }
}
