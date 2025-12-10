package io.amichne.konditional.core.dsl.json

import io.amichne.konditional.core.types.json.JsonSchema

/**
 * Top-level function to create a JSON object schema using DSL.
 */
fun jsonObject(builder: JsonObjectSchemaBuilder.() -> Unit): JsonSchema.ObjectSchema {
    return JsonObjectSchemaBuilder().apply(builder).build()
}
