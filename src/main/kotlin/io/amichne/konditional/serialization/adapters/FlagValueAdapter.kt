package io.amichne.konditional.serialization.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import io.amichne.konditional.serialization.models.FlagValue
import java.lang.reflect.Type

/**
 * Custom Moshi adapter for the FlagValue sealed class.
 *
 * Serializes FlagValue subclasses with their type discriminator for type-safe deserialization.
 * Parse-don't-validate: Deserialization constructs typed domain objects at the boundary.
 *
 * Supports:
 * - Primitives: BOOLEAN, STRING, INT, LONG, DOUBLE
 * - Complex: JSON (objects as Map<String, Any?>)
 */
class FlagValueAdapter : JsonAdapter<FlagValue<*>>() {

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
                // Serialize the map as a JSON object
                writeJsonObject(writer, value.value)
            }
        }
        writer.endObject()
    }

    /**
     * Writes a Map<String, Any?> as a JSON object.
     */
    private fun writeJsonObject(writer: JsonWriter, map: Map<String, Any?>) {
        writer.beginObject()
        for ((key, value) in map) {
            writer.name(key)
            writeAnyValue(writer, value)
        }
        writer.endObject()
    }

    /**
     * Writes any value recursively (for nested objects, arrays, etc.).
     */
    private fun writeAnyValue(writer: JsonWriter, value: Any?) {
        when (value) {
            null -> writer.nullValue()
            is Boolean -> writer.value(value)
            is Number -> writer.value(value)
            is String -> writer.value(value)
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                writeJsonObject(writer, value as Map<String, Any?>)
            }
            is List<*> -> {
                writer.beginArray()
                for (item in value) {
                    writeAnyValue(writer, item)
                }
                writer.endArray()
            }
            else -> throw JsonDataException("Unsupported JSON value type: ${value::class.simpleName}")
        }
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
                            // Parse JSON object
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

    /**
     * Reads a JSON object as a Map<String, Any?>.
     */
    private fun readJsonObject(reader: JsonReader): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            val value = readAnyValue(reader)
            result[key] = value
        }
        reader.endObject()
        return result
    }

    /**
     * Reads any JSON value recursively.
     */
    private fun readAnyValue(reader: JsonReader): Any? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull()
                null
            }
            JsonReader.Token.BOOLEAN -> reader.nextBoolean()
            JsonReader.Token.NUMBER -> {
                // Try to preserve numeric type
                val numStr = reader.nextString()
                when {
                    numStr.contains('.') -> numStr.toDouble()
                    numStr.toLongOrNull()?.let { it > Int.MAX_VALUE || it < Int.MIN_VALUE } == true ->
                        numStr.toLong()
                    else -> numStr.toInt()
                }
            }
            JsonReader.Token.STRING -> reader.nextString()
            JsonReader.Token.BEGIN_OBJECT -> readJsonObject(reader)
            JsonReader.Token.BEGIN_ARRAY -> {
                val list = mutableListOf<Any?>()
                reader.beginArray()
                while (reader.hasNext()) {
                    list.add(readAnyValue(reader))
                }
                reader.endArray()
                list
            }
            else -> throw JsonDataException("Unexpected token: ${reader.peek()}")
        }
    }

    companion object {
        /**
         * Factory for creating FlagValueAdapter instances.
         * This ensures the adapter is properly registered with Moshi for the FlagValue type.
         */
        val FACTORY: JsonAdapter.Factory = JsonAdapter.Factory { type, _, _ ->
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
