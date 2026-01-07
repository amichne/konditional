package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.internal.KonditionalInternalApi
import io.amichne.konditional.serialization.instance.ConfigurationMetadata

@KonditionalInternalApi
@JsonClass(generateAdapter = true)
data class SerializableSnapshotMetadata(
    val version: String? = null,
    val generatedAtEpochMillis: Long? = null,
    val source: String? = null,
) {
    fun toConfigurationMetadata(): ConfigurationMetadata =
        ConfigurationMetadata(
            version = version,
            generatedAtEpochMillis = generatedAtEpochMillis,
            source = source,
        )

    companion object {
        fun from(metadata: ConfigurationMetadata): SerializableSnapshotMetadata? =
            if (metadata.version == null && metadata.generatedAtEpochMillis == null && metadata.source == null) {
                null
            } else {
                SerializableSnapshotMetadata(
                    version = metadata.version,
                    generatedAtEpochMillis = metadata.generatedAtEpochMillis,
                    source = metadata.source,
                )
            }
    }
}
