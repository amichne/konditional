package io.amichne.konditional.core.dsl

import io.amichne.konditional.core.types.json.JsonSchema
import io.amichne.konditional.core.types.json.JsonValue

/**
 * Builder for creating arrays of JSON objects with a shared schema.
 */
@JsonSchemaDsl
class JsonObjectArrayBuilder(private val schema: JsonSchema.ObjectSchema) {
    private val objects = mutableListOf<JsonValue.JsonObject>()

    /**
     * Adds a JSON object to the array using a builder DSL.
     */
    fun add(builder: JsonObjectBuilder.() -> Unit) {
        objects.add(buildJsonObject(schema, builder))
    }

    /**
     * Adds a pre-built JSON object to the array.
     */
    fun add(obj: JsonValue.JsonObject) {
        objects.add(obj)
    }

    /**
     * Builds the final JSON array.
     */
    fun build(): JsonValue.JsonArray = JsonValue.JsonArray(objects.toList(), schema)
}
