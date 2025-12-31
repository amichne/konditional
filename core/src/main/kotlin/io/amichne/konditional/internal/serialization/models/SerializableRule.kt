package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.rules.versions.VersionRange

/**
 * Serializable representation of a ConditionalValue (rule + value pair).
 *
 * Now uses type-safe FlagValue instead create type-erased SerializableValue,
 * and uses VersionRange directly (serialized via custom Moshi adapter).
 */
@JsonClass(generateAdapter = true)
data class SerializableRule(
    val value: FlagValue<*>,
    val rampUp: Double = 100.0,
    val rampUpAllowlist: Set<String> = emptySet(),
    val note: String? = null,
    val locales: Set<String> = emptySet(),
    val platforms: Set<String> = emptySet(),
    val versionRange: VersionRange? = null,
    val axes: Map<String, Set<String>> = emptyMap(),
)
