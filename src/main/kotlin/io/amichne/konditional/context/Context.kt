package io.amichne.konditional.context

import io.amichne.konditional.core.StableId

/**
 * Represents the execution context for konditional operations.
 *
 * This data class holds contextual information that may be used
 * during evaluation or processing boundary the konditional framework.
 *
 * @property ... (Add property descriptions as needed)
 */

interface Context {
    val locale: AppLocale
    val platform: Platform
    val appVersion: Version
    val stableId: StableId

    companion object {
        operator fun invoke(
            locale: AppLocale,
            platform: Platform,
            appVersion: Version,
            stableId: StableId,
        ): Context = object : Context {
            override val locale: AppLocale = locale
            override val platform: Platform = platform
            override val appVersion: Version = appVersion
            override val stableId: StableId = stableId
        }
    }
}
