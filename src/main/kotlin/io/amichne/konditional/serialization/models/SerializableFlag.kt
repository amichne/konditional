package io.amichne.konditional.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.core.ValueType

/**
 * Serializable representation of a single flag configuration.
 * Contains all the data needed to reconstruct a FlagEntry with its Condition.
 */
@JsonClass(generateAdapter = true)
data class SerializableFlag(
    val key: String,
    val type: ValueType,
    val defaultValue: Any,
    val salt: String = "v1",
    val isActive: Boolean = true,
    val rules: List<SerializableRule> = emptyList(),
    val default: SerializableRule.SerializableValue = SerializableRule.SerializableValue(defaultValue, type),
)
