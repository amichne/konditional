package io.amichne.konditional.core.types.json

import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.core.types.JsonEncodeable

/**
 * Encodeable wrapper for JSON object types.
 * Stores a JsonObject with its definition for compile-time type safety and runtime validation.
 *
 * @param value The JsonObject value
 * @param schema The definition defining the structure and types of this object
 */
data class JsonObjectEncodeable(
    override val value: JsonValue.JsonObject,
    val schema: JsonSchema.ObjectSchema,
) : JsonEncodeable<JsonValue.JsonObject> {
    override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.JSON_OBJECT

    companion object {
        /**
         * Creates a JsonObjectEncodeable with definition validation.
         */
        fun of(
            value: JsonValue.JsonObject,
            schema: JsonSchema.ObjectSchema,
        ): JsonObjectEncodeable {
            // Validate the value against the definition
            val result = value.validate(schema)
            if (result.isInvalid) {
                throw IllegalArgumentException(
                    "JsonObject does not match definition: ${result.getErrorMessage()}"
                )
            }
            return JsonObjectEncodeable(value, schema)
        }
    }
}
