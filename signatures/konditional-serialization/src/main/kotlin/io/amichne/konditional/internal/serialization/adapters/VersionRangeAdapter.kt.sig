file=konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/adapters/VersionRangeAdapter.kt
package=io.amichne.konditional.internal.serialization.adapters
imports=com.squareup.moshi.FromJson,com.squareup.moshi.JsonDataException,com.squareup.moshi.JsonReader,com.squareup.moshi.JsonWriter,com.squareup.moshi.Moshi,com.squareup.moshi.ToJson,io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.context.Version,io.amichne.konditional.rules.versions.FullyBound,io.amichne.konditional.rules.versions.LeftBound,io.amichne.konditional.rules.versions.RightBound,io.amichne.konditional.rules.versions.Unbounded,io.amichne.konditional.rules.versions.VersionRange
type=io.amichne.konditional.internal.serialization.adapters.VersionRangeAdapter|kind=class|decl=class VersionRangeAdapter(moshi: Moshi)
type=io.amichne.konditional.internal.serialization.adapters.VersionRangeParts|kind=class|decl=private data class VersionRangeParts( val type: String?, val min: Version?, val max: Version?, )
fields:
- private val versionAdapter
methods:
- fun fromJson(reader: JsonReader): VersionRange
- fun toJson( writer: JsonWriter, value: VersionRange )
- private fun requirePart( value: Version?, field: String, type: String, ): Version
- private fun readVersionRangeParts(reader: JsonReader): VersionRangeParts
- private fun invalid(message: String): Nothing
