package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.core.ValueType
import io.amichne.konditional.core.types.EncodableValue

/**
 * Type-safe representation of flag values that replaces the type-erased SerializableValue.
 *
 * This sealed class follows parseUnsafe-don't-validate principles:
 * - No type erasure via `Any`
 * - Compile-time type safety
 * - Illegal states are unrepresentable (can't have INT type with Boolean value)
 *
 * Each subclass encodes both the value AND its type in a type-safe manner.
 *
 * Supports only primitive types:
 * - Boolean, String, Int, Double
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
    data class DoubleValue(override val value: Double) : FlagValue<Double>() {
        override fun toValueType() = ValueType.DOUBLE
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
            is Double -> DoubleValue(value)
            else -> throw IllegalArgumentException(
                "Unsupported value type: ${value::class.simpleName}. " +
                "Only primitives (Boolean, String, Int, Double) are supported."
            )
        }
    }
}
