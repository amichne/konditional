package io.amichne.konditional.example

import io.amichne.konditional.builders.ConfigBuilder.Companion.buildSnapshot
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.BooleanFeature
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.Module
import io.amichne.konditional.core.boolean
import io.amichne.konditional.core.moduleNameFromEnum

/**
 * Example flags for the UI module.
 */
enum class SampleFeatureEnum(
    override val key: String,
) : BooleanFeature<Context> by boolean(key) {
    ENABLE_COMPACT_CARDS("enable_compact_cards"),
    USE_LIGHTWEIGHT_HOME("use_lightweight_home"),
    FIFTY_TRUE_US_IOS("fifty_true_us_ios"),
    DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY("default_true_except_android_legacy"),
    PRIORITY_CHECK("priority_check"),
    VERSIONED("versioned"),
    UNIFORM50("uniform50"),
}

/**
 * Example module enum demonstrating module-based flag organization.
 *
 * Each enum constant represents a module containing a fixed set of feature flags.
 * The enum constant name becomes the module name.
 */
enum class SampleModules : Module {
    /**
     * UI-related feature flags.
     */
    UI_FEATURES,

    /**
     * Experimental feature flags.
     */
    EXPERIMENTAL
}

/**
 * Example demonstrating the new module-based configuration approach.
 */
val runnable: () -> Unit = {
    val konfig = buildSnapshot {
        module(SampleModules.UI_FEATURES) {
            SampleFeatureEnum.ENABLE_COMPACT_CARDS with {
                default(true)
                rule {
                    locales()
                } implies false
            }
            SampleFeatureEnum.USE_LIGHTWEIGHT_HOME with {
                default(false)
            }
        }

        module(SampleModules.EXPERIMENTAL) {
            SampleFeatureEnum.FIFTY_TRUE_US_IOS with {
                default(false)
            }
            SampleFeatureEnum.DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY with {
                default(true)
            }
            SampleFeatureEnum.PRIORITY_CHECK with {
                default(false)
            }
            SampleFeatureEnum.VERSIONED with {
                default(false)
            }
            SampleFeatureEnum.UNIFORM50 with {
                default(false)
            }
        }
    }
}
