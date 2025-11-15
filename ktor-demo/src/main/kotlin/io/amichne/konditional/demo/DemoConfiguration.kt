package io.amichne.konditional.demo

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.config
import io.amichne.konditional.rules.versions.Version

/**
 * Initializes the demo configuration with example rules and rollouts
 */
fun initializeDemoConfig() {
    Taxonomy.Global.config {
        // Boolean Features
        DemoFeatures.DARK_MODE with {
            default(false)
            rule {
                platforms(Platform.IOS, Platform.ANDROID)
                rollout = Rollout.of(50.0)
            } implies true
            rule {
                platforms(Platform.WEB)
                rollout = Rollout.of(75.0)
            } implies true
        }

        DemoFeatures.BETA_FEATURES with {
            default(false)
            rule {
                appVersionRange = Version.parse("2.0.0")..Version.parse("3.0.0")
                rollout = Rollout.of(25.0)
            } implies true
            rule {
                appVersionRange = Version.parse("3.0.0")..Version.unbounded()
                rollout = Rollout.of(100.0)
            } implies true
        }

        DemoFeatures.ANALYTICS_ENABLED with {
            default(true)
            rule {
                locales(AppLocale.EN_US, AppLocale.EN_GB)
            } implies true
            rule {
                locales(AppLocale.FR_FR, AppLocale.DE_DE)
                rollout = Rollout.of(50.0)
            } implies false
        }

        // String Features
        DemoFeatures.WELCOME_MESSAGE with {
            default("Welcome!")
            rule {
                locales(AppLocale.EN_US, AppLocale.EN_GB)
            } implies "Welcome to Konditional Demo!"
            rule {
                locales(AppLocale.FR_FR)
            } implies "Bienvenue dans Konditional Demo!"
            rule {
                locales(AppLocale.DE_DE)
            } implies "Willkommen bei Konditional Demo!"
            rule {
                locales(AppLocale.ES_ES)
            } implies "¡Bienvenido a Konditional Demo!"
        }

        DemoFeatures.THEME_COLOR with {
            default("#3B82F6") // Blue
            rule {
                platforms(Platform.IOS)
                rollout = Rollout.of(50.0)
            } implies "#10B981" // Green
            rule {
                platforms(Platform.ANDROID)
            } implies "#8B5CF6" // Purple
            rule {
                platforms(Platform.WEB)
            } implies "#F59E0B" // Amber
        }

        // Integer Features
        DemoFeatures.MAX_ITEMS_PER_PAGE with {
            default(10)
            rule {
                platforms(Platform.DESKTOP)
            } implies 50
            rule {
                platforms(Platform.WEB)
            } implies 25
            rule {
                platforms(Platform.IOS, Platform.ANDROID)
            } implies 15
        }

        DemoFeatures.CACHE_TTL_SECONDS with {
            default(300) // 5 minutes
            rule {
                appVersionRange = Version.parse("2.0.0")..Version.unbounded()
            } implies 600 // 10 minutes for v2+
            rule {
                platforms(Platform.WEB)
            } implies 900 // 15 minutes for web
        }

        // Double Features
        DemoFeatures.DISCOUNT_PERCENTAGE with {
            default(0.0)
            rule {
                platforms(Platform.IOS)
                rollout = Rollout.of(30.0)
            } implies 10.0
            rule {
                platforms(Platform.ANDROID)
                rollout = Rollout.of(20.0)
            } implies 15.0
            rule {
                appVersionRange = Version.parse("2.5.0")..Version.unbounded()
                rollout = Rollout.of(50.0)
            } implies 20.0
        }

        DemoFeatures.API_RATE_LIMIT with {
            default(100.0)
            rule {
                platforms(Platform.WEB)
            } implies 200.0
            rule {
                appVersionRange = Version.parse("3.0.0")..Version.unbounded()
            } implies 500.0
        }
    }
}

/**
 * Initializes the enterprise configuration with enterprise-specific features
 */
fun initializeEnterpriseConfig() {
    Taxonomy.Global.config {
        EnterpriseFeatures.SSO_ENABLED with {
            default(false)
            rule {
                custom<EnterpriseContext> { ctx ->
                    ctx.subscriptionTier == SubscriptionTier.ENTERPRISE ||
                        ctx.subscriptionTier == SubscriptionTier.PROFESSIONAL
                }
            } implies true
        }

        EnterpriseFeatures.ADVANCED_ANALYTICS with {
            default(false)
            rule {
                custom<EnterpriseContext> { ctx ->
                    ctx.subscriptionTier == SubscriptionTier.ENTERPRISE &&
                        ctx.employeeCount > 100
                }
            } implies true
            rule {
                custom<EnterpriseContext> { ctx ->
                    ctx.subscriptionTier == SubscriptionTier.PROFESSIONAL
                }
                rollout = Rollout.of(50.0)
            } implies true
        }

        EnterpriseFeatures.CUSTOM_BRANDING with {
            default(false)
            rule {
                custom<EnterpriseContext> { ctx ->
                    ctx.subscriptionTier == SubscriptionTier.ENTERPRISE
                }
            } implies true
        }

        EnterpriseFeatures.DEDICATED_SUPPORT with {
            default(false)
            rule {
                custom<EnterpriseContext> { ctx ->
                    ctx.subscriptionTier == SubscriptionTier.ENTERPRISE &&
                        ctx.employeeCount >= 50
                }
            } implies true
        }
    }
}
