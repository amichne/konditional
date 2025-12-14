package io.amichne.konditional.core.types

import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.result.utils.map
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.value.JsonObject

/**
 * Encodeable wrapper for custom data class configuration values.
 *
 * This class wraps a data class instance that implements [KotlinEncodeable] with an
 * [ObjectSchema], providing automatic schema validation and JSON serialization support.
 *
 * The schema is generated at compile-time by the KSP processor from the
 * data class structure, ensuring type-safe configuration management.
 *
 * ## Type Constraint
 * [T] is constrained to [KotlinEncodeable]<[ObjectSchema]>, which is aliased as
 * [JsonSchemaClass] for backwards compatibility and ergonomic usage.
 *
 * @param T The data class type, must implement [KotlinEncodeable]<[ObjectSchema]>
 * @property value The data class instance
 * @property schema The JSON schema for validation (generated at compile-time)
 */
data class DataClassEncodeable<T : KotlinEncodeable<ObjectSchema>>(
    override val value: T,
    val schema: ObjectSchema,
) : EncodableValue<T> {
    override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.CUSTOM

    /**
     * Converts this data class instance to a JsonValue.JsonObject for serialization.
     */
    fun toJsonValue(): JsonObject {
        return value.toJsonValue(schema)
    }

    companion object {
        /**
         * Creates a DataClassEncodeable from a JsonValue.JsonObject.
         *
         * @param jsonObject The JSON object to parse
         * @param schema The schema to validate against
         * @return ParseResult containing either the data class instance or an error
         */
        inline fun <reified T : KotlinEncodeable<ObjectSchema>> fromJsonValue(
            jsonObject: JsonObject,
            schema: ObjectSchema,
        ): ParseResult<DataClassEncodeable<T>> {
            return jsonObject.parseAs<T>().map { instance ->
                DataClassEncodeable(instance, schema)
            }
        }
    }
}
