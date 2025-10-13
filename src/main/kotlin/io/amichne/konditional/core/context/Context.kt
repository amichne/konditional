package io.amichne.konditional.core.context

import io.amichne.konditional.core.StableId
import io.amichne.konditional.core.context.Version

data class Context(
    val locale: AppLocale,
    val platform: Platform,
    val appVersion: Version,
    val stableId: StableId
)
