package io.amichne.konditional.core.types

import kotlin.reflect.KClass

/**
 * Sealed interface representing encodable value types.
 *
 * Supports:
 * - Primitives: Boolean, String, Int, Double
 * - JSON Objects: Any type that can be represented as a JSON object
 * - Custom Wrappers: Extension types that wrap primitives (e.g., DateTime, UUID)
 *
 * This enforces compile-time type safety by making Conditional and FeatureFlag
 * only accept EncodableValue subtypes, preventing unsupported types entirely.
 *
 * Parse, don't validate: The type system makes illegal states unrepresentable.
 */
sealed interface EncodableValue<T : Any> {
    val value: T
    val encoding: Encoding

    enum class Encoding(val klazz: KClass<*>?) {
        BOOLEAN(Boolean::class),
        STRING(String::class),
        INTEGER(Int::class),
        DECIMAL(Double::class),
        JSON(null),  // JSON objects have no single KClass
    }

    // ========== Primitive Types ==========

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

    // ========== JSON Object Type ==========

    /**
     * Represents a JSON object value that can encode/decode to/from a Map representation.
     *
     * This enables support for:
     * - Complex data classes
     * - HSON-object type representations (conditional values as distinct object nodes)
     * - Any type with custom JSON serialization
     *
     * @param T The domain type being encoded
     * @property value The actual domain object
     * @property encoder Function to convert T to JSON-serializable Map
     * @property decoder Function to reconstruct T from JSON Map
     */
    data class JsonObjectEncodeable<T : Any>(
        override val value: T,
        val encoder: (T) -> Map<String, Any?>,
        val decoder: (Map<String, Any?>) -> T,
    ) : EncodableValue<T> {
        override val encoding: Encoding = Encoding.JSON

        companion object {
            /**
             * Creates a JsonObjectEncodeable using reflection-based Moshi serialization.
             * For production use, provide explicit encoder/decoder functions.
             */
            inline fun <reified T : Any> of(
                value: T,
                noinline encoder: (T) -> Map<String, Any?>,
                noinline decoder: (Map<String, Any?>) -> T,
            ): JsonObjectEncodeable<T> = JsonObjectEncodeable(value, encoder, decoder)
        }
    }

    // ========== Custom Wrapper Types ==========

    /**
     * Represents a custom wrapper type that encodes to a JSON primitive.
     *
     * This enables extension types like:
     * - DateTime (encodes to ISO-8601 String)
     * - UUID (encodes to String)
     * - Duration (encodes to milliseconds Int/Double)
     * - Email (encodes to validated String)
     *
     * These are "0-depth primitive-like values" - wrappers around JSON primitives
     * that provide type safety and domain semantics.
     *
     * @param T The custom wrapper type
     * @param P The primitive type it encodes to (Boolean, String, Int, or Double)
     */
    data class CustomEncodeable<T : Any, P : Any>(
        override val value: T,
        val primitiveEncoding: Encoding,
        val encoder: (T) -> P,
        val decoder: (P) -> T,
    ) : EncodableValue<T> {
        override val encoding: Encoding = primitiveEncoding

        init {
            require(primitiveEncoding in listOf(Encoding.BOOLEAN, Encoding.STRING, Encoding.INTEGER, Encoding.DECIMAL)) {
                "CustomEncodeable must encode to a primitive type (BOOLEAN, STRING, INTEGER, or DECIMAL)"
            }
        }

        companion object {
            /**
             * Creates a CustomEncodeable that encodes to String.
             * Common for DateTime, UUID, Email, etc.
             */
            fun <T : Any> asString(
                value: T,
                encoder: (T) -> String,
                decoder: (String) -> T,
            ): CustomEncodeable<T, String> =
                CustomEncodeable(value, Encoding.STRING, encoder, decoder)

            /**
             * Creates a CustomEncodeable that encodes to Int.
             * Common for enums, timestamps, etc.
             */
            fun <T : Any> asInt(
                value: T,
                encoder: (T) -> Int,
                decoder: (Int) -> T,
            ): CustomEncodeable<T, Int> =
                CustomEncodeable(value, Encoding.INTEGER, encoder, decoder)

            /**
             * Creates a CustomEncodeable that encodes to Double.
             * Common for durations, measurements, etc.
             */
            fun <T : Any> asDouble(
                value: T,
                encoder: (T) -> Double,
                decoder: (Double) -> T,
            ): CustomEncodeable<T, Double> =
                CustomEncodeable(value, Encoding.DECIMAL, encoder, decoder)

            /**
             * Creates a CustomEncodeable that encodes to Boolean.
             * Less common, but available for completeness.
             */
            fun <T : Any> asBoolean(
                value: T,
                encoder: (T) -> Boolean,
                decoder: (Boolean) -> T,
            ): CustomEncodeable<T, Boolean> =
                CustomEncodeable(value, Encoding.BOOLEAN, encoder, decoder)
        }
    }
}
