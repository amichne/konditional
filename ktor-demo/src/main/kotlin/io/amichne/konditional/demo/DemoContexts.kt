package io.amichne.konditional.demo

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.id.StableId

/**
 * Basic contextFn implementation for the demo
 */
data class DemoContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
) : Context

/**
 * Extended contextFn with additional enterprise features
 */
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    // Extended fields
    val subscriptionTier: SubscriptionTier,
    val organizationId: String,
    val employeeCount: Int,
) : Context

/**
 * Subscription tier for enterprise users
 */
enum class SubscriptionTier {
    FREE,
    STARTER,
    PROFESSIONAL,
    ENTERPRISE;

    companion object {
        fun fromString(value: String): SubscriptionTier =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: FREE
    }
}
