package io.amichne.konditional.internal.serialization.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
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

    @FromJson
    fun fromJson(reader: JsonReader): VersionRange =
        readVersionRangeParts(reader).let { parts ->
            // Parse at the boundary: construct typed domain objects with validation
            when (val type = parts.type) {
                VersionRange.Type.UNBOUNDED.name -> Unbounded()
                VersionRange.Type.MIN_BOUND.name ->
                    LeftBound(
                        requirePart(parts.min, field = "min", type = type),
                    )
                VersionRange.Type.MAX_BOUND.name ->
                    RightBound(
                        requirePart(parts.max, field = "max", type = type),
                    )
                VersionRange.Type.MIN_AND_MAX_BOUND.name ->
                    FullyBound(
                        requirePart(parts.min, field = "min", type = type),
                        requirePart(parts.max, field = "max", type = type),
                    )
                null -> invalid("Missing required 'type' field")
                else -> invalid("Unknown VersionRange type: $type")
            }
        }

    private fun requirePart(
        value: Version?,
        field: String,
        type: String,
    ): Version = value ?: invalid("$type requires '$field' field")

    private data class VersionRangeParts(
        val type: String?,
        val min: Version?,
        val max: Version?,
    )

    private fun readVersionRangeParts(reader: JsonReader): VersionRangeParts {
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

        return VersionRangeParts(
            type = type,
            min = min,
            max = max,
        )
    }

    private fun invalid(message: String): Nothing = throw JsonDataException(message)
}
