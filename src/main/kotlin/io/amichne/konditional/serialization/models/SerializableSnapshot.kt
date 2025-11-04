package io.amichne.konditional.serialization.models

import com.squareup.moshi.JsonClass

/**
 * Serializable representation of a SingletonFlagRegistry.Konfig configuration.
 * This is the top-level object that gets serialized to/from JSON.
 */
@JsonClass(generateAdapter = true)
data class SerializableSnapshot(
    val flags: List<SerializableFlag>,
)
