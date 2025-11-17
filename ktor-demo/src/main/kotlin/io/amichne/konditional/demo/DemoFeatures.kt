package io.amichne.konditional.demo

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.rules.evaluable.Evaluable.Companion.factory

/**
 * Demo features container showcasing different value types using FeatureContainer delegation.
 *
 * This demonstrates the modern pattern of using FeatureContainer instead of enum-based features:
 * - Complete enumeration via allFeatures()
 * - Zero boilerplate (taxonomy declared once)
 * - Mixed types in one container
 * - Type-safe delegation with DSL configuration
 */
object DemoFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    // Boolean Features
    val DARK_MODE by boolean<Context>(false) {
        rule {
            platforms(Platform.IOS, Platform.ANDROID)
            rollout { 50.0 }
        } implies true
        rule {
            platforms(Platform.WEB)
            rollout { 75.0 }
        } implies true
    }

    val BETA_FEATURES by boolean<Context>(false) {
        default(false)
        rule {
            versions {
                max(3)
                min(2)
            }
            rollout { 25.0 }
        } implies true
        rule {
            versions { max(3) }
            rollout { 100.0 }
        } implies true
    }
    val ANALYTICS_ENABLED by boolean<Context>(true) { }

    // String Features
    val WELCOME_MESSAGE by string<Context>("Hello!") {
        default("Welcome!")
        rule {
            locales(AppLocale.EN_US, AppLocale.EN_CA)
        } implies "Welcome to Konditional Demo!"
        rule {
            locales(AppLocale.ES_US)
        } implies "Bienvenue dans Konditional Demo!"

    }
    val THEME_COLOR by string<Context>("blue") {
        default("#3B82F6") // Blue
        rule {
            platforms(Platform.IOS)
            rollout { 50.0 }
        } implies "#10B981" // Green
        rule {
            platforms(Platform.ANDROID)
        } implies "#8B5CF6" // Purple
        rule {
            platforms(Platform.WEB)
        } implies "#F59E0B" // Amber
    }

    // Integer Features
    val MAX_ITEMS_PER_PAGE by int<Context>(10) {
        default(10)
        rule {
            platforms(Platform.WEB)
        } implies 25
        rule {
            platforms(Platform.IOS, Platform.ANDROID)
        } implies 15
    }

    val CACHE_TTL_SECONDS by int<Context>(60) {
        default(300) // 5 minutes
        rule {
            versions { min(2) }
        } implies 600 // 10 minutes for v2+
        rule {
            platforms(Platform.WEB)
        } implies 900 // 15 minutes for web

    }

    // Double Features
    val DISCOUNT_PERCENTAGE by double<Context>(15.0) {
        default(0.0)
        rule {
            platforms(Platform.IOS)
            rollout { 30.0 }
        } implies 10.0
        rule {
            platforms(Platform.ANDROID)
            rollout { 20.0 }
        } implies 15.0
        rule {
            versions { min(2, 5) }
            rollout { 50.0 }
        } implies 20.0

    }
    val API_RATE_LIMIT by double<Context>(100.5) {
        default(100.0)
        rule {
            platforms(Platform.WEB)
        } implies 200.0
        rule {
            versions { min(3) }
        } implies 500.0

    }
}

/**
 * Enterprise-specific features container demonstrating context extensibility.
 *
 * These features require the EnterpriseContext with additional fields like
 * subscription tier and employee count.
 */
object EnterpriseFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    // Enterprise Boolean Features
    val SSO_ENABLED by boolean<EnterpriseContext>(true) {
        rule {
            extension {
                factory { ctx ->
                    ctx.subscriptionTier == SubscriptionTier.ENTERPRISE ||
                        ctx.subscriptionTier == SubscriptionTier.PROFESSIONAL
                }
            }
        } implies true

    }
    val ADVANCED_ANALYTICS by boolean<EnterpriseContext>(false) {
        rule {
            extension {
                factory { ctx ->
                    ctx.subscriptionTier == SubscriptionTier.ENTERPRISE &&
                        ctx.employeeCount > 100
                }
            }
        } implies true
    }
    val CUSTOM_BRANDING by boolean<EnterpriseContext>(true) { }
    val DEDICATED_SUPPORT by boolean<EnterpriseContext>(false) { }
}
