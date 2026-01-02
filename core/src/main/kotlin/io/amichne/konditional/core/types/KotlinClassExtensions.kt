package io.amichne.konditional.core.types

import io.amichne.konditional.serialization.SchemaValueCodec
import io.amichne.kontracts.schema.DoubleSchema
import io.amichne.kontracts.schema.IntSchema
import io.amichne.kontracts.value.JsonArray
import io.amichne.kontracts.value.JsonBoolean
import io.amichne.kontracts.value.JsonNull
import io.amichne.kontracts.value.JsonNumber
import io.amichne.kontracts.value.JsonObject
import io.amichne.kontracts.value.JsonString
import io.amichne.kontracts.value.JsonValue

/**
 * Converts any value to a JsonValue.
 *
 * This is a helper function that converts Kotlin types to their JsonValue equivalents.
 */
internal fun Any?.toJsonValue(): JsonValue = when (this) {
    null -> JsonNull
    is Boolean -> JsonBoolean(this)
    is String -> JsonString(this)
    is Int -> JsonNumber(this.toDouble())
    is Double -> JsonNumber(this)
    is Enum<*> -> JsonString(this.name)
    is Map<*, *> -> {
        @Suppress("UNCHECKED_CAST")
        JsonObject(
            fields =
                this.entries.associate { (rawKey, rawValue) ->
                    val key =
                        rawKey as? String
                            ?: error("JsonObject keys must be strings, got ${rawKey?.let { it::class.simpleName }}")
                    key to rawValue.toJsonValue()
                },
            schema = null,
        )
    }
    is KotlinEncodeable<*> -> SchemaValueCodec.encode(this, schema.asObjectSchema())
    is JsonValue -> this
    is List<*> -> JsonArray(map { it.toJsonValue() }, null)
    else -> throw IllegalArgumentException(
        "Unsupported type for JSON conversion: ${this::class.simpleName}"
    )
}

/**
 * Converts a JsonValue to a primitive value for serialization.
 *
 * This is used when converting JsonObjects to Maps for FlagValue serialization.
 */
internal fun JsonValue.toPrimitiveValue(): Any? = when (this) {
    is JsonNull -> null
    is JsonBoolean -> value
    is JsonString -> value
    is JsonNumber -> value
    is JsonObject ->
        schema?.let { s ->
            fields.mapValues { (k, v) ->
                val fieldSchema = s.fields[k]?.schema
                when {
                    v is JsonNumber && fieldSchema is IntSchema -> v.toInt()
                    v is JsonNumber && fieldSchema is DoubleSchema -> v.toDouble()
                    else -> v.toPrimitiveValue()
                }
            }
        } ?: fields.mapValues { (_, v) -> v.toPrimitiveValue() }
    is JsonArray -> elements.map { it.toPrimitiveValue() }
}
