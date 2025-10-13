package io.amichne.konditional.core

enum class FeatureFlagPlaceholder(val key: String) {
    ENABLE_COMPACT_CARDS("enable_compact_cards"),
    USE_LIGHTWEIGHT_HOME("use_lightweight_home"),
    FIFTY_TRUE_US_IOS("fifty_true_us_ios"),
    DEFAULT_TRUE_EXCEPT_ANDROID_LEGACY("default_true_except_android_legacy"),
    PRIORITY_CHECK("priority_check"),
    VERSIONED("versioned"),
    DEFAULT_FALSE_WITH_30_TRUE("default_false_with_30_true"),
    UNIFORM50("uniform50");
}
