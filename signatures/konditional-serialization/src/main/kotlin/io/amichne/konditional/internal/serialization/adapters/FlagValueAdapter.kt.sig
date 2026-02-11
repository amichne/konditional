file=konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/adapters/FlagValueAdapter.kt
package=io.amichne.konditional.internal.serialization.adapters
imports=com.squareup.moshi.JsonAdapter,com.squareup.moshi.JsonDataException,com.squareup.moshi.JsonReader,com.squareup.moshi.JsonWriter,com.squareup.moshi.Moshi,io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.internal.serialization.models.FlagValue,java.lang.reflect.ParameterizedType,java.lang.reflect.Type
type=io.amichne.konditional.internal.serialization.adapters.FlagValueAdapter|kind=class|decl=class FlagValueAdapter : JsonAdapter<FlagValue<*>>()
type=io.amichne.konditional.internal.serialization.adapters.FlagValueAdapterFactory|kind=object|decl=object FlagValueAdapterFactory : JsonAdapter.Factory
type=io.amichne.konditional.internal.serialization.adapters.FlagValueParts|kind=class|decl=private data class FlagValueParts( val type: String?, val value: Any?, val enumClassName: String?, val dataClassName: String?, )
fields:
- private val flagValueAdapter
methods:
- override fun toJson( writer: JsonWriter, value: FlagValue<*>?, )
- override fun fromJson(reader: JsonReader): FlagValue<*>
- override fun create( type: Type, annotations: Set<Annotation?>, moshi: Moshi, ): JsonAdapter<*>?
- private fun getRawType(type: Type): Class<*>
