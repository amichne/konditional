package io.amichne.konditional.example

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Rollout.Companion.default
import io.amichne.konditional.context.Version.Companion.default
import io.amichne.konditional.context.evaluate
import io.amichne.konditional.core.BooleanFeature
import io.amichne.konditional.core.FeatureContainer
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.config
import io.amichne.konditional.serialization.SnapshotSerializer.Companion.default

/**
 * Example features demonstrating the taxonomy-scoped architecture.
 *
 * In the new design, each team's features are bound to their taxonomy at compile-time.
 * This example shows flags from different modules to demonstrate isolation.
 */

/**
 * Core taxonomy features accessible to all teams.
 *
 * These are system-wide flags like kill switches and maintenance modes.
 */
// TODO - try to constrain the feature container on the taxonomy type only, and feature level allow context to be dynamic
object CoreFeatures : FeatureContainer<Context, Taxonomy.Core>(Taxonomy.Core) {
    val KILL_SWITCH: BooleanFeature<Context, Taxonomy.Core> by boolean("kill_switch") {
        default(true)
    }

}

object test {
    init {
        if (Context(TODO(), TODO(), TODO(), TODO()).evaluate(CoreFeatures.KILL_SWITCH) ) {

        }
    }
}

/**
 * Example features for the Payments team taxonomy.
 *
 * These flags are completely isolated from other teams' modules.
 */
enum class PaymentFeatures : FeatureContainer<Context, Taxonomy.Domain.Payments>(Taxonomy.Domain.Payments) {
    ENABLE_COMPACT_CARDS("enable_compact_cards"),
    USE_LIGHTWEIGHT_HOME("use_lightweight_home"),
    FIFTY_TRUE_US_IOS("fifty_true_us_ios");

    override val module = Taxonomy.Domain.Payments
}

/**
 * Example features for the Search team taxonomy.
 *
 * These flags are completely isolated from other teams' modules.
 */
enum class SearchFeatures(
    override val key: String,
) : BooleanFeature<Context, Taxonomy.Domain.Search> {
    DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY("default_true_except_android_legacy"),
    PRIORITY_CHECK("priority_check"),
    VERSIONED("versioned"),
    UNIFORM50("uniform50");

    override val module = Taxonomy.Domain.Search
}
