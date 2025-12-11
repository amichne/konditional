package io.amichne.konditional.core.types

import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.result.utils.map
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.value.JsonValue

/**
 * Encodeable wrapper for custom data class configuration values.
 *
 * This class wraps a data class instance that implements [KotlinEncodeable] with an
 * [JsonSchema.ObjectSchema], providing automatic schema validation and JSON serialization support.
 *
 * The schema is generated at compile-time by the KSP processor from the
 * data class structure, ensuring type-safe configuration management.
 *
 * ## Type Constraint
 * [T] is constrained to [KotlinEncodeable]<[JsonSchema.ObjectSchema]>, which is aliased as
 * [JsonSchemaClass] for backwards compatibility and ergonomic usage.
 *
 * @param T The data class type, must implement [KotlinEncodeable]<[JsonSchema.ObjectSchema]>
 * @property value The data class instance
 * @property schema The JSON schema for validation (generated at compile-time)
 */
data class DataClassEncodeable<T : KotlinEncodeable<JsonSchema.ObjectSchema>>(
    override val value: T,
    val schema: JsonSchema.ObjectSchema
) : EncodableValue<T> {
    override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.DATA_CLASS

    /**
     * Converts this data class instance to a JsonValue.JsonObject for serialization.
     */
    fun toJsonValue(): JsonValue.JsonObject {
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
        inline fun <reified T : KotlinEncodeable<JsonSchema.ObjectSchema>> fromJsonValue(
            jsonObject: JsonValue.JsonObject,
            schema: JsonSchema.ObjectSchema
        ): ParseResult<DataClassEncodeable<T>> {
            return jsonObject.parseAs<T>().map { instance ->
                DataClassEncodeable(instance, schema)
            }
        }
    }
}
