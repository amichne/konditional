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
 * Supports primitive types and user-defined types:
 * - Boolean, String, Int, Double, Enum
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

    /**
     * Represents an enum value.
     * Stores the enum as a string (its name) along with the fully qualified enum class name
     * to enable proper deserialization.
     */
    @JsonClass(generateAdapter = true)
    data class EnumValue(
        override val value: String,
        val enumClassName: String
    ) : FlagValue<String>() {
        override fun toValueType() = ValueType.ENUM
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
            is Enum<*> -> EnumValue(
                value = value.name,
                enumClassName = value.javaClass.name
            )
            else -> throw IllegalArgumentException(
                "Unsupported value type: ${value::class.simpleName}. " +
                "Supported types: Boolean, String, Int, Double, Enum."
            )
        }
    }
}
