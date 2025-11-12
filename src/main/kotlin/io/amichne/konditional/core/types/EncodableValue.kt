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
        JSON(null);  // JSON objects have no single KClass

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
                    JSON -> throw IllegalArgumentException("Cannot create JSON encodable from primitive. Use asJsonObject() instead.")
                } as EncodableValue<T>
            }

            /**
             * Deprecated unsafe version - use of(value, evidence) instead.
             */
            @Deprecated(
                "Use of(value, evidence) with explicit EncodableEvidence for type safety",
                ReplaceWith("of(value, EncodableEvidence.get())")
            )
            @Suppress("UNCHECKED_CAST")
            inline fun <reified T : Any> parse(value: T): EncodableValue<T> {
                return when (value) {
                    is Boolean -> BooleanEncodeable(value)
                    is String -> StringEncodeable(value)
                    is Int -> IntEncodeable(value)
                    is Double -> DecimalEncodeable(value)
                    else -> throw IllegalArgumentException("Unsupported EncodableValue type: ${T::class.simpleName}")
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

    // ========== JSON Object Type ==========

    /**
     * Represents a JSON object value that can encode/decode to/from a Map representation.
     *
     * This enables support for:
     * - Complex data classes
     * - JSON-object type representations (conditional values as distinct object nodes)
     * - Any type with custom JSON serialization
     *
     * @param T The domain type being encoded
     * @property value The actual domain object
     * @property converter Bidirectional converter between T and JSON Map
     */
    data class JsonObjectEncodeable<T : Any>(
        override val value: T,
        val converter: Converter<T, Map<String, Any?>>,
    ) : EncodableValue<T> {
        override val encoding: Encoding = Encoding.JSON

        @Deprecated(
            "Use the fluent builder API: value.asJsonObject().encoder { }.decoder { }",
            ReplaceWith("value.asJsonObject().encoder(encoder).decoder(decoder)")
        )
        constructor(
            value: T,
            encoder: (T) -> Map<String, Any?>,
            decoder: (Map<String, Any?>) -> T,
        ) : this(value, Converter(encoder, decoder))
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
        val converter: Converter<T, P>,
    ) : EncodableValue<T> {
        override val encoding: Encoding = primitiveEncoding

        init {
            require(
                primitiveEncoding in listOf(
                    Encoding.BOOLEAN,
                    Encoding.STRING,
                    Encoding.INTEGER,
                    Encoding.DECIMAL
                )
            ) {
                "CustomEncodeable must encode to a primitive type (BOOLEAN, STRING, INTEGER, or DECIMAL)"
            }
        }

        @Deprecated(
            "Use the fluent builder API: value.asCustomString().encoder { }.decoder { }",
            ReplaceWith("value.asCustomString().encoder(encoder).decoder(decoder)")
        )
        constructor(
            value: T,
            primitiveEncoding: Encoding,
            encoder: (T) -> P,
            decoder: (P) -> T,
        ) : this(value, primitiveEncoding, Converter(encoder, decoder))
    }
}

// ========== Fluent Builder API ==========
// Builder classes enforce typestate - do not instantiate directly, use extension functions

/**
 * Typestate builder for JSON object-encoded types.
 * Do not instantiate directly - use [asJsonObject] extension function.
 * @see asJsonObject
 */
class JsonObjectEncoderBuilder<T : Any> internal constructor(private val value: T) {
    fun encoder(block: (T) -> Map<String, Any?>): JsonObjectDecoderBuilder<T> =
        JsonObjectDecoderBuilder(value, block)
}

/**
 * Decoder builder for JSON object-encoded types.
 * Do not instantiate directly - obtained via [JsonObjectEncoderBuilder.encoder].
 */
class JsonObjectDecoderBuilder<T : Any> internal constructor(
    private val value: T,
    private val encoderFn: (T) -> Map<String, Any?>
) {
    fun decoder(block: (Map<String, Any?>) -> T): EncodableValue.JsonObjectEncodeable<T> =
        EncodableValue.JsonObjectEncodeable(value, Converter(encoderFn, block))
}

/**
 * Typestate builder for String-encoded custom types.
 * Do not instantiate directly - use [asCustomString] extension function.
 * @see asCustomString
 */
class CustomStringEncoderBuilder<T : Any> internal constructor(private val value: T) {
    fun encoder(block: (T) -> String): CustomStringDecoderBuilder<T> =
        CustomStringDecoderBuilder(value, block)
}

/**
 * Decoder builder for String-encoded custom types.
 * Do not instantiate directly - obtained via [CustomStringEncoderBuilder.encoder].
 */
class CustomStringDecoderBuilder<T : Any> internal constructor(
    private val value: T,
    private val encoderFn: (T) -> String
) {
    fun decoder(block: (String) -> T): EncodableValue.CustomEncodeable<T, String> =
        EncodableValue.CustomEncodeable(value, EncodableValue.Encoding.STRING, Converter(encoderFn, block))
}

/**
 * Typestate builder for Int-encoded custom types.
 * Do not instantiate directly - use [asCustomInt] extension function.
 * @see asCustomInt
 */
class CustomIntEncoderBuilder<T : Any> internal constructor(private val value: T) {
    fun encoder(block: (T) -> Int): CustomIntDecoderBuilder<T> =
        CustomIntDecoderBuilder(value, block)
}

/**
 * Decoder builder for Int-encoded custom types.
 * Do not instantiate directly - obtained via [CustomIntEncoderBuilder.encoder].
 */
class CustomIntDecoderBuilder<T : Any> internal constructor(
    private val value: T,
    private val encoderFn: (T) -> Int
) {
    fun decoder(block: (Int) -> T): EncodableValue.CustomEncodeable<T, Int> =
        EncodableValue.CustomEncodeable(value, EncodableValue.Encoding.INTEGER, Converter(encoderFn, block))
}

/**
 * Typestate builder for Double-encoded custom types.
 * Do not instantiate directly - use [asCustomDouble] extension function.
 * @see asCustomDouble
 */
class CustomDoubleEncoderBuilder<T : Any> internal constructor(private val value: T) {
    fun encoder(block: (T) -> Double): CustomDoubleDecoderBuilder<T> =
        CustomDoubleDecoderBuilder(value, block)
}

/**
 * Decoder builder for Double-encoded custom types.
 * Do not instantiate directly - obtained via [CustomDoubleEncoderBuilder.encoder].
 */
class CustomDoubleDecoderBuilder<T : Any> internal constructor(
    private val value: T,
    private val encoderFn: (T) -> Double
) {
    fun decoder(block: (Double) -> T): EncodableValue.CustomEncodeable<T, Double> =
        EncodableValue.CustomEncodeable(value, EncodableValue.Encoding.DECIMAL, Converter(encoderFn, block))
}

/**
 * Typestate builder for Boolean-encoded custom types.
 * Do not instantiate directly - use [asCustomBoolean] extension function.
 * @see asCustomBoolean
 */
class CustomBooleanEncoderBuilder<T : Any> internal constructor(private val value: T) {
    fun encoder(block: (T) -> Boolean): CustomBooleanDecoderBuilder<T> =
        CustomBooleanDecoderBuilder(value, block)
}

/**
 * Decoder builder for Boolean-encoded custom types.
 * Do not instantiate directly - obtained via [CustomBooleanEncoderBuilder.encoder].
 */
class CustomBooleanDecoderBuilder<T : Any> internal constructor(
    private val value: T,
    private val encoderFn: (T) -> Boolean
) {
    fun decoder(block: (Boolean) -> T): EncodableValue.CustomEncodeable<T, Boolean> =
        EncodableValue.CustomEncodeable(value, EncodableValue.Encoding.BOOLEAN, Converter(encoderFn, block))
}

// ========== Public API - Extension Functions ==========
// These are the ONLY public entry points for creating custom encodables

/**
 * Create a JSON object-encoded value with bidirectional conversion.
 *
 * Example:
 * ```kotlin
 * data class User(val name: String, val age: Int)
 * val user = User("Alice", 30)
 * val encodable = user.asJsonObject()
 *     .encoder { mapOf("name" to it.name, "age" to it.age) }
 *     .decoder { User(it["name"] as String, it["age"] as Int) }
 * ```
 */
fun <T : Any> T.asJsonObject(): JsonObjectEncoderBuilder<T> = JsonObjectEncoderBuilder(this)

/**
 * Create a String-encoded custom value with bidirectional conversion.
 *
 * Example:
 * ```kotlin
 * val uuid = UUID.randomUUID()
 * val encodable = uuid.asCustomString()
 *     .encoder { it.toString() }
 *     .decoder { UUID.fromString(it) }
 * ```
 */
fun <T : Any> T.asCustomString(): CustomStringEncoderBuilder<T> = CustomStringEncoderBuilder(this)

/**
 * Create an Int-encoded custom value with bidirectional conversion.
 *
 * Example:
 * ```kotlin
 * enum class Status { ACTIVE, INACTIVE }
 * val status = Status.ACTIVE
 * val encodable = status.asCustomInt()
 *     .encoder { it.ordinal }
 *     .decoder { Status.values()[it] }
 * ```
 */
fun <T : Any> T.asCustomInt(): CustomIntEncoderBuilder<T> = CustomIntEncoderBuilder(this)

/**
 * Create a Double-encoded custom value with bidirectional conversion.
 *
 * Example:
 * ```kotlin
 * val duration = Duration.ofMinutes(5)
 * val encodable = duration.asCustomDouble()
 *     .encoder { it.toMillis().toDouble() }
 *     .decoder { Duration.ofMillis(it.toLong()) }
 * ```
 */
fun <T : Any> T.asCustomDouble(): CustomDoubleEncoderBuilder<T> = CustomDoubleEncoderBuilder(this)

/**
 * Create a Boolean-encoded custom value with bidirectional conversion.
 *
 * Example:
 * ```kotlin
 * data class FeatureToggle(val enabled: Boolean)
 * val toggle = FeatureToggle(true)
 * val encodable = toggle.asCustomBoolean()
 *     .encoder { it.enabled }
 *     .decoder { FeatureToggle(it) }
 * ```
 */
fun <T : Any> T.asCustomBoolean(): CustomBooleanEncoderBuilder<T> = CustomBooleanEncoderBuilder(this)
