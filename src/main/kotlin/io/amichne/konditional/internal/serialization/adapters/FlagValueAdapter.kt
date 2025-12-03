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
 * Supports primitive types and user-defined types: Boolean, String, Int, Double, Enum
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
            is FlagValue.DoubleValue -> {
                writer.name("type").value("DOUBLE")
                writer.name("value").value(value.value)
            }
            is FlagValue.EnumValue -> {
                writer.name("type").value("ENUM")
                writer.name("value").value(value.value)
                writer.name("enumClassName").value(value.enumClassName)
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
                        else -> reader.skipValue()
                    }
                }
                "enumClassName" -> enumClassName = reader.nextString()
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
            null -> throw JsonDataException("Missing required 'type' field")
            else -> throw JsonDataException("Unknown FlagValue type: $type")
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
