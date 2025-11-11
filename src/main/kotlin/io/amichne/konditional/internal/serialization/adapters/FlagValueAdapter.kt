package io.amichne.konditional.internal.serialization.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import io.amichne.konditional.internal.serialization.models.FlagValue
import java.lang.reflect.Type

/**
 * Custom Moshi adapter for the FlagValue sealed class.
 *
 * Serializes FlagValue subclasses with their type discriminator for type-safe deserialization.
 * Parse-don't-validate: Deserialization constructs typed domain objects at the boundary.
 */
internal class FlagValueAdapter : JsonAdapter<FlagValue<*>>() {

    override fun toJson(writer: JsonWriter, value: FlagValue<*>?) {
        if (value == null) {
            writer.nullValue()
            return
        }

        writer.beginObject()
        when (value) {
            is FlagValue.BooleanValue -> {
                writer.name("type").value("BOOLEAN")
                writer.name("value").value(value.value)
            }
            is FlagValue.StringValue -> {
                writer.name("type").value("STRING")
                writer.name("value").value(value.value)
            }
            is FlagValue.IntValue -> {
                writer.name("type").value("INT")
                writer.name("value").value(value.value)
            }
            is FlagValue.LongValue -> {
                writer.name("type").value("LONG")
                writer.name("value").value(value.value)
            }
            is FlagValue.DoubleValue -> {
                writer.name("type").value("DOUBLE")
                writer.name("value").value(value.value)
            }
            is FlagValue.JsonObjectValue -> {
                writer.name("type").value("JSON")
                writer.name("value")
                // Write the map as a JSON object
                writer.beginObject()
                value.value.forEach { (k, v) ->
                    writer.name(k)
                    when (v) {
                        null -> writer.nullValue()
                        is Boolean -> writer.value(v)
                        is String -> writer.value(v)
                        is Number -> writer.value(v)
                        is Map<*, *> -> writer.jsonValue(v.toString()) // Nested objects
                        else -> writer.value(v.toString())
                    }
                }
                writer.endObject()
            }
        }
        writer.endObject()
    }

    override fun fromJson(reader: JsonReader): FlagValue<*> {
        var type: String? = null
        var boolValue: Boolean? = null
        var stringValue: String? = null
        var intValue: Int? = null
        var longValue: Long? = null
        var doubleValue: Double? = null
        var jsonObjectValue: Map<String, Any?>? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "type" -> type = reader.nextString()
                "value" -> {
                    // Peek to determine the actual JSON type
                    when (reader.peek()) {
                        JsonReader.Token.BOOLEAN -> boolValue = reader.nextBoolean()
                        JsonReader.Token.STRING -> stringValue = reader.nextString()
                        JsonReader.Token.NUMBER -> {
                            // Read as string first to preserve type information
                            val numStr = reader.nextString()
                            when {
                                numStr.contains('.') -> doubleValue = numStr.toDouble()
                                numStr.toLongOrNull()?.let { it > Int.MAX_VALUE || it < Int.MIN_VALUE } == true ->
                                    longValue = numStr.toLong()
                                else -> intValue = numStr.toInt()
                            }
                        }
                        JsonReader.Token.BEGIN_OBJECT -> {
                            // Read JSON object
                            jsonObjectValue = readJsonObject(reader)
                        }
                        else -> reader.skipValue()
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        // Parse at the boundary: construct typed domain objects with validation
        return when (type) {
            "BOOLEAN" -> boolValue?.let { FlagValue.BooleanValue(it) }
                ?: throw JsonDataException("BOOLEAN type requires boolean value")
            "STRING" -> stringValue?.let { FlagValue.StringValue(it) }
                ?: throw JsonDataException("STRING type requires string value")
            "INT" -> intValue?.let { FlagValue.IntValue(it) }
                ?: throw JsonDataException("INT type requires int value")
            "LONG" -> longValue?.let { FlagValue.LongValue(it) }
                ?: throw JsonDataException("LONG type requires long value")
            "DOUBLE" -> doubleValue?.let { FlagValue.DoubleValue(it) }
                ?: throw JsonDataException("DOUBLE type requires double value")
            "JSON" -> jsonObjectValue?.let { FlagValue.JsonObjectValue(it) }
                ?: throw JsonDataException("JSON type requires object value")
            null -> throw JsonDataException("Missing required 'type' field")
            else -> throw JsonDataException("Unknown FlagValue type: $type")
        }
    }

    private fun readJsonObject(reader: JsonReader): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            val value = when (reader.peek()) {
                JsonReader.Token.BOOLEAN -> reader.nextBoolean()
                JsonReader.Token.STRING -> reader.nextString()
                JsonReader.Token.NUMBER -> {
                    val numStr = reader.nextString()
                    when {
                        numStr.contains('.') -> numStr.toDouble()
                        else -> numStr.toLong()
                    }
                }
                JsonReader.Token.BEGIN_OBJECT -> readJsonObject(reader) // Recursive
                JsonReader.Token.NULL -> reader.nextNull<Any?>()
                else -> {
                    reader.skipValue()
                    null
                }
            }
            map[key] = value
        }
        reader.endObject()
        return map
    }

    companion object {
        /**
         * Factory for creating FlagValueAdapter instances.
         * This ensures the adapter is properly registered with Moshi for the FlagValue type.
         */
        val FACTORY: Factory = Factory { type, _, _ ->
            if (Types.getRawType(type) == FlagValue::class.java) {
                FlagValueAdapter()
            } else {
                null
            }
        }
    }
}

/**
 * Helper object for type operations.
 */
private object Types {
    fun getRawType(type: Type): Class<*> {
        return when (type) {
            is Class<*> -> type
            is java.lang.reflect.ParameterizedType -> getRawType(type.rawType)
            else -> Any::class.java
        }
    }
}
