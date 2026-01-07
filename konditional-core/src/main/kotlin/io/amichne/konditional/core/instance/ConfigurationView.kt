@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core.instance

import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.internal.KonditionalInternalApi

/**
 * Read-only view over a loaded configuration.
 *
 * `:konditional-core` depends only on this abstraction; concrete configuration models live in sibling modules.
 */
interface ConfigurationView {
    val flags: Map<Feature<*, *, *>, FlagDefinition<*, *, *>>
    val metadata: ConfigurationMetadataView
}

/**
 * Read-only view over configuration metadata.
 */
interface ConfigurationMetadataView {
    val version: String?
    val generatedAtEpochMillis: Long?
    val source: String?
}
