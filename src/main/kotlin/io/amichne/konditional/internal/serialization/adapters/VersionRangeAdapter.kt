package io.amichne.konditional.internal.serialization.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import io.amichne.konditional.context.Version
import io.amichne.konditional.rules.versions.FullyBound
import io.amichne.konditional.rules.versions.LeftBound
import io.amichne.konditional.rules.versions.RightBound
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

/**
 * Custom Moshi adapter for the VersionRange sealed class.
 *
 * This adapter directly serializes/deserializes the domain types without
 * requiring parallel "Serializable" representations. It maintains the exact
 * same JSON format for backward compatibility.
 *
 * Parse-don't-validate: Deserialization constructs typed domain objects with
 * validation at the boundary, making illegal states unrepresentable.
 */
internal class VersionRangeAdapter(moshi: Moshi) {
    private val versionAdapter = moshi.adapter(Version::class.java)

    @ToJson
    fun toJson(writer: JsonWriter, value: VersionRange?) {
        if (value == null) {
            writer.nullValue()
            return
        }

        writer.beginObject()
        when (value) {
            is Unbounded -> {
                writer.name("type").value("UNBOUNDED")
            }
            is LeftBound -> {
                writer.name("type").value("MIN_BOUND")
                writer.name("min")
                versionAdapter.toJson(writer, value.min)
            }
            is RightBound -> {
                writer.name("type").value("MAX_BOUND")
                writer.name("max")
                versionAdapter.toJson(writer, value.max)
            }
            is FullyBound -> {
                writer.name("type").value("MIN_AND_MAX_BOUND")
                writer.name("min")
                versionAdapter.toJson(writer, value.min)
                writer.name("max")
                versionAdapter.toJson(writer, value.max)
            }
        }
        writer.endObject()
    }

    @FromJson
    fun fromJson(reader: JsonReader): VersionRange {
        var type: String? = null
        var min: Version? = null
        var max: Version? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "type" -> type = reader.nextString()
                "min" -> min = versionAdapter.fromJson(reader)
                "max" -> max = versionAdapter.fromJson(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        // Parse at the boundary: construct typed domain objects with validation
        return when (type) {
            "UNBOUNDED" -> Unbounded
            "MIN_BOUND" -> min?.let { LeftBound(it) }
                ?: throw JsonDataException("MIN_BOUND requires 'min' field")
            "MAX_BOUND" -> max?.let { RightBound(it) }
                ?: throw JsonDataException("MAX_BOUND requires 'max' field")
            "MIN_AND_MAX_BOUND" -> {
                if (min != null && max != null) {
                    FullyBound(min, max)
                } else {
                    throw JsonDataException("MIN_AND_MAX_BOUND requires both 'min' and 'max' fields")
                }
            }
            null -> throw JsonDataException("Missing required 'type' field")
            else -> throw JsonDataException("Unknown VersionRange type: $type")
        }
    }
}
