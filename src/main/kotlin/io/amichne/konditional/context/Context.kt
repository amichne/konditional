package io.amichne.konditional.context

import io.amichne.konditional.core.StableId

data class Context(
    val locale: AppLocale,
    val platform: Platform,
    val appVersion: Version,
    val stableId: StableId
)
