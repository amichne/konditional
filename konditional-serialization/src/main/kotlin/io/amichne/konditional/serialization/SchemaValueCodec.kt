package io.amichne.konditional.serialization

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.types.Konstrained
import io.amichne.konditional.core.types.asObjectSchema
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.value.JsonBoolean
import io.amichne.kontracts.value.JsonNull
import io.amichne.kontracts.value.JsonNumber
import io.amichne.kontracts.value.JsonObject
import io.amichne.kontracts.value.JsonString
import io.amichne.kontracts.value.JsonValue
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Schema-based serializer that uses reflection and ObjectSchema to encode/decode instances.
 *
 * Replaces the explicit TypeSerializer/SerializerRegistry pattern with automatic serialization
 * driven by the schema definition. Any type implementing Konstrained with object-like traits can be
 * serialized without additional registration.
 */
@KonditionalInternalApi
object SchemaValueCodec {

    /**
     * Encodes a value to JsonObject using its schema.
     */
    fun <T : Any> encode(value: T, schema: ObjectSchema): JsonObject {
        val fields =
            schema.fields.mapValues { (fieldName, _) ->
                val property =
                    value::class.memberProperties.find { it.name == fieldName }
                        ?: error("Property '$fieldName' not found on ${value::class.qualifiedName}")

                val propertyValue = property.call(value)

                when {
                    propertyValue == null -> JsonNull
                    else -> encodeValue(propertyValue)
                }
            }

        return JsonObject(fields, schema)
    }

    private fun encodeValue(value: Any): JsonValue =
        when (value) {
            is Boolean -> JsonBoolean(value)
            is String -> JsonString(value)
            is Int -> JsonNumber(value.toDouble())
            is Double -> JsonNumber(value)
            is Enum<*> -> JsonString(value.name)
            is Konstrained<*> -> {
                val schema =
                    runCatching { value.schema.asObjectSchema() }
                        .getOrElse {
                            error(
                                "Konstrained schema must be an object schema, got ${value.schema::class.simpleName}",
                            )
                        }
                encode(value, schema)
            }

            else ->
                error(
                    "Unsupported type for encoding: ${value::class.qualifiedName}. " +
                        "Type must implement Konstrained<ObjectSchema> or be a built-in type " +
                        "(Boolean, String, Int, Double, Enum).",
                )
        }

    /**
     * Decodes JsonObject to an instance using schema and reflection.
     */
    fun <T : Any> decode(kClass: KClass<T>, json: JsonObject, schema: ObjectSchema): ParseResult<T> {
        val constructor =
            kClass.primaryConstructor
                ?: return ParseResult.failure(
                    ParseError.InvalidSnapshot(
                        "${kClass.qualifiedName} must have a primary constructor for deserialization",
                    ),
                )

        val parameterMap = mutableMapOf<KParameter, Any?>()

        for (param in constructor.parameters) {
            val fieldName =
                param.name
                    ?: return ParseResult.failure(
                        ParseError.InvalidSnapshot("Constructor parameter missing name in ${kClass.qualifiedName}"),
                    )

            val fieldSchema = schema.fields[fieldName]
            val jsonValue = json.fields[fieldName]

            val value =
                when {
                    jsonValue != null && jsonValue !is JsonNull ->
                        when (val decoded = decodeValue(param.type.classifier as? KClass<*>, jsonValue)) {
                            is ParseResult.Success -> decoded.value
                            is ParseResult.Failure -> return decoded
                        }

                    fieldSchema?.defaultValue != null -> fieldSchema.defaultValue
                    param.isOptional -> continue
                    fieldSchema?.required == true ->
                        return ParseResult.failure(
                            ParseError.InvalidSnapshot(
                                "Required field '$fieldName' missing in JSON for ${kClass.qualifiedName}",
                            ),
                        )

                    else ->
                        return ParseResult.failure(
                            ParseError.InvalidSnapshot(
                                "Field '$fieldName' missing in JSON and has no default in schema",
                            ),
                        )
                }

            parameterMap[param] = value
        }

        return runCatching { ParseResult.success(constructor.callBy(parameterMap)) }
            .getOrElse { e ->
                ParseResult.failure(
                    ParseError.InvalidSnapshot(
                        "Failed to instantiate ${kClass.qualifiedName}: ${e.message}",
                    ),
                )
            }
    }

    /**
     * Decodes JsonObject to an instance using an extractable schema if present.
     * Falls back to constructor-based decoding when no schema is available.
     */
    fun <T : Any> decode(kClass: KClass<T>, json: JsonObject): ParseResult<T> =
        extractSchema(kClass)
            ?.let { schema -> decode(kClass, json, schema) }
            ?: decodeWithoutSchema(kClass, json)

    private fun decodeValue(kClass: KClass<*>?, json: JsonValue): ParseResult<Any> =
        kClass?.let { decodeValueForClass(it, json) }
            ?: ParseResult.failure(ParseError.InvalidSnapshot("Cannot decode value without type information"))

    private fun decodeValueForClass(kClass: KClass<*>, json: JsonValue): ParseResult<Any> =
        decodeBuiltIn(kClass, json)
            ?: decodeEnum(kClass, json)
            ?: decodeCustomObject(kClass, json)

    private fun decodeBuiltIn(kClass: KClass<*>, json: JsonValue): ParseResult<Any>? =
        when (kClass) {
            Boolean::class ->
                when (json) {
                    is JsonBoolean -> ParseResult.success(json.value)
                    else ->
                        ParseResult.failure(
                            ParseError.InvalidSnapshot("Expected JsonBoolean, got ${json::class.simpleName}"),
                        )
                }

            String::class ->
                when (json) {
                    is JsonString -> ParseResult.success(json.value)
                    else ->
                        ParseResult.failure(
                            ParseError.InvalidSnapshot("Expected JsonString, got ${json::class.simpleName}"),
                        )
                }

            Int::class ->
                when (json) {
                    is JsonNumber -> ParseResult.success(json.toInt())
                    else ->
                        ParseResult.failure(
                            ParseError.InvalidSnapshot("Expected JsonNumber, got ${json::class.simpleName}"),
                        )
                }

            Double::class ->
                when (json) {
                    is JsonNumber -> ParseResult.success(json.toDouble())
                    else ->
                        ParseResult.failure(
                            ParseError.InvalidSnapshot("Expected JsonNumber, got ${json::class.simpleName}"),
                        )
                }

            else -> null
        }

    private fun decodeEnum(kClass: KClass<*>, json: JsonValue): ParseResult<Any>? =
        if (!kClass.java.isEnum) {
            null
        } else {
            when (json) {
                is JsonString -> {
                    @Suppress("UNCHECKED_CAST")
                    val enumClass = kClass.java as Class<out Enum<*>>
                    val enumValue = enumClass.enumConstants.find { it.name == json.value }
                    if (enumValue != null) {
                        ParseResult.success(enumValue)
                    } else {
                        ParseResult.failure(
                            ParseError.InvalidSnapshot(
                                "Unknown enum constant '${json.value}' for ${kClass.simpleName}",
                            ),
                        )
                    }
                }

                else ->
                    ParseResult.failure(
                        ParseError.InvalidSnapshot("Expected JsonString for enum, got ${json::class.simpleName}"),
                    )
            }
        }

    private fun decodeCustomObject(kClass: KClass<*>, json: JsonValue): ParseResult<Any> =
        when (json) {
            is JsonObject -> decode(kClass, json)
            else ->
                ParseResult.failure(
                    ParseError.InvalidSnapshot(
                        "Expected JsonObject for custom type, got ${json::class.simpleName}",
                    ),
                )
        }

    private fun <T : Any> decodeWithoutSchema(kClass: KClass<T>, json: JsonObject): ParseResult<T> =
        kClass.primaryConstructor
            ?.let { constructor ->
                val parametersResult =
                    buildParameterMap(constructor, json, kClass, ::decodeValue)

                when (parametersResult) {
                    is ParseResult.Success ->
                        runCatching { ParseResult.success(constructor.callBy(parametersResult.value)) }
                            .getOrElse { e ->
                                ParseResult.failure(
                                    ParseError.InvalidSnapshot(
                                        "Failed to instantiate ${kClass.qualifiedName}: ${e.message}",
                                    ),
                                )
                            }
                    is ParseResult.Failure -> parametersResult
                }
            }
            ?: ParseResult.failure(
                ParseError.InvalidSnapshot(
                    "${kClass.qualifiedName} must have a primary constructor for deserialization",
                ),
            )

}

private sealed interface ParameterResolution {
    data class Value(val value: Any?) : ParameterResolution

    data object Skip : ParameterResolution
}

private fun <T : Any> buildParameterMap(
    constructor: kotlin.reflect.KFunction<T>,
    json: JsonObject,
    owner: KClass<*>,
    decodeValue: (KClass<*>?, JsonValue) -> ParseResult<Any>,
): ParseResult<Map<KParameter, Any?>> =
    constructor.parameters.fold(ParseResult.success(mutableMapOf<KParameter, Any?>())) { acc, param ->
        when (acc) {
            is ParseResult.Failure -> acc
            is ParseResult.Success ->
                resolveParameter(param, json, owner, decodeValue).let { resolved ->
                    when (resolved) {
                        is ParseResult.Failure -> resolved
                        is ParseResult.Success -> {
                            when (val resolution = resolved.value) {
                                is ParameterResolution.Value -> acc.value[param] = resolution.value
                                ParameterResolution.Skip -> Unit
                            }
                            ParseResult.success(acc.value)
                        }
                    }
                }
        }
    }

private fun resolveParameter(
    param: KParameter,
    json: JsonObject,
    owner: KClass<*>,
    decodeValue: (KClass<*>?, JsonValue) -> ParseResult<Any>,
): ParseResult<ParameterResolution> {
    val fieldName = param.name
    val jsonValue = fieldName?.let { json.fields[it] }

    return when {
        fieldName == null ->
            ParseResult.failure(
                ParseError.InvalidSnapshot(
                    "Constructor parameter missing name in ${owner.qualifiedName}",
                ),
            )

        jsonValue == null || jsonValue is JsonNull ->
            if (param.isOptional) {
                ParseResult.success(ParameterResolution.Skip)
            } else {
                ParseResult.failure(
                    ParseError.InvalidSnapshot(
                        "Field '$fieldName' missing in JSON and has no default for ${owner.qualifiedName}",
                    ),
                )
            }

        else ->
            decodeValue(param.type.classifier as? KClass<*>, jsonValue)
                .let { decoded ->
                    when (decoded) {
                        is ParseResult.Success -> ParseResult.success(ParameterResolution.Value(decoded.value))
                        is ParseResult.Failure -> decoded
                    }
                }
    }
}
