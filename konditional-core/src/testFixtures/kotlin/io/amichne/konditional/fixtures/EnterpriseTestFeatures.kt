package io.amichne.konditional.fixtures

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.rules.targeting.Targeting

/**
 * TestNamespace features for validating context polymorphism.
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
) : Context, Context.LocaleContext, Context.PlatformContext, Context.VersionContext, Context.StableIdContext

data class CompositeContext(
    val context: Context,
    val experimentGroups: Set<String>,
    val sessionId: String,
) : Context,
    Context.LocaleContext,
    Context.PlatformContext,
    Context.VersionContext,
    Context.StableIdContext {
    override val locale = (context as Context.LocaleContext).locale
    override val platform = (context as Context.PlatformContext).platform
    override val appVersion = (context as Context.VersionContext).appVersion
    override val stableId = (context as Context.StableIdContext).stableId
    override val axisValues = context.axisValues
}

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
) : Context, Context.LocaleContext, Context.PlatformContext, Context.VersionContext, Context.StableIdContext

// ========== Enterprise Feature Flags ==========

/**
 * Feature flags for enterprise contexts.
 */
object EnterpriseFeatures : Namespace.TestNamespaceFacade("enterprise-features") {
    /** Advanced analytics feature */
    val advanced_analytics by boolean<EnterpriseContext>(default = false)

    /** Custom branding feature */
    val custom_branding by boolean<CompositeContext>(default = false)

    /** API access feature */
    val api_access by boolean<EnterpriseContext>(default = false)
}

/**
 * Feature flags for experiment contexts.
 */
object ExperimentFeatures : Namespace.TestNamespaceFacade("experiment-features") {
    /** Homepage variant */
    val homepage_variant by string<ExperimentContext>(default = "default")

    /** Onboarding style */
    val onboarding_style by string<ExperimentContext>(default = "test")
}

// ========== Custom Rules ==========

/**
 * Custom targeting leaf for enterprise-specific evaluation logic.
 *
 * This demonstrates how to extend evaluation with business-specific constraints
 * using the [Targeting.Custom] approach.
 *
 * @property requiredTier Minimum subscription tier required, or null for any tier.
 * @property requiredRole Minimum user role required, or null for any role.
 */
fun enterpriseRule(
    requiredTier: SubscriptionTier? = null,
    requiredRole: UserRole? = null,
): Targeting.Custom<EnterpriseContext> = Targeting.Custom(
    block = { context ->
        (requiredTier == null || context.subscriptionTier >= requiredTier) &&
            (requiredRole == null || context.userRole >= requiredRole)
    },
    weight = listOfNotNull(requiredTier, requiredRole).size,
)
