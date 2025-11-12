package io.amichne.konditional.example

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.BooleanFeature
import io.amichne.konditional.core.FeatureModule

/**
 * Example features demonstrating the featureModule-scoped architecture.
 *
 * In the new design, each team's features are bound to their featureModule at compile-time.
 * This example shows flags from different modules to demonstrate isolation.
 */

/**
 * Core featureModule features accessible to all teams.
 *
 * These are system-wide flags like kill switches and maintenance modes.
 */
enum class CoreFeatures(
    override val key: String,
) : BooleanFeature<Context, FeatureModule.Core> {
    KILL_SWITCH("kill_switch"),
    MAINTENANCE_MODE("maintenance_mode");

    override val module = FeatureModule.Core
}

/**
 * Example features for the Payments team featureModule.
 *
 * These flags are completely isolated from other teams' modules.
 */
enum class PaymentFeatures(
    override val key: String,
) : BooleanFeature<Context, FeatureModule.Team.Payments> {
    ENABLE_COMPACT_CARDS("enable_compact_cards"),
    USE_LIGHTWEIGHT_HOME("use_lightweight_home"),
    FIFTY_TRUE_US_IOS("fifty_true_us_ios");

    override val module = FeatureModule.Team.Payments
}

/**
 * Example features for the Search team featureModule.
 *
 * These flags are completely isolated from other teams' modules.
 */
enum class SearchFeatures(
    override val key: String,
) : BooleanFeature<Context, FeatureModule.Team.Search> {
    DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY("default_true_except_android_legacy"),
    PRIORITY_CHECK("priority_check"),
    VERSIONED("versioned"),
    UNIFORM50("uniform50");

    override val module = FeatureModule.Team.Search
}
