package io.amichne.konditional.internal.serialization.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import io.amichne.konditional.context.Version
import io.amichne.konditional.internal.SerializedTargeting
import io.amichne.konditional.rules.versions.FullyBound
import io.amichne.konditional.rules.versions.LeftBound
import io.amichne.konditional.rules.versions.RightBound
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

/** The v1 grammar accepts exactly the operands defined for each operation. */
internal object TargetingAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): SerializedTargeting {
        val fields = mutableSetOf<String>()
        var op = ""
        var key = ""
        var argument = ""
        var version = 0
        var values = emptyList<String>()
        var operands = emptyList<SerializedTargeting>()
        var min: Version? = null
        var max: Version? = null
        reader.beginObject()
        while (reader.hasNext()) {
            val field = reader.nextName()
            if (!fields.add(field)) throw JsonDataException("Duplicate targeting field '$field' at ${reader.path}")
            when (field) {
                "op" -> op = reader.readText()
                "key" -> key = reader.readText()
                "argument" -> argument = reader.readText()
                "version" -> version = reader.readPositiveInt()
                "values" -> values = reader.readArray { readText() }
                "operands" -> operands = reader.readArray { fromJson(this) }
                "min" -> min = reader.readVersion()
                "max" -> max = reader.readVersion()
                else -> throw JsonDataException("Unknown targeting field '$field' at ${reader.path}")
            }
        }
        reader.endObject()
        fun expect(vararg names: String) {
            if (fields != setOf("op", *names)) {
                throw JsonDataException("Invalid fields for targeting operation '$op': $fields at ${reader.path}")
            }
        }
        return when (op) {
            "allOf" -> SerializedTargeting.AllOf(operands).also { expect("operands") }
            "anyOf" -> SerializedTargeting.AnyOf(operands).also { expect("operands") }
            "locale" -> SerializedTargeting.Locale(values.toSet()).also { expect("values") }
            "platform" -> SerializedTargeting.Platform(values.toSet()).also { expect("values") }
            "axis" -> SerializedTargeting.Axis(key, values.toSet()).also { expect("key", "values") }
            "attributeIn" -> SerializedTargeting.Attribute(key, values).also { expect("key", "values") }
            "predicate" -> SerializedTargeting.Predicate(key, version, argument).also { expect("key", "version", "argument") }
            "version" -> {
                if (fields.any { it !in versionFields }) throw JsonDataException("Unexpected version fields: $fields")
                SerializedTargeting.Version(versionRange(min, max))
            }
            else -> throw JsonDataException("Unknown targeting operation '$op' at ${reader.path}")
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, expression: SerializedTargeting) {
        writer.beginObject()
        when (expression) {
            is SerializedTargeting.AllOf -> writer.writeGroup("allOf", expression.operands)
            is SerializedTargeting.AnyOf -> writer.writeGroup("anyOf", expression.operands)
            is SerializedTargeting.Locale -> writer.writeValues("locale", expression.ids)
            is SerializedTargeting.Platform -> writer.writeValues("platform", expression.ids)
            is SerializedTargeting.Axis -> {
                writer.writeValues("axis", expression.allowedIds)
                writer.name("key").value(expression.axisId)
            }
            is SerializedTargeting.Attribute -> {
                writer.writeValues("attributeIn", expression.operands)
                writer.name("key").value(expression.key)
            }
            is SerializedTargeting.Predicate -> {
                writer.name("op").value("predicate")
                writer.name("key").value(expression.key)
                writer.name("version").value(expression.version.toLong())
                writer.name("argument").value(expression.argument)
            }
            is SerializedTargeting.Version -> {
                writer.name("op").value("version")
                when (val range = expression.range) {
                    Unbounded -> Unit
                    is LeftBound -> writer.name("min").value(range.min.components())
                    is RightBound -> writer.name("max").value(range.max.components())
                    is FullyBound -> {
                        writer.name("min").value(range.min.components())
                        writer.name("max").value(range.max.components())
                    }
                }
            }
        }
        writer.endObject()
    }

    private fun JsonWriter.writeGroup(op: String, operands: List<SerializedTargeting>) {
        name("op").value(op)
        name("operands").beginArray()
        operands.forEach { toJson(this, it) }
        endArray()
    }

    private fun JsonWriter.writeValues(op: String, values: Collection<String>) {
        name("op").value(op)
        name("values").beginArray()
        values.forEach { value(it) }
        endArray()
    }
}

private val versionFields = setOf("op", "min", "max")

private fun JsonReader.readText(): String {
    if (peek() != JsonReader.Token.STRING) throw JsonDataException("Expected a string at $path")
    return nextString()
}

private fun JsonReader.readPositiveInt(): Int {
    if (peek() != JsonReader.Token.NUMBER) throw JsonDataException("Expected a predicate version number at $path")
    return nextString().toIntOrNull()?.takeIf { it > 0 }
        ?: throw JsonDataException("Expected a positive integer predicate version at $path")
}

private inline fun <T> JsonReader.readArray(element: JsonReader.() -> T): List<T> {
    beginArray()
    val items = buildList { while (hasNext()) add(element()) }
    endArray()
    return items
}

private fun Version.components(): String = "$major.$minor.$patch"

private fun JsonReader.readVersion(): Version {
    val text = readText()
    return Version.parse(text).getOrNull()?.takeIf { it.components() == text }
        ?: throw JsonDataException("Expected a major.minor.patch version at $path")
}

private fun versionRange(min: Version?, max: Version?): VersionRange = when {
    min != null && max != null -> FullyBound(min, max)
    min != null -> LeftBound(min)
    max != null -> RightBound(max)
    else -> Unbounded
}
