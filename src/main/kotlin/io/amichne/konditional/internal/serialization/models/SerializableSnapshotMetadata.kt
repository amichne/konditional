package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SerializableSnapshotMetadata(
    val version: String? = null,
    val generatedAtEpochMillis: Long? = null,
    val source: String? = null,
)

