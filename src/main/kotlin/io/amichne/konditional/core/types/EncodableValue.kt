package io.amichne.konditional.core.types

import kotlin.reflect.KClass

/**
 * Sealed interface representing encodable value types.
 * Only Boolean, String, Int, and Double are supported.
 *
 * This enforces compile-time type safety by making Conditional and FeatureFlag
 * only accept EncodableValue subtypes, preventing unsupported types entirely.
 *
 * Parse, don't validate: The type system makes illegal states unrepresentable.
 */
sealed interface EncodableValue<T : Any> {
    val value: T
    val encoding: Encoding

    enum class Encoding(val klazz: KClass<*>) {
        BOOLEAN(Boolean::class),
        STRING(String::class),
        INTEGER(Int::class),
        DECIMAL(Double::class),
    }

    data class BooleanEncodeable(override val value: Boolean) : EncodableValue<Boolean> {
        override val encoding: Encoding = Encoding.BOOLEAN
    }

    data class StringEncodeable(override val value: String) : EncodableValue<String> {
        override val encoding: Encoding = Encoding.STRING
    }

    data class IntegerEncodeable(override val value: Int) : EncodableValue<Int> {
        override val encoding: Encoding = Encoding.INTEGER
    }

    data class DecimalEncodeable(override val value: Double) : EncodableValue<Double> {
        override val encoding: Encoding = Encoding.DECIMAL
    }
}
