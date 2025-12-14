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
 *
 * Supports primitive types and user-defined types: Boolean, String, Int, Double, Enum, DataClass
 */
internal class FlagValueAdapter : JsonAdapter<FlagValue<*>>() {

    override fun toJson(
        writer: JsonWriter,
        value: FlagValue<*>?,
    ) {
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
            is FlagValue.DoubleValue -> {
                writer.name("type").value("DOUBLE")
                writer.name("value").value(value.value)
            }
            is FlagValue.EnumValue -> {
                writer.name("type").value("ENUM")
                writer.name("value").value(value.value)
                writer.name("enumClassName").value(value.enumClassName)
            }
            is FlagValue.DataClassValue -> {
                writer.name("type").value("DATA_CLASS")
                writer.name("dataClassName").value(value.dataClassName)
                writer.name("value")
                // Serialize the map as JSON object
                serializeMap(writer, value.value)
            }
        }
        writer.endObject()
    }

    override fun fromJson(reader: JsonReader): FlagValue<*> {
        var type: String? = null
        var boolValue: Boolean? = null
        var stringValue: String? = null
        var intValue: Int? = null
        var doubleValue: Double? = null
        var enumClassName: String? = null
        var dataClassName: String? = null
        var dataClassFields: Map<String, Any?>? = null

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
                                else -> intValue = numStr.toInt()
                            }
                        }
                        JsonReader.Token.BEGIN_OBJECT -> {
                            // For data class values, deserialize the map
                            dataClassFields = deserializeMap(reader)
                        }
                        else -> reader.skipValue()
                    }
                }
                "enumClassName" -> enumClassName = reader.nextString()
                "dataClassName" -> dataClassName = reader.nextString()
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
            "DOUBLE" -> doubleValue?.let { FlagValue.DoubleValue(it) }
                        ?: throw JsonDataException("DOUBLE type requires double value")
            "ENUM" -> {
                if (stringValue == null) {
                    throw JsonDataException("ENUM type requires string value (enum name)")
                }
                if (enumClassName == null) {
                    throw JsonDataException("ENUM type requires enumClassName field")
                }
                FlagValue.EnumValue(stringValue, enumClassName)
            }
            "DATA_CLASS" -> {
                if (dataClassFields == null) {
                    throw JsonDataException("DATA_CLASS type requires object value")
                }
                if (dataClassName == null) {
                    throw JsonDataException("DATA_CLASS type requires dataClassName field")
                }
                FlagValue.DataClassValue(dataClassFields, dataClassName)
            }
            null -> throw JsonDataException("Missing required 'type' field")
            else -> throw JsonDataException("Unknown FlagValue type: $type")
        }
    }

    /**
     * Serializes a map to JSON object.
     */
    private fun serializeMap(
        writer: JsonWriter,
        map: Map<String, Any?>,
    ) {
        writer.beginObject()
        map.forEach { (key, value) ->
            writer.name(key)
            serializeValue(writer, value)
        }
        writer.endObject()
    }

    /**
     * Serializes any value to JSON.
     */
    private fun serializeValue(
        writer: JsonWriter,
        value: Any?,
    ) {
        when (value) {
            null -> writer.nullValue()
            is Boolean -> writer.value(value)
            is String -> writer.value(value)
            is Number -> writer.value(value)
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                serializeMap(writer, value as Map<String, Any?>)
            }
            is List<*> -> {
                writer.beginArray()
                value.forEach { serializeValue(writer, it) }
                writer.endArray()
            }
            else -> throw JsonDataException("Unsupported value type: ${value::class.simpleName}")
        }
    }

    /**
     * Deserializes a JSON object to a map.
     */
    private fun deserializeMap(reader: JsonReader): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            val value = deserializeValue(reader)
            map[key] = value
        }
        reader.endObject()
        return map
    }

    /**
     * Deserializes any JSON value.
     */
    private fun deserializeValue(reader: JsonReader): Any? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Any?>()
                null
            }
            JsonReader.Token.BOOLEAN -> reader.nextBoolean()
            JsonReader.Token.STRING -> reader.nextString()
            JsonReader.Token.NUMBER -> {
                val numStr = reader.nextString()
                when {
                    numStr.contains('.') -> numStr.toDouble()
                    else -> numStr.toInt()
                }
            }
            JsonReader.Token.BEGIN_OBJECT -> deserializeMap(reader)
            JsonReader.Token.BEGIN_ARRAY -> {
                val list = mutableListOf<Any?>()
                reader.beginArray()
                while (reader.hasNext()) {
                    list.add(deserializeValue(reader))
                }
                reader.endArray()
                list
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    companion object {
        private val flagValueAdapter = FlagValueAdapter()

        /**
         * Factory for creating FlagValueAdapter instances.
         * This ensures the adapter is properly registered with Moshi for the FlagValue type.
         */
        val FACTORY: Factory = Factory { type, _, _ ->
            if (Types.getRawType(type) == FlagValue::class.java) {
                flagValueAdapter
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
