package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass

/**
 * Serializable representation of a SingletonModuleRegistry.Konfig configuration.
 * This is the top-level object that gets serialized to/from JSON.
 */
@JsonClass(generateAdapter = true)
internal data class SerializableSnapshot(
    val flags: List<SerializableFlag>,
)
