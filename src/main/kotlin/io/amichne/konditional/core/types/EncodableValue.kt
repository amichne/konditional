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
                    DATA_CLASS -> {
                        // For data class types, get the schema from the instance
                        val dataClassWithSchema = value as DataClassWithSchema
                        DataClassEncodeable(dataClassWithSchema, dataClassWithSchema.schema)
                    }
                } as EncodableValue<T>
            }
        }
    }
}
