file=konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/adapters/ValueClassAdapterFactory.kt
package=io.amichne.konditional.internal.serialization.adapters
imports=com.squareup.moshi.JsonAdapter,com.squareup.moshi.JsonDataException,com.squareup.moshi.JsonReader,com.squareup.moshi.JsonWriter,com.squareup.moshi.Moshi,com.squareup.moshi.rawType,io.amichne.konditional.api.KonditionalInternalApi,java.lang.reflect.Constructor,java.lang.reflect.Type
type=io.amichne.konditional.internal.serialization.adapters.ValueClassAdapterFactory|kind=object|decl=object ValueClassAdapterFactory : JsonAdapter.Factory
type=io.amichne.konditional.internal.serialization.adapters.ValueClassAdapter|kind=class|decl=private class ValueClassAdapter<InlineT : Any, ValueT : Any>( val constructor: Constructor<out InlineT>, val adapter: JsonAdapter<ValueT>, ) : JsonAdapter<InlineT>()
methods:
- override fun toJson( writer: JsonWriter, inlineT: InlineT?, )
- override fun fromJson(reader: JsonReader): InlineT
