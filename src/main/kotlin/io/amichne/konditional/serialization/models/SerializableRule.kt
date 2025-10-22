package io.amichne.konditional.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.core.ValueType

/**
 * Serializable representation of a Surjection (rule + value pair).
 */
@JsonClass(generateAdapter = true)
data class SerializableRule(
    val value: SerializableValue,
    val rampUp: Double = 100.0,
    val note: String? = null,
    val locales: Set<String> = emptySet(),
    val platforms: Set<String> = emptySet(),
    val versionRange: SerializableVersionRange? = null,
) {
    /**
     * Serializable value containter, long term plan to use something like polymorphic enumerator/enumerated to solve
     *
     * @property value
     * @property type
     * @constructor Create empty Serializable value
     */
    data class SerializableValue(
        val value: Any,
        val type: ValueType,
    )
}
