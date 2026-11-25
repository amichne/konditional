package io.amichne.konditional.internal.serialization.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

/** An omitted rule field selects its format-specific default. Explicit JSON null is invalid. */
@JsonQualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class TargetingField

internal object TargetingFieldAdapter : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        val remaining = Types.nextAnnotations(annotations, TargetingField::class.java) ?: return null
        val delegate = moshi.adapter<Any>(type, remaining)
        return object : JsonAdapter<Any>() {
            private val readerAdapter = delegate.nonNull()
            private val writerAdapter = delegate.nullSafe()

            override fun fromJson(reader: JsonReader): Any? = readerAdapter.fromJson(reader)

            override fun toJson(writer: JsonWriter, value: Any?) = writerAdapter.toJson(writer, value)
        }
    }
}
