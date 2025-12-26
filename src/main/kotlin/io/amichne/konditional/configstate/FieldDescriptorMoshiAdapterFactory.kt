package io.amichne.konditional.configstate

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

/**
 * Moshi adapter for [FieldDescriptor]'s sealed interface polymorphism.
 *
 * The JSON payload includes a `kind` discriminator field whose value is a [FieldDescriptor.Kind] name.
 * This adapter uses that discriminator to select the correct concrete descriptor type for deserialization.
 */
object FieldDescriptorMoshiAdapterFactory : JsonAdapter.Factory {
    override fun create(
        type: Type,
        annotations: Set<Annotation>,
        moshi: Moshi,
    ): JsonAdapter<*>? {
        val rawType = Types.getRawType(type)
        return if (rawType == FieldDescriptor::class.java && annotations.isEmpty()) {
            FieldDescriptorJsonAdapter(moshi).nullSafe()
        } else {
            null
        }
    }
}

private class FieldDescriptorJsonAdapter(
    private val moshi: Moshi,
) : JsonAdapter<FieldDescriptor>() {
    private val booleanAdapter = moshi.adapter(BooleanDescriptor::class.java)
    private val enumOptionsAdapter = moshi.adapter(EnumOptionsDescriptor::class.java)
    private val numberRangeAdapter = moshi.adapter(NumberRangeDescriptor::class.java)
    private val semverConstraintsAdapter = moshi.adapter(SemverConstraintsDescriptor::class.java)
    private val stringConstraintsAdapter = moshi.adapter(StringConstraintsDescriptor::class.java)
    private val schemaRefAdapter = moshi.adapter(SchemaRefDescriptor::class.java)
    private val mapConstraintsAdapter = moshi.adapter(MapConstraintsDescriptor::class.java)

    override fun fromJson(reader: JsonReader): FieldDescriptor {
        val kind = peekKind(reader)
        val adapter =
            when (kind) {
                FieldDescriptor.Kind.BOOLEAN -> booleanAdapter
                FieldDescriptor.Kind.ENUM_OPTIONS -> enumOptionsAdapter
                FieldDescriptor.Kind.NUMBER_RANGE -> numberRangeAdapter
                FieldDescriptor.Kind.SEMVER_CONSTRAINTS -> semverConstraintsAdapter
                FieldDescriptor.Kind.STRING_CONSTRAINTS -> stringConstraintsAdapter
                FieldDescriptor.Kind.SCHEMA_REF -> schemaRefAdapter
                FieldDescriptor.Kind.MAP_CONSTRAINTS -> mapConstraintsAdapter
            }

        return adapter.fromJson(reader)
            ?: throw JsonDataException("FieldDescriptor adapter returned null for kind=$kind")
    }

    override fun toJson(
        writer: JsonWriter,
        value: FieldDescriptor?,
    ) {
        if (value == null) {
            writer.nullValue()
        } else {
            when (value) {
                is BooleanDescriptor -> booleanAdapter.toJson(writer, value)
                is EnumOptionsDescriptor -> enumOptionsAdapter.toJson(writer, value)
                is NumberRangeDescriptor -> numberRangeAdapter.toJson(writer, value)
                is SemverConstraintsDescriptor -> semverConstraintsAdapter.toJson(writer, value)
                is StringConstraintsDescriptor -> stringConstraintsAdapter.toJson(writer, value)
                is SchemaRefDescriptor -> schemaRefAdapter.toJson(writer, value)
                is MapConstraintsDescriptor -> mapConstraintsAdapter.toJson(writer, value)
            }
        }
    }

    private fun peekKind(reader: JsonReader): FieldDescriptor.Kind {
        val peekReader = reader.peekJson()
        val asAny =
            peekReader.readJsonValue()
                ?: throw JsonDataException("Expected FieldDescriptor JSON object but was null")

        val jsonObject = asAny as? Map<*, *> ?: throw JsonDataException("Expected FieldDescriptor JSON object but was ${asAny::class.simpleName}")
        val kindRaw = jsonObject["kind"] as? String ?: throw JsonDataException("FieldDescriptor JSON missing required 'kind' discriminator")

        return runCatching { FieldDescriptor.Kind.valueOf(kindRaw) }
            .getOrElse { throw JsonDataException("Unsupported FieldDescriptor kind='$kindRaw'") }
    }
}
