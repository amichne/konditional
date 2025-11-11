package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass

/**
 * Serializable representation of a single flag configuration.
 * Contains all the data needed to reconstruct a FlagDefinition with its Conditional.
 *
 * Now uses type-safe FlagValue instead of type-erased Any values.
 */
@JsonClass(generateAdapter = true)
internal data class SerializableFlag(
    val key: String,
    val defaultValue: FlagValue<*>,
    val salt: String = "v1",
    val isActive: Boolean = true,
    val rules: List<SerializableRule> = emptyList(),
)
