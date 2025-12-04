package io.amichne.konditional.core.types

import io.amichne.konditional.core.types.json.JsonValue

/**
 * Type witness that provides evidence a type T can be encoded.
 * This is used to constrain Conditional and FeatureFlag to only work with supported types.
 *
 * Supported types: Boolean, String, Int, Double, Enum<E>, JsonValue.JsonObject, JsonValue.JsonArray, DataClassWithSchema
 *
 * This enforces the "parse, don't validate" principle by making illegal states (unsupported types)
 * unrepresentable at compile time.
 */
sealed interface EncodableEvidence<T : Any> {
    /**
     * The encoding type for this evidence.
     */
    val encoding: EncodableValue.Encoding

    companion object {
        /**
         * Obtains type evidence for a reified type T.
         * Throws IllegalArgumentException if T is not a supported encodable type.
         */
        inline fun <reified T : Any> get(): EncodableEvidence<T> {
            @Suppress("UNCHECKED_CAST")
            return when (T::class) {
                Boolean::class -> BooleanEvidence as EncodableEvidence<T>
                String::class -> StringEvidence as EncodableEvidence<T>
                Int::class -> IntEvidence as EncodableEvidence<T>
                Double::class -> DoubleEvidence as EncodableEvidence<T>
                JsonValue.JsonObject::class -> JsonObjectEvidence as EncodableEvidence<T>
                JsonValue.JsonArray::class -> JsonArrayEvidence as EncodableEvidence<T>
                else -> {
                    // Check if T is an enum type
                    if (T::class.java.isEnum) {
                        EnumEvidence<T>() as EncodableEvidence<T>
                    }
                    // Check if T implements DataClassWithSchema
                    else if (DataClassWithSchema::class.java.isAssignableFrom(T::class.java)) {
                        DataClassEvidence<T>() as EncodableEvidence<T>
                    } else {
                        throw IllegalArgumentException(
                            "Type ${T::class.qualifiedName} is not encodable. " +
                                "Supported types: Boolean, String, Int, Double, Enum, JsonObject, JsonArray, DataClassWithSchema"
                        )
                    }
                }
            }
        }

        /**
         * Checks if a reified type T is encodable without throwing.
         * Returns true if T is supported, false otherwise.
         */
        inline fun <reified T : Any> isEncodable(): Boolean {
            return when (T::class) {
                Boolean::class, String::class, Int::class, Double::class,
                JsonValue.JsonObject::class, JsonValue.JsonArray::class -> true
                else -> T::class.java.isEnum || DataClassWithSchema::class.java.isAssignableFrom(T::class.java)
            }
        }
    }

    object BooleanEvidence : EncodableEvidence<Boolean> {
        override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.BOOLEAN
    }

    object StringEvidence : EncodableEvidence<String> {
        override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.STRING
    }

    object IntEvidence : EncodableEvidence<Int> {
        override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.INTEGER
    }

    object DoubleEvidence : EncodableEvidence<Double> {
        override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.DECIMAL
    }

    /**
     * Evidence for enum types. Generic over the specific enum type E.
     * Each enum type gets its own evidence instance.
     */
    class EnumEvidence<E : Any> : EncodableEvidence<E> {
        override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.ENUM
    }

    /**
     * Evidence for JsonObject type.
     */
    object JsonObjectEvidence : EncodableEvidence<JsonValue.JsonObject> {
        override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.JSON_OBJECT
    }

    /**
     * Evidence for JsonArray type.
     */
    object JsonArrayEvidence : EncodableEvidence<JsonValue.JsonArray> {
        override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.JSON_ARRAY
    }

    /**
     * Evidence for data class types. Generic over the specific data class type T.
     * Each data class type gets its own evidence instance.
     */
    class DataClassEvidence<T : Any> : EncodableEvidence<T> {
        override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.DATA_CLASS
    }
}
