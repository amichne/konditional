package io.amichne.konditional.demo

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.core.Namespace

/**
 * Demo features showcasing different value types using [Namespace] property delegation.
 *
 * This demonstrates the modern pattern of using [Namespace] instead of enum-based features:
 * - Complete enumeration via allFeatures()
 * - Zero boilerplate (namespace declared once)
 * - Mixed types in one features
 * - Type-safe delegation with DSL configuration
 */
object DemoFeatures : Namespace("demo") {
    // Boolean Features
    val DARK_MODE by boolean<Context>(false) {
        rule(true) {
            platforms(Platform.IOS, Platform.ANDROID)
            rampUp { 50.0 }
        }
        rule(true) {
            platforms(Platform.WEB)
            rampUp { 75.0 }
        }
    }

    val BETA_FEATURES by boolean<Context>(false) {
        rule(true) {
            versions {
                max(3)
                min(2)
            }
            rampUp { 25.0 }
        }
        rule(true) {
            versions { max(3) }
            rampUp { 100.0 }
        }
    }
    val ANALYTICS_ENABLED by boolean<Context>(true) { }

    // String Features
    val WELCOME_MESSAGE by string<Context>("Welcome!") {
        rule("Welcome to Konditional Demo!") {
            locales(AppLocale.UNITED_STATES, AppLocale.CANADA)
        }
        rule("Bienvenue dans Konditional Demo!") {
            locales(AppLocale.UNITED_STATES)
        }
    }
    val THEME_COLOR by string<Context>("#3B82F6") {
        rule("#10B981") {
            platforms(Platform.IOS)
            rampUp { 50.0 }
        }
        rule("#8B5CF6") {
            platforms(Platform.ANDROID)
        }
        rule("#F59E0B") {
            platforms(Platform.WEB)
        }
    }

    // Integer Features
    val MAX_ITEMS_PER_PAGE by integer<Context>(10) {
        rule(25) {
            platforms(Platform.WEB)
        }
        rule(15) {
            platforms(Platform.IOS, Platform.ANDROID)
        }
    }

    val CACHE_TTL_SECONDS by integer<Context>(300) {
        rule(600) {
            versions { min(2) }
        }
        rule(900) {
            platforms(Platform.WEB)
        }
    }

    // Double Features
    val DISCOUNT_PERCENTAGE by double<Context>(0.0) {
        rule(10.0) {
            platforms(Platform.IOS)
            rampUp { 30.0 }
        }
        rule(15.0) {
            platforms(Platform.ANDROID)
            rampUp { 20.0 }
        }
        rule(20.0) {
            versions { min(2, 5) }
            rampUp { 50.0 }
        }
    }
    val API_RATE_LIMIT by double<Context>(100.0) {
        rule(200.0) {
            platforms(Platform.WEB)
        }
        rule(500.0) {
            versions { min(3) }
        }
    }

    // Enterprise Boolean Features
    val SSO_ENABLED by boolean<EnterpriseContext>(true) {
        rule(true) {
            extension {
                subscriptionTier == SubscriptionTier.ENTERPRISE ||
                    subscriptionTier == SubscriptionTier.PROFESSIONAL
            }
        }
    }
    val ADVANCED_ANALYTICS by boolean<EnterpriseContext>(false) {
        rule(true) {
            extension {
                subscriptionTier == SubscriptionTier.ENTERPRISE &&
                    employeeCount > 100
            }
        }
    }
    val CUSTOM_BRANDING by boolean<EnterpriseContext>(true) { }
    val DEDICATED_SUPPORT by boolean<EnterpriseContext>(false) { }
}
