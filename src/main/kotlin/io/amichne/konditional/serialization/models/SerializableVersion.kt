package io.amichne.konditional.serialization.models

import com.squareup.moshi.JsonClass

/**
 * Serializable representation of a Version.
 */
@JsonClass(generateAdapter = true)
data class SerializableVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
)
