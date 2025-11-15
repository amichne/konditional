package io.amichne.konditional.demo

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.*

/**
 * Demo features showcasing different value types
 */
enum class DemoFeatures(override val key: String) : Feature<*, *, Context, FeatureModule.Core> {
    // Boolean features
    DARK_MODE("dark_mode"),
    BETA_FEATURES("beta_features"),
    ANALYTICS_ENABLED("analytics_enabled"),

    // String features
    WELCOME_MESSAGE("welcome_message"),
    THEME_COLOR("theme_color"),

    // Integer features
    MAX_ITEMS_PER_PAGE("max_items_per_page"),
    CACHE_TTL_SECONDS("cache_ttl_seconds"),

    // Double features
    DISCOUNT_PERCENTAGE("discount_percentage"),
    API_RATE_LIMIT("api_rate_limit");

    override val module: FeatureModule.Core
        get() = FeatureModule.Core
}

// Type-safe feature references for use in code
object Features {
    val DARK_MODE: BooleanFeature<DemoContext, FeatureModule.Core> =
        DemoFeatures.DARK_MODE as BooleanFeature<DemoContext, FeatureModule.Core>

    val BETA_FEATURES: BooleanFeature<DemoContext, FeatureModule.Core> =
        DemoFeatures.BETA_FEATURES as BooleanFeature<DemoContext, FeatureModule.Core>

    val ANALYTICS_ENABLED: BooleanFeature<DemoContext, FeatureModule.Core> =
        DemoFeatures.ANALYTICS_ENABLED as BooleanFeature<DemoContext, FeatureModule.Core>

    val WELCOME_MESSAGE: StringFeature<DemoContext, FeatureModule.Core> =
        DemoFeatures.WELCOME_MESSAGE as StringFeature<DemoContext, FeatureModule.Core>

    val THEME_COLOR: StringFeature<DemoContext, FeatureModule.Core> =
        DemoFeatures.THEME_COLOR as StringFeature<DemoContext, FeatureModule.Core>

    val MAX_ITEMS_PER_PAGE: IntFeature<DemoContext, FeatureModule.Core> =
        DemoFeatures.MAX_ITEMS_PER_PAGE as IntFeature<DemoContext, FeatureModule.Core>

    val CACHE_TTL_SECONDS: IntFeature<DemoContext, FeatureModule.Core> =
        DemoFeatures.CACHE_TTL_SECONDS as IntFeature<DemoContext, FeatureModule.Core>

    val DISCOUNT_PERCENTAGE: DoubleFeature<DemoContext, FeatureModule.Core> =
        DemoFeatures.DISCOUNT_PERCENTAGE as DoubleFeature<DemoContext, FeatureModule.Core>

    val API_RATE_LIMIT: DoubleFeature<DemoContext, FeatureModule.Core> =
        DemoFeatures.API_RATE_LIMIT as DoubleFeature<DemoContext, FeatureModule.Core>
}

/**
 * Enterprise-specific features that require the extended context
 */
enum class EnterpriseFeatures(override val key: String) : Feature<*, *, EnterpriseContext, FeatureModule.Core> {
    SSO_ENABLED("sso_enabled"),
    ADVANCED_ANALYTICS("advanced_analytics"),
    CUSTOM_BRANDING("custom_branding"),
    DEDICATED_SUPPORT("dedicated_support");

    override val module: FeatureModule.Core
        get() = FeatureModule.Core
}

// Type-safe enterprise feature references
object EnterpriseFeatureRefs {
    val SSO_ENABLED: BooleanFeature<EnterpriseContext, FeatureModule.Core> =
        EnterpriseFeatures.SSO_ENABLED as BooleanFeature<EnterpriseContext, FeatureModule.Core>

    val ADVANCED_ANALYTICS: BooleanFeature<EnterpriseContext, FeatureModule.Core> =
        EnterpriseFeatures.ADVANCED_ANALYTICS as BooleanFeature<EnterpriseContext, FeatureModule.Core>

    val CUSTOM_BRANDING: BooleanFeature<EnterpriseContext, FeatureModule.Core> =
        EnterpriseFeatures.CUSTOM_BRANDING as BooleanFeature<EnterpriseContext, FeatureModule.Core>

    val DEDICATED_SUPPORT: BooleanFeature<EnterpriseContext, FeatureModule.Core> =
        EnterpriseFeatures.DEDICATED_SUPPORT as BooleanFeature<EnterpriseContext, FeatureModule.Core>
}
