package io.amichne.konditional.core.types

import kotlin.reflect.KClass

/**
 * Bidirectional converter between domain type and primitive encoding.
 *
 * Enforces that both encoding and decoding logic are provided together,
 * preventing partial implementations and ensuring round-trip safety.
 *
 * @param I Input domain type
 * @param O Output primitive type
 */
data class Converter<I : Any, O : Any>(
    private val encodeFn: (I) -> O,
    private val decodeFn: (O) -> I
) {
    fun encode(input: I): O = encodeFn(input)
    fun decode(output: O): I = decodeFn(output)
}

/**
 * Sealed interface representing encodable value types.
 *
 * Supports only primitive types:
 * - Boolean
 * - String
 * - Int
 * - Double
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
        DECIMAL(Double::class);

        companion object {
            /**
             * Parse a value into an EncodableValue with compile-time evidence.
             * Requires EncodableEvidence to prove the type is supported at compile-time.
             */
            inline fun <reified T : Any> of(
                value: T,
                evidence: EncodableEvidence<T> = EncodableEvidence.get()
            ): EncodableValue<T> {
                @Suppress("UNCHECKED_CAST")
                return when (evidence.encoding) {
                    BOOLEAN -> BooleanEncodeable(value as Boolean)
                    STRING -> StringEncodeable(value as String)
                    INTEGER -> IntEncodeable(value as Int)
                    DECIMAL -> DecimalEncodeable(value as Double)
                } as EncodableValue<T>
            }
        }
    }

    // ========== Primitive Types ==========

    data class BooleanEncodeable(override val value: Boolean) : EncodableValue<Boolean> {
        override val encoding: Encoding = Encoding.BOOLEAN
    }

    data class StringEncodeable(override val value: String) : EncodableValue<String> {
        override val encoding: Encoding = Encoding.STRING
    }

    data class IntEncodeable(override val value: Int) : EncodableValue<Int> {
        override val encoding: Encoding = Encoding.INTEGER
    }

    data class DecimalEncodeable(override val value: Double) : EncodableValue<Double> {
        override val encoding: Encoding = Encoding.DECIMAL
    }
}
