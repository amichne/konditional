file=konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/SchemaValueCodec.kt
package=io.amichne.konditional.serialization
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.core.result.ParseError,io.amichne.konditional.core.result.parseFailure,io.amichne.konditional.core.types.Konstrained,io.amichne.konditional.core.types.asObjectSchema,io.amichne.kontracts.dsl.jsonArray,io.amichne.kontracts.dsl.jsonObject,io.amichne.kontracts.dsl.jsonValue,io.amichne.kontracts.schema.ArraySchema,io.amichne.kontracts.schema.BooleanSchema,io.amichne.kontracts.schema.DoubleSchema,io.amichne.kontracts.schema.IntSchema,io.amichne.kontracts.schema.ObjectSchema,io.amichne.kontracts.schema.ObjectTraits,io.amichne.kontracts.schema.StringSchema,io.amichne.kontracts.value.JsonArray,io.amichne.kontracts.value.JsonBoolean,io.amichne.kontracts.value.JsonNull,io.amichne.kontracts.value.JsonNumber,io.amichne.kontracts.value.JsonObject,io.amichne.kontracts.value.JsonString,io.amichne.kontracts.value.JsonValue,kotlin.reflect.KClass,kotlin.reflect.KParameter,kotlin.reflect.full.memberProperties,kotlin.reflect.full.primaryConstructor
type=io.amichne.konditional.serialization.SchemaValueCodec|kind=object|decl=object SchemaValueCodec
type=io.amichne.konditional.serialization.Value|kind=class|decl=data class Value(val value: Any?) : ParameterResolution
type=io.amichne.konditional.serialization.Skip|kind=object|decl=data object Skip : ParameterResolution
methods:
- fun <T : Any> encode(value: T, schema: ObjectSchema): JsonObject
- fun encodeKonstrained(konstrained: Konstrained<*>): JsonValue
- fun <T : Any> decodeKonstrainedPrimitive(kClass: KClass<T>, rawValue: Any): Result<T>
- fun <T : Any> decode(kClass: KClass<T>, json: JsonObject, schema: ObjectSchema): Result<T>
- fun <T : Any> decode(kClass: KClass<T>, json: JsonObject): Result<T>
