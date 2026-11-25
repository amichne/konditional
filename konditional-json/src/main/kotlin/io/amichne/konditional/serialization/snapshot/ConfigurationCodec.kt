@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization.snapshot

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.fact.FactSpec
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.parseFailure
import io.amichne.konditional.core.schema.CompiledNamespaceSchema
import io.amichne.konditional.internal.serialization.adapters.FactSpecAdapter
import io.amichne.konditional.internal.serialization.adapters.FlagValueAdapterFactory
import io.amichne.konditional.internal.serialization.adapters.SnapshotFieldAdapter
import io.amichne.konditional.internal.serialization.adapters.IdentifierJsonAdapter
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import okio.Buffer
import java.io.IOException

@JsonClass(generateAdapter = true)
internal data class SnapshotDocument(val format: SnapshotFormat, val snapshot: SerializableSnapshot)

internal enum class SnapshotFormat {
    @Json(name = "konditional/facts-v1") FACTS_V1,
}

/** A distinct root envelope prevents older readers from silently dropping fact conditions. */
internal object ConfigurationCodec {
    private val snapshotAdapter = Moshi.Builder()
        .add(SnapshotFieldAdapter)
        .add(IdentifierJsonAdapter)
        .add(FlagValueAdapterFactory)
        .add(FactSpec::class.java, FactSpecAdapter)
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(SnapshotDocument::class.java)
        .failOnUnknown()
        .indent("  ")

    fun encode(configuration: Configuration): String =
        snapshotAdapter.toJson(SnapshotDocument(SnapshotFormat.FACTS_V1, SerializableSnapshot.from(configuration)))

    fun encode(namespace: Namespace): String = encode(namespace.configuration)

    fun decode(json: String, namespace: Namespace): Result<Configuration> {
        val document = try {
            JsonReader.of(Buffer().writeUtf8(json)).use { reader ->
                // Moshi's generated adapters accept repeated properties. A repeated rule or condition
                // property could discard a constraint before the namespace schema sees it.
                reader.peekJson().use(::rejectDuplicateFields)
                val decoded = snapshotAdapter.fromJson(reader)
                if (reader.peek() != JsonReader.Token.END_DOCUMENT) {
                    throw JsonDataException("Unexpected trailing JSON")
                }
                decoded
            }
        } catch (invalid: JsonDataException) {
            return parseFailure(ParseError.invalidJson(invalid.message ?: "Invalid snapshot document"))
        } catch (malformed: IOException) {
            return parseFailure(ParseError.invalidJson(malformed.message ?: "Malformed JSON"))
        }
        return document?.snapshot?.toConfiguration(CompiledNamespaceSchema.from(namespace))
            ?: parseFailure(ParseError.invalidJson("Expected a snapshot document"))
    }

    private fun rejectDuplicateFields(reader: JsonReader) {
        when (reader.peek()) {
            JsonReader.Token.BEGIN_OBJECT -> {
                reader.beginObject()
                val seen = mutableSetOf<String>()
                while (reader.hasNext()) {
                    if (!seen.add(reader.nextName())) throw JsonDataException("Duplicate field at ${reader.path}")
                    rejectDuplicateFields(reader)
                }
                reader.endObject()
            }
            JsonReader.Token.BEGIN_ARRAY -> {
                reader.beginArray()
                while (reader.hasNext()) rejectDuplicateFields(reader)
                reader.endArray()
            }
            else -> reader.skipValue()
        }
    }
}
