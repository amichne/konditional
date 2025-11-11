package io.amichne.konditional.example

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Feature
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.FlagRegistry
import io.amichne.konditional.core.types.EncodableValue

enum class SampleFeatureEnum(
    override val key: String,
) : Feature<EncodableValue.BooleanEncodeable, Boolean, Context> {
    ENABLE_COMPACT_CARDS("enable_compact_cards"),
    USE_LIGHTWEIGHT_HOME("use_lightweight_home"),
    FIFTY_TRUE_US_IOS("fifty_true_us_ios"),
    DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY("default_true_except_android_legacy"),
    PRIORITY_CHECK("priority_check"),
    VERSIONED("versioned"),
    UNIFORM50("uniform50");

    override val registry: FlagRegistry = FlagRegistry

    override fun update(definition: FlagDefinition<EncodableValue.BooleanEncodeable, Boolean, Context>) {
        registry.update(definition)
    }
}
