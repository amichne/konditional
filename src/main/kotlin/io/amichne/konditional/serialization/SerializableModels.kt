package io.amichne.konditional.serialization

import com.squareup.moshi.JsonClass

/**
 * Serializable representation of a Flags.Snapshot configuration.
 * This is the top-level object that gets serialized to/from JSON.
 */
@JsonClass(generateAdapter = true)
data class SerializableSnapshot(
    val flags: List<SerializableFlag>,
)

/**
 * Serializable representation of a single flag configuration.
 * Contains all the data needed to reconstruct a FlagEntry with its Condition.
 */
@JsonClass(generateAdapter = true)
data class SerializableFlag(
    val key: String,
    val valueType: ValueType,
    val defaultValue: Any,
    val salt: String = "v1",
    val isActive: Boolean = true,
    val rules: List<SerializableRule> = emptyList(),
)

/**
 * Enum representing the supported value types for feature flags.
 */
enum class ValueType {
    BOOLEAN,
    STRING,
    INT,
    LONG,
    DOUBLE,
}

/**
 * Serializable representation of a Surjection (rule + value pair).
 */
@JsonClass(generateAdapter = true)
data class SerializableRule(
    val value: Any,
    val rampUp: Double = 100.0,
    val note: String? = null,
    val locales: Set<String> = emptySet(),
    val platforms: Set<String> = emptySet(),
    val versionRange: SerializableVersionRange? = null,
)

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

/**
 * Enum discriminator for VersionRange types.
 */
enum class VersionRangeType {
    UNBOUNDED,
    LEFT_BOUND,
    RIGHT_BOUND,
    FULLY_BOUND,
}

/**
 * Serializable representation of a Version.
 */
@JsonClass(generateAdapter = true)
data class SerializableVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
)

/**
 * Patch update configuration that can be applied to an existing Snapshot.
 * Only includes flags that should be updated or added.
 */
@JsonClass(generateAdapter = true)
data class SerializablePatch(
    val flags: List<SerializableFlag>,
    val removeKeys: List<String> = emptyList(),
)
