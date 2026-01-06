package io.amichne.konditional.serialization.instance

import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.instance.ConfigurationMetadataView
import io.amichne.konditional.core.instance.ConfigurationView

@ConsistentCopyVisibility
data class Configuration(
    override val flags: Map<Feature<*, *, *>, FlagDefinition<*, *, *>>,
    override val metadata: ConfigurationMetadata = ConfigurationMetadata(),
) : ConfigurationView {
    fun diff(other: Configuration): ConfigurationDiff = ConfigurationDiff.between(this, other)

    fun withMetadata(metadata: ConfigurationMetadata): Configuration = copy(metadata = metadata)

    fun withMetadata(
        version: String? = null,
        generatedAtEpochMillis: Long? = null,
        source: String? = null,
    ): Configuration =
        withMetadata(
            ConfigurationMetadata(
                version = version,
                generatedAtEpochMillis = generatedAtEpochMillis,
                source = source,
            ),
        )
}

@ConsistentCopyVisibility
data class ConfigurationMetadata(
    override val version: String? = null,
    override val generatedAtEpochMillis: Long? = null,
    override val source: String? = null,
) : ConfigurationMetadataView

