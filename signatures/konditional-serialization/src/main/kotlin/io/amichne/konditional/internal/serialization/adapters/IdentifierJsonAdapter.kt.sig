file=konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/adapters/IdentifierJsonAdapter.kt
package=io.amichne.konditional.internal.serialization.adapters
imports=com.squareup.moshi.JsonAdapter,com.squareup.moshi.JsonDataException,com.squareup.moshi.JsonReader,com.squareup.moshi.JsonWriter,com.squareup.moshi.Moshi,io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.values.FeatureId,java.lang.reflect.Type
type=io.amichne.konditional.internal.serialization.adapters.IdentifierJsonAdapter|kind=object|decl=object IdentifierJsonAdapter : JsonAdapter.Factory
type=io.amichne.konditional.internal.serialization.adapters.FeatureIdAdapter|kind=object|decl=private object FeatureIdAdapter : JsonAdapter<FeatureId>()
methods:
- override fun create( type: Type, annotations: Set<Annotation>, moshi: Moshi, ): JsonAdapter<*>?
- override fun toJson( writer: JsonWriter, value: FeatureId?, )
- override fun fromJson(reader: JsonReader): FeatureId
