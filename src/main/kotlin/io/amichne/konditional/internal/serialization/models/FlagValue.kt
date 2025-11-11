package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.core.ValueType

/**
 * Type-safe representation of flag values that replaces the type-erased SerializableValue.
 *
 * This sealed class follows parse-don't-validate principles:
 * - No type erasure via `Any`
 * - Compile-time type safety
 * - Illegal states are unrepresentable (can't have INT type with Boolean value)
 *
 * Each subclass encodes both the value AND its type in a type-safe manner.
 *
 * Supports:
 * - Primitives: Boolean, String, Int, Long, Double
 * - Complex: JSON objects (Map<String, Any?>)
 */
internal sealed class FlagValue<out T : Any> {
    abstract val value: T

    /**
     * Returns the ValueType corresponding to this FlagValue subclass.
     */
    abstract fun toValueType(): ValueType

    // ========== Primitive Types ==========

    @JsonClass(generateAdapter = true)
    data class BooleanValue(override val value: Boolean) : FlagValue<Boolean>() {
        override fun toValueType() = ValueType.BOOLEAN
    }

    @JsonClass(generateAdapter = true)
    data class StringValue(override val value: String) : FlagValue<String>() {
        override fun toValueType() = ValueType.STRING
    }

    @JsonClass(generateAdapter = true)
    data class IntValue(override val value: Int) : FlagValue<Int>() {
        override fun toValueType() = ValueType.INT
    }

    @JsonClass(generateAdapter = true)
    data class LongValue(override val value: Long) : FlagValue<Long>() {
        override fun toValueType() = ValueType.LONG
    }

    @JsonClass(generateAdapter = true)
    data class DoubleValue(override val value: Double) : FlagValue<Double>() {
        override fun toValueType() = ValueType.DOUBLE
    }

    // ========== JSON Object Type ==========

    /**
     * Represents a JSON object value.
     *
     * The value is stored as a Map<String, Any?> which can be serialized/deserialized
     * using Moshi. Domain types must provide encoder/decoder functions to convert
     * to/from this representation.
     */
    @JsonClass(generateAdapter = true)
    data class JsonObjectValue(override val value: Map<String, Any?>) : FlagValue<Map<String, Any?>>() {
        override fun toValueType() = ValueType.JSON
    }

    companion object {
        /**
         * Creates a FlagValue from an untyped value by inferring its type.
         * Used during serialization when we have a typed domain value.
         */
        fun from(value: Any): FlagValue<*> = when (value) {
            is Boolean -> BooleanValue(value)
            is String -> StringValue(value)
            is Int -> IntValue(value)
            is Long -> LongValue(value)
            is Double -> DoubleValue(value)
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                JsonObjectValue(value as Map<String, Any?>)
            }
            else -> throw IllegalArgumentException(
                "Unsupported value type: ${value::class.simpleName}. " +
                "Primitives (Boolean, String, Int, Long, Double) and Map<String, Any?> are supported."
            )
        }
    }
}
