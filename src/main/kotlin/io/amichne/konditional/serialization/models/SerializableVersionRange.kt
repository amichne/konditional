package io.amichne.konditional.serialization.models

import com.squareup.moshi.JsonClass

/**
 * Serializable representation of a VersionRange.
 * Uses a discriminator field to handle the sealed class hierarchy.
 */
@JsonClass(generateAdapter = true)
data class SerializableVersionRange(
    val type: VersionRangeType,
    val min: SerializableVersion? = null,
    val max: SerializableVersion? = null,
)
