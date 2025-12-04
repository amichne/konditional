package io.amichne.konditional.core.types

import kotlin.reflect.KClass

/**
 * Encodeable wrapper for enum types.
 * Stores the enum value with compile-time type safety while supporting
 * serialization to/from String representation.
 *
 * @param E The specific enum type (must be an Enum)
 * @param value The enum value
 * @param enumClass The KClass of the enum for deserialization
 */
data class EnumEncodeable<E : Enum<*>>(
    override val value: E,
    val enumClass: KClass<out E>,
) : EncodableValue<E> {
    override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.ENUM

    /**
     * Returns the string representation of the enum value (its name).
     */
    fun toEncodedString(): String = value.name

    companion object {
        /**
         * Creates an EnumEncodeable from an enum value.
         */
        inline fun <reified E : Enum<E>> of(value: E): EnumEncodeable<E> {
            return EnumEncodeable(value, E::class)
        }

        /**
         * Decodes a string back to the enum value.
         */
        fun <E : Enum<*>> fromString(
            name: String,
            enumClass: KClass<E>,
        ): EnumEncodeable<E> {
            val enumValue = enumClass.java.enumConstants.first { it.name == name }
            return EnumEncodeable(enumValue, enumClass)
        }
    }
}
