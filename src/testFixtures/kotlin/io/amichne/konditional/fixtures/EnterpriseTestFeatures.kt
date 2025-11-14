package io.amichne.konditional.fixtures

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.BooleanFeature
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.StringFeature
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.rules.evaluable.Evaluable

/**
 * Test features for validating context polymorphism.
 *
 * These demonstrate custom context types and specialized evaluation rules.
 */

// ========== Custom Contexts ==========

/** Subscription tier for enterprise contexts */
enum class SubscriptionTier {
    BASIC, PREMIUM, ENTERPRISE
}

/** User role for enterprise contexts */
enum class UserRole {
    EDITOR, ADMIN, OWNER
}

/**
 * Enterprise context with additional business-specific properties.
 */
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val organizationId: String,
    val subscriptionTier: SubscriptionTier,
    val userRole: UserRole,
) : Context

/**
 * Experiment context for A/B testing scenarios.
 */
data class ExperimentContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val experimentGroups: Set<String>,
    val sessionId: String,
) : Context

// ========== Enterprise Feature Flags ==========

/**
 * Feature flags for enterprise contexts.
 */
enum class EnterpriseFeatures(
    override val key: String,
) : BooleanFeature<EnterpriseContext, Taxonomy.Core> {
    /** Advanced analytics feature */
    ADVANCED_ANALYTICS("advanced_analytics"),

    /** Custom branding feature */
    CUSTOM_BRANDING("custom_branding"),

    /** API access feature */
    API_ACCESS("api_access");

    override val module: Taxonomy.Core = Taxonomy.Core
}

/**
 * Feature flags for experiment contexts.
 */
enum class ExperimentFeatures(
    override val key: String,
) : StringFeature<ExperimentContext, Taxonomy.Core> {
    /** Homepage variant */
    HOMEPAGE_VARIANT("homepage_variant"),

    /** Onboarding style */
    ONBOARDING_STYLE("onboarding_style");

    override val module: Taxonomy.Core = Taxonomy.Core
}

// ========== Custom Rules ==========

/**
 * Custom rule for enterprise-specific evaluation logic.
 *
 * This demonstrates how to extend evaluation with business-specific constraints.
 */
data class EnterpriseRule(
    val requiredTier: SubscriptionTier? = null,
    val requiredRole: UserRole? = null,
) : Evaluable<EnterpriseContext>() {
    override fun matches(context: EnterpriseContext): Boolean =
        (requiredTier == null || context.subscriptionTier >= requiredTier) &&
            (requiredRole == null || context.userRole >= requiredRole)
}
