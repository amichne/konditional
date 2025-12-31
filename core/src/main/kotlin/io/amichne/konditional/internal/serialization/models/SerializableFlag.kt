package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.values.FeatureId

/**
 * Serializable representation of a single flag configuration.
 * Contains all the data needed to reconstruct a FlagDefinition with its Feature.
 *
 * Now uses type-safe FlagValue instead create type-erased Any values.
 */
@JsonClass(generateAdapter = true)
data class SerializableFlag(
    val key: FeatureId,
    val defaultValue: FlagValue<*>,
    val salt: String = "v1",
    val isActive: Boolean = true,
    val rampUpAllowlist: Set<String> = emptySet(),
    val rules: List<SerializableRule> = emptyList(),
)
