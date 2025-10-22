package io.amichne.konditional.serialization.models

import com.squareup.moshi.JsonClass

/**
 * Patch update configuration that can be applied to an existing Snapshot.
 * Only includes flags that should be updated or added.
 */
@JsonClass(generateAdapter = true)
data class SerializablePatch(
    val flags: List<SerializableFlag>,
    val removeKeys: List<String> = emptyList(),
)
