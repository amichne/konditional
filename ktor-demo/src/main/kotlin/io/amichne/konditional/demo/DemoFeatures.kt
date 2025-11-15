package io.amichne.konditional.demo

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.RegistryScope
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.features.FeatureContainer

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
    init {
        // Initialize with a fresh registry for the demo
        RegistryScope.setGlobal(io.amichne.konditional.core.ModuleRegistry.create())
    }

    // Boolean Features
    val DARK_MODE by boolean<Context> { }
    val BETA_FEATURES by boolean<Context> { }
    val ANALYTICS_ENABLED by boolean<Context> { }

    // String Features
    val WELCOME_MESSAGE by string<Context> { }
    val THEME_COLOR by string<Context> { }

    // Integer Features
    val MAX_ITEMS_PER_PAGE by int<Context> { }
    val CACHE_TTL_SECONDS by int<Context> { }

    // Double Features
    val DISCOUNT_PERCENTAGE by double<Context> { }
    val API_RATE_LIMIT by double<Context> { }
}

/**
 * Enterprise-specific features container demonstrating context extensibility.
 *
 * These features require the EnterpriseContext with additional fields like
 * subscription tier and employee count.
 */
object EnterpriseFeatures : FeatureContainer<Taxonomy.Global>(Taxonomy.Global) {
    // Enterprise Boolean Features
    val SSO_ENABLED by boolean<EnterpriseContext> { }
    val ADVANCED_ANALYTICS by boolean<EnterpriseContext> { }
    val CUSTOM_BRANDING by boolean<EnterpriseContext> { }
    val DEDICATED_SUPPORT by boolean<EnterpriseContext> { }
}
