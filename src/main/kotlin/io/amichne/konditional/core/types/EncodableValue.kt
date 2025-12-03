package io.amichne.konditional.core.types

import kotlin.ranges.rangeTo
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
    private val decodeFn: (O) -> I,
) {
    fun encode(input: I): O = encodeFn(input)
    fun decode(output: O): I = decodeFn(output)
}

/**
 * Sealed interface representing encodable value types.
 *
 * Supports primitive types and user-defined types:
 * - Boolean
 * - String
 * - Int
 * - Double
 * - Enum (user-defined enum types)
 * - JsonObject (structured JSON objects with typed fields)
 * - JsonArray (homogeneous arrays)
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
        ENUM(Enum::class),
        JSON_OBJECT(JsonValue.JsonObject::class),
        JSON_ARRAY(JsonValue.JsonArray::class);

        companion object {
            /**
             * Parse a value into an EncodableValue with compile-time evidence.
             * Requires EncodableEvidence to prove the type is supported at compile-time.
             */
            inline fun <reified T : Any> of(
                value: T,
                evidence: EncodableEvidence<T> = EncodableEvidence.get(),
            ): EncodableValue<T> {
                @Suppress("UNCHECKED_CAST")
                return when (evidence.encoding) {
                    BOOLEAN -> BooleanEncodeable(value as Boolean)
                    STRING -> StringEncodeable(value as String)
                    INTEGER -> IntEncodeable(value as Int)
                    DECIMAL -> DecimalEncodeable(value as Double)
                    ENUM -> {
                        // For enum types, we need to create EnumEncodeable with the proper type
                        EnumEncodeable.fromString(value.javaClass.name, value::class as KClass<out Enum<*>>)
                    }
                    JSON_OBJECT -> JsonObjectEncodeable(value as JsonValue.JsonObject, requireNotNull(value.schema))
                    JSON_ARRAY -> JsonArrayEncodeable(value as JsonValue.JsonArray, requireNotNull(value.elementSchema))
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
        override val encoding: Encoding = Encoding.ENUM

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

    /**
     * Encodeable wrapper for JSON object types.
     * Stores a JsonObject with its schema for compile-time type safety and runtime validation.
     *
     * @param value The JsonObject value
     * @param schema The schema defining the structure and types of this object
     */
    data class JsonObjectEncodeable(
        override val value: JsonValue.JsonObject,
        val schema: JsonSchema.ObjectSchema,
    ) : EncodableValue<JsonValue.JsonObject> {
        override val encoding: Encoding = Encoding.JSON_OBJECT

        companion object {
            /**
             * Creates a JsonObjectEncodeable with schema validation.
             */
            fun of(
                value: JsonValue.JsonObject,
                schema: JsonSchema.ObjectSchema,
            ): JsonObjectEncodeable {
                // Validate the value against the schema
                val result = value.validate(schema)
                if (result.isInvalid) {
                    throw IllegalArgumentException(
                        "JsonObject does not match schema: ${result.getErrorMessage()}"
                    )
                }
                return JsonObjectEncodeable(value, schema)
            }
        }
    }

    /**
     * Encodeable wrapper for JSON array types.
     * Stores a JsonArray with its element schema for compile-time type safety and runtime validation.
     *
     * @param value The JsonArray value
     * @param elementSchema The schema defining the type of array elements
     */
    data class JsonArrayEncodeable(
        override val value: JsonValue.JsonArray,
        val elementSchema: JsonSchema,
    ) : EncodableValue<JsonValue.JsonArray> {
        override val encoding: Encoding = Encoding.JSON_ARRAY

        companion object {
            /**
             * Creates a JsonArrayEncodeable with schema validation.
             */
            fun of(
                value: JsonValue.JsonArray,
                elementSchema: JsonSchema,
            ): JsonArrayEncodeable {
                // Validate the value against the schema
                val result = value.validate(JsonSchema.ArraySchema(elementSchema))
                if (result.isInvalid) {
                    throw IllegalArgumentException(
                        "JsonArray does not match schema: ${result.getErrorMessage()}"
                    )
                }
                return JsonArrayEncodeable(value, elementSchema)
            }
        }
    }
}
