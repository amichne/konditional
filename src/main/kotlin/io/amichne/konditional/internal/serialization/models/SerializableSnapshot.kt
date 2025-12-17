package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass

/**
 * Serializable representation of a Configuration configuration.
 * This is the top-level object that gets serialized to/from JSON.
 */
@JsonClass(generateAdapter = true)
internal data class SerializableSnapshot(
    val meta: SerializableSnapshotMetadata? = null,
    val flags: List<SerializableFlag>,
)
