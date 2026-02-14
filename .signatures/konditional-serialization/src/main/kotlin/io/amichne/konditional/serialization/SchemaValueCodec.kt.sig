file=konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/SchemaValueCodec.kt
package=io.amichne.konditional.serialization
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.core.result.ParseError,io.amichne.konditional.core.result.parseFailure,io.amichne.konditional.core.types.Konstrained,io.amichne.kontracts.dsl.jsonObject,io.amichne.kontracts.dsl.jsonValue,io.amichne.kontracts.schema.ObjectSchema,io.amichne.kontracts.value.JsonBoolean,io.amichne.kontracts.value.JsonNull,io.amichne.kontracts.value.JsonNumber,io.amichne.kontracts.value.JsonObject,io.amichne.kontracts.value.JsonString,io.amichne.kontracts.value.JsonValue,kotlin.reflect.KClass,kotlin.reflect.KParameter,kotlin.reflect.full.memberProperties,kotlin.reflect.full.primaryConstructor
type=io.amichne.konditional.serialization.SchemaValueCodec|kind=object|decl=object SchemaValueCodec
type=io.amichne.konditional.serialization.ParameterResolution|kind=interface|decl=private sealed interface ParameterResolution
type=io.amichne.konditional.serialization.Value|kind=class|decl=data class Value(val value: Any?) : ParameterResolution
type=io.amichne.konditional.serialization.Skip|kind=object|decl=data object Skip : ParameterResolution
methods:
- fun <T : Any> encode(value: T, schema: ObjectSchema): JsonObject
- private fun encodeValue(value: Any): JsonValue
- fun <T : Any> decode(kClass: KClass<T>, json: JsonObject, schema: ObjectSchema): Result<T>
- fun <T : Any> decode(kClass: KClass<T>, json: JsonObject): Result<T>
- private fun decodeValue(kClass: KClass<*>?, json: JsonValue): Result<Any>
- private fun decodeValueForClass(kClass: KClass<*>, json: JsonValue): Result<Any>
- private fun decodeBuiltIn(kClass: KClass<*>, json: JsonValue): Result<Any>?
- private fun decodeEnum(kClass: KClass<*>, json: JsonValue): Result<Any>?
- private fun decodeCustomObject(kClass: KClass<*>, json: JsonValue): Result<Any>
- private fun <T : Any> decodeWithoutSchema(kClass: KClass<T>, json: JsonObject): Result<T>
