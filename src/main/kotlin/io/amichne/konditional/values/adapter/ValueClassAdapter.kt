package io.amichne.konditional.values.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.rawType
import java.lang.reflect.Constructor
import java.lang.reflect.Type

/**
 * Value class json adapter
 *
 * [Pulled from remote](https://github.com/amichne/moshi-value-classes/blob/main/adapter/src/main/kotlin/io/amichne/moshi/extension/ValueClassJsonAdapter.kt)
 *
 * @param InlineT
 * @param ValueT
 * @property constructor
 * @property adapter
 * @constructor Create empty Value class json adapter
 *
 */
internal class ValueClassJsonAdapter<InlineT : Any, ValueT : Any> private constructor(
    private val constructor: Constructor<out InlineT>,
    private val adapter: JsonAdapter<ValueT>,
) : JsonAdapter<InlineT>() {
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any, ValueT> T.declaredProperty(): ValueT = with(this::class.java.declaredFields.first()) {
        isAccessible = true
        get(this@declaredProperty) as ValueT
    }

    @ToJson
    override fun toJson(
        writer: JsonWriter,
        value: InlineT?,
    ) {
        value?.let { writer.jsonValue(adapter.toJsonValue(it.declaredProperty())) }
    }

    @Suppress("TooGenericExceptionCaught")
    @FromJson
    override fun fromJson(reader: JsonReader): InlineT =
        reader.readJsonValue().let { jsonValue -> constructor.newInstance(adapter.fromJsonValue(jsonValue)) }

    object Factory : JsonAdapter.Factory {
        private val unsignedTypes = listOf(
            ULong::class.java,
            UInt::class.java,
            UShort::class.java,
            UByte::class.java,
        )

        override fun create(
            type: Type,
            annotations: Set<Annotation>,
            moshi: Moshi,
        ): JsonAdapter<Any>? = if (type.rawType.kotlin.isValue && !unsignedTypes.contains(type)) {
            val constructor = (type.rawType.declaredConstructors.first { it.parameterCount == 1 } as Constructor<*>)
                .also { it.isAccessible = true }
            val valueType = type.rawType.declaredFields[0].genericType
            ValueClassJsonAdapter(constructor = constructor, adapter = moshi.adapter(valueType))
        } else null
    }
}
