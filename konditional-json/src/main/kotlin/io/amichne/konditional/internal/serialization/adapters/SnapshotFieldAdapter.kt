package io.amichne.konditional.internal.serialization.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.internal.serialization.models.SerializableFlag
import io.amichne.konditional.internal.serialization.models.SerializableRule
import java.lang.reflect.Type

/** Apply the snapshot field types before Moshi can coerce a token or admit a null collection element. */
@OptIn(KonditionalInternalApi::class)
internal object SnapshotFieldAdapter : JsonAdapter.Factory {
    private val stringSet = Types.newParameterizedType(Set::class.java, String::class.java)

    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (annotations.isNotEmpty()) return null
        return when (type) {
            SerializableFlag::class.java, SerializableRule::class.java -> moshi.nextAdapter<Any>(this, type, annotations).nonNull()
            stringSet -> Identifiers
            String::class.java -> typedScalar(moshi, type, JsonReader.Token.STRING)
            Double::class.java, Double::class.javaObjectType,
            Long::class.java, Long::class.javaObjectType -> typedScalar(moshi, type, JsonReader.Token.NUMBER)
            else -> null
        }
    }

    private fun typedScalar(moshi: Moshi, type: Type, token: JsonReader.Token): JsonAdapter<Any> {
        val delegate = moshi.nextAdapter<Any>(this, type, emptySet())
        return object : JsonAdapter<Any>() {
            override fun fromJson(reader: JsonReader): Any? {
                if (reader.peek() != token) throw JsonDataException("Expected $token at ${reader.path}")
                return delegate.fromJson(reader)
            }

            override fun toJson(writer: JsonWriter, value: Any?) = delegate.toJson(writer, value)
        }.nullSafe()
    }

    private object Identifiers : JsonAdapter<Set<String>>() {
        override fun fromJson(reader: JsonReader): Set<String> = buildSet {
            reader.beginArray()
            while (reader.hasNext()) {
                if (reader.peek() != JsonReader.Token.STRING) throw JsonDataException("Expected identifier at ${reader.path}")
                add(reader.nextString())
            }
            reader.endArray()
        }

        override fun toJson(writer: JsonWriter, value: Set<String>?) {
            writer.beginArray()
            requireNotNull(value).forEach { writer.value(it) }
            writer.endArray()
        }
    }
}
