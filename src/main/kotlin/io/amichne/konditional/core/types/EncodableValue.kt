package io.amichne.konditional.core.types

import io.amichne.konditional.core.types.json.JsonArrayEncodeable
import io.amichne.konditional.core.types.json.JsonObjectEncodeable
import io.amichne.konditional.core.types.json.JsonValue
import kotlin.reflect.KClass

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
 * - DataClass (user-defined data classes implementing DataClassWithSchema)
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
        JSON_ARRAY(JsonValue.JsonArray::class),
        DATA_CLASS(DataClassWithSchema::class);

        companion object {
            /**
             * Parse a value into an EncodableValue with compile-time evidence.
             * Requires EncodableEvidence to prove the type is supported at compile-time.
             *
             * This function avoids unsafe casts by trusting the evidence system to ensure
             * type correctness at compile time. The evidence parameter proves that T is
             * a valid encodable type, so the conversions are guaranteed to be safe.
             */
            inline fun <reified T : Any> of(
                value: T,
                evidence: EncodableEvidence<T> = EncodableEvidence.get(),
            ): EncodableValue<T> {
                // The evidence system guarantees these conversions are safe,
                // so we only need one final cast at the end
                @Suppress("UNCHECKED_CAST")
                val encodable: EncodableValue<*> = when (evidence.encoding) {
                    BOOLEAN -> BooleanEncodeable(value as Boolean)
                    STRING -> StringEncodeable(value as String)
                    INTEGER -> IntEncodeable(value as Int)
                    DECIMAL -> DecimalEncodeable(value as Double)
                    ENUM -> {
                        // The evidence proves value is an Enum, so this cast is safe
                        val enumValue = value as Enum<*>
                        @Suppress("UNCHECKED_CAST")
                        EnumEncodeable(enumValue, enumValue::class as KClass<out Enum<*>>)
                    }
                    JSON_OBJECT -> {
                        // The evidence proves value is a JsonObject
                        val jsonObject = value as JsonValue.JsonObject
                        JsonObjectEncodeable(
                            jsonObject,
                            requireNotNull(jsonObject.schema) {
                                "JsonObject must have a schema attached for encoding"
                            }
                        )
                    }
                    JSON_ARRAY -> {
                        // The evidence proves value is a JsonArray
                        val jsonArray = value as JsonValue.JsonArray
                        JsonArrayEncodeable(
                            jsonArray,
                            requireNotNull(jsonArray.elementSchema) {
                                "JsonArray must have an element schema attached for encoding"
                            }
                        )
                    }
                    DATA_CLASS -> {
                        // The evidence proves value implements DataClassWithSchema
                        val dataClassWithSchema = value as DataClassWithSchema
                        DataClassEncodeable(dataClassWithSchema, dataClassWithSchema.schema)
                    }
                }
                // Safe cast because the when statement covers all cases and each creates the correct type
                return encodable as EncodableValue<T>
            }
        }
    }
}
