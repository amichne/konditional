package io.amichne.konditional.example

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Context.Companion.evaluate
import io.amichne.konditional.context.Platform
import io.amichne.konditional.core.BooleanFeature
import io.amichne.konditional.core.FeatureContainer
import io.amichne.konditional.core.Taxonomy

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
data object CoreFeatures : FeatureContainer<Taxonomy.Core>(Taxonomy.Core) {
    val toggleOnOff: BooleanFeature<Context, Taxonomy.Core> by boolean {
        default(true)
    }

    val MAINTENANCE_MODE: BooleanFeature<Context, Taxonomy.Core> by boolean {
        default(false)
    }

}

object test {
    init {
        if (Context(TODO(), TODO(), TODO(), TODO()).evaluate(CoreFeatures.toggleOnOff)) {

        }
    }
}

/**
 * Example features for the Payments team taxonomy.
 *
 * These flags are completely isolated from other teams' modules.
 */
data object PaymentFeatures : FeatureContainer<Taxonomy.Domain.Payments>(Taxonomy.Domain.Payments) {
    val ENABLE_COMPACT_CARDS by boolean<Context> {

    }
    val USE_LIGHTWEIGHT_HOME by boolean<Context> {

    }
    val FIFTY_TRUE_US_IOS by boolean<Context> {
        default(false)

        rule {
            platforms(Platform.IOS)
            rollout { 50 }
        }

    }
}

/**
 * Example features for the Search team taxonomy.
 *
 * These flags are completely isolated from other teams' modules.
 */
object SearchFeatures : FeatureContainer<Taxonomy.Domain.Search>(Taxonomy.Domain.Search) {
    val DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY by string<Context> { }
    val PRIORITY_CHECK by boolean<Context> { }
    val VERSIONED by string<Context> { }
    val UNIFORM50 by string<Context> { }
}
