package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.values.FeatureId

/**
 * Patch update configuration that can be applied to an existing Configuration.
 * Only includes flags that should be updated or added.
 */
@JsonClass(generateAdapter = true)
internal data class SerializablePatch(
    val meta: SerializableSnapshotMetadata? = null,
    val flags: List<SerializableFlag>,
    val removeKeys: List<FeatureId> = emptyList(),
)
