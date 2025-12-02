package io.amichne.konditional.core.types

/**
 * Type witness that provides evidence a type T can be encoded.
 * This is used to constrain Conditional and FeatureFlag to only work with supported primitive types.
 *
 * Supported types: Boolean, String, Int, Double
 *
 * This enforces the "parseUnsafe, don't validate" principle by making illegal states (unsupported types)
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
                else -> throw IllegalArgumentException(
                    "Type ${T::class.qualifiedName} is not encodable. " +
                        "Supported types: Boolean, String, Int, Double"
                )
            }
        }

        /**
         * Checks if a reified type T is encodable without throwing.
         * Returns true if T is supported, false otherwise.
         */
        inline fun <reified T : Any> isEncodable(): Boolean {
            return when (T::class) {
                Boolean::class, String::class, Int::class, Double::class -> true
                else -> false
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
}
