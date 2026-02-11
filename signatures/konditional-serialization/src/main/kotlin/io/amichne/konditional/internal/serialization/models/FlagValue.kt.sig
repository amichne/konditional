file=konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/models/FlagValue.kt
package=io.amichne.konditional.internal.serialization.models
imports=com.squareup.moshi.JsonClass,io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.core.ValueType,io.amichne.konditional.core.result.ParseResult,io.amichne.konditional.core.types.Konstrained,io.amichne.konditional.core.types.asObjectSchema,io.amichne.konditional.serialization.SchemaValueCodec,io.amichne.konditional.serialization.internal.toJsonValue,io.amichne.konditional.serialization.internal.toPrimitiveMap,io.amichne.kontracts.schema.ObjectSchema,io.amichne.kontracts.value.JsonObject
type=io.amichne.konditional.internal.serialization.models.FlagValue|kind=class|decl=sealed class FlagValue<out T : Any>
type=io.amichne.konditional.internal.serialization.models.BooleanValue|kind=class|decl=data class BooleanValue( override val value: Boolean, ) : FlagValue<Boolean>()
type=io.amichne.konditional.internal.serialization.models.StringValue|kind=class|decl=data class StringValue( override val value: String, ) : FlagValue<String>()
type=io.amichne.konditional.internal.serialization.models.IntValue|kind=class|decl=data class IntValue( override val value: Int, ) : FlagValue<Int>()
type=io.amichne.konditional.internal.serialization.models.DoubleValue|kind=class|decl=data class DoubleValue( override val value: Double, ) : FlagValue<Double>()
type=io.amichne.konditional.internal.serialization.models.EnumValue|kind=class|decl=data class EnumValue( override val value: String, val enumClassName: String, ) : FlagValue<String>()
type=io.amichne.konditional.internal.serialization.models.DataClassValue|kind=class|decl=data class DataClassValue( override val value: Map<String, Any?>, val dataClassName: String, ) : FlagValue<Map<String, Any?>>()
fields:
- abstract val value: T
methods:
- abstract fun toValueType(): ValueType @JsonClass(generateAdapter = true)
- fun validate(schema: ObjectSchema)
- fun <V : Any> extractValue(schema: ObjectSchema? = null): V
- override fun toValueType()
- override fun toValueType()
- override fun toValueType()
- override fun toValueType()
- override fun toValueType()
- override fun toValueType()
