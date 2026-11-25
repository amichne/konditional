package io.amichne.konditional.internal.serialization.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import io.amichne.konditional.values.FeatureId
import io.amichne.konditional.values.RuleId
import java.lang.reflect.Type

/** Parse identifiers through their domain constructors, including Kotlin value-class invariants. */
internal object IdentifierJsonAdapter : JsonAdapter.Factory {
    private val featureId = identifier(FeatureId::parse, FeatureId::plainId)
    private val ruleId = identifier(::RuleId, RuleId::value)

    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? =
        if (annotations.isNotEmpty()) null else when (type) {
            FeatureId::class.java -> featureId
            RuleId::class.java -> ruleId
            else -> null
        }

    private fun <T : Any> identifier(parse: (String) -> T, encode: (T) -> String): JsonAdapter<T> =
        object : JsonAdapter<T>() {
            override fun toJson(writer: JsonWriter, value: T?) {
                writer.value(encode(requireNotNull(value)))
            }

            override fun fromJson(reader: JsonReader): T {
                val path = reader.path
                if (reader.peek() != JsonReader.Token.STRING) throw JsonDataException("Expected identifier string at $path")
                val raw = reader.nextString()
                return try {
                    parse(raw)
                } catch (invalid: IllegalArgumentException) {
                    throw JsonDataException("Invalid identifier at $path", invalid)
                }
            }
        }.nullSafe()
}
