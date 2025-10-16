package io.amichne.konditional.context

import io.amichne.konditional.core.StableId

/**
 * Represents the execution context for konditional operations.
 *
 * This data class holds contextual information that may be used
 * during evaluation or processing within the konditional framework.
 *
 * @property ... (Add property descriptions as needed)
 */
data class Context(
    val locale: AppLocale,
    val platform: Platform,
    val appVersion: Version,
    val stableId: StableId
)
