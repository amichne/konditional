package io.amichne.konditional.core.instance

import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.features.Feature

@ConsistentCopyVisibility
data class Configuration internal constructor(
    val flags: Map<Feature<*, *, *>, FlagDefinition<*, *, *>>,
    val metadata: ConfigurationMetadata = ConfigurationMetadata(),
) {
    fun diff(other: Configuration): ConfigurationDiff = ConfigurationDiff.between(this, other)

    fun withMetadata(metadata: ConfigurationMetadata): Configuration = Configuration(flags, metadata)

    fun withMetadata(
        version: String? = null,
        generatedAtEpochMillis: Long? = null,
        source: String? = null,
    ): Configuration =
        withMetadata(
            ConfigurationMetadata.of(
                version = version,
                generatedAtEpochMillis = generatedAtEpochMillis,
                source = source,
            ),
        )
}
