package io.amichne.konditional.example

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Conditional

enum class SampleFeatureEnum(
    override val key: String,
) : Conditional<Boolean, Context> {
    ENABLE_COMPACT_CARDS("enable_compact_cards"),
    USE_LIGHTWEIGHT_HOME("use_lightweight_home"),
    FIFTY_TRUE_US_IOS("fifty_true_us_ios"),
    DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY("default_true_except_android_legacy"),
    PRIORITY_CHECK("priority_check"),
    VERSIONED("versioned"),
    UNIFORM50("uniform50"),
}
