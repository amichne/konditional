package io.amichne.konditional.core.types.json

import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.core.types.JsonEncodeable
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.value.JsonObject

/**
 * Encodeable wrapper for JSON object types.
 * Stores a JsonObject with its schema for compile-time type safety and runtime validation.
 *
 * @param value The JsonObject value
 * @param schema The schema defining the structure and types of this object
 */
data class JsonObjectEncodeable(
    override val value: JsonObject,
    val schema: ObjectSchema,
) : JsonEncodeable<JsonObject> {
    override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.JSON_OBJECT

    companion object {
        /**
         * Creates a JsonObjectEncodeable with schema validation.
         */
        fun of(
            value: JsonObject,
            schema: ObjectSchema,
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
