package io.amichne.konditional.core.types.json

import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.core.types.JsonEncodeable

/**
 * Encodeable wrapper for JSON array types.
 * Stores a JsonArray with its element definition for compile-time type safety and runtime validation.
 *
 * @param value The JsonArray value
 * @param elementSchema The definition defining the type of array elements
 */
data class JsonArrayEncodeable(
    override val value: JsonValue.JsonArray,
    val elementSchema: JsonSchema,
) : JsonEncodeable<JsonValue.JsonArray> {
    override val encoding: EncodableValue.Encoding = EncodableValue.Encoding.JSON_ARRAY

    companion object {
        /**
         * Creates a JsonArrayEncodeable with definition validation.
         */
        fun of(
            value: JsonValue.JsonArray,
            elementSchema: JsonSchema,
        ): JsonArrayEncodeable {
            // Validate the value against the definition
            val result = value.validate(JsonSchema.ArraySchema(elementSchema))
            if (result.isInvalid) {
                throw IllegalArgumentException(
                    "JsonArray does not match definition: ${result.getErrorMessage()}"
                )
            }
            return JsonArrayEncodeable(value, elementSchema)
        }
    }
}
