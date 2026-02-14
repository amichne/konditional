package io.amichne.konditional.serialization

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.parseFailure
import io.amichne.konditional.core.types.Konstrained
import io.amichne.kontracts.dsl.jsonObject
import io.amichne.kontracts.dsl.jsonValue
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
 * driven by the schema definition. Any type implementing Konstrained<ObjectSchema> can be
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

        return jsonObject {
            this.schema = schema
            fields(fields)
        }
    }

    private fun encodeValue(value: Any): JsonValue =
        when (value) {
            is Boolean -> jsonValue { boolean(value) }
            is String -> jsonValue { string(value) }
            is Int -> jsonValue { number(value) }
            is Double -> jsonValue { number(value) }
            is Enum<*> -> jsonValue { string(value.name) }
            is Konstrained<*> -> {
                val schema =
                    value.schema as? ObjectSchema
                        ?: error("Konstrained must have ObjectSchema, got ${value.schema::class.simpleName}")
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
    fun <T : Any> decode(kClass: KClass<T>, json: JsonObject, schema: ObjectSchema): Result<T> {
        val constructor =
            kClass.primaryConstructor
                ?: return parseFailure(
                    ParseError.InvalidSnapshot(
                        "${kClass.qualifiedName} must have a primary constructor for deserialization",
                    ),
                )

        val parameterMap = mutableMapOf<KParameter, Any?>()

        for (param in constructor.parameters) {
            val fieldName =
                param.name
                    ?: return parseFailure(
                        ParseError.InvalidSnapshot("Constructor parameter missing name in ${kClass.qualifiedName}"),
                    )

            val fieldSchema = schema.fields[fieldName]
            val jsonValue = json.fields[fieldName]

            val value =
                when {
                    jsonValue != null && jsonValue !is JsonNull -> {
                        val decoded = decodeValue(param.type.classifier as? KClass<*>, jsonValue)
                        if (decoded.isFailure) {
                            return Result.failure(
                                decoded.exceptionOrNull()
                                    ?: IllegalStateException("Unknown decode failure for field '$fieldName'"),
                            )
                        }
                        decoded.getOrThrow()
                    }

                    fieldSchema?.defaultValue != null -> fieldSchema.defaultValue
                    param.isOptional -> continue
                    fieldSchema?.required == true ->
                        return parseFailure(
                            ParseError.InvalidSnapshot(
                                "Required field '$fieldName' missing in JSON for ${kClass.qualifiedName}",
                            ),
                        )

                    else ->
                        return parseFailure(
                            ParseError.InvalidSnapshot(
                                "Field '$fieldName' missing in JSON and has no default in schema",
                            ),
                        )
                }

            parameterMap[param] = value
        }

        return runCatching { constructor.callBy(parameterMap) }
            .fold(
                onSuccess = { Result.success(it) },
                onFailure = { error ->
                    parseFailure(
                        ParseError.InvalidSnapshot(
                            "Failed to instantiate ${kClass.qualifiedName}: ${error.message}",
                        ),
                    )
                },
            )
    }

    /**
     * Decodes JsonObject to an instance using an extractable schema if present.
     * Falls back to constructor-based decoding when no schema is available.
     */
    fun <T : Any> decode(kClass: KClass<T>, json: JsonObject): Result<T> =
        extractSchema(kClass)
            ?.let { schema -> decode(kClass, json, schema) }
            ?: decodeWithoutSchema(kClass, json)

    private fun decodeValue(kClass: KClass<*>?, json: JsonValue): Result<Any> =
        kClass?.let { decodeValueForClass(it, json) }
            ?: parseFailure(ParseError.InvalidSnapshot("Cannot decode value without type information"))

    private fun decodeValueForClass(kClass: KClass<*>, json: JsonValue): Result<Any> =
        decodeBuiltIn(kClass, json)
            ?: decodeEnum(kClass, json)
            ?: decodeCustomObject(kClass, json)

    private fun decodeBuiltIn(kClass: KClass<*>, json: JsonValue): Result<Any>? =
        when (kClass) {
            Boolean::class ->
                when (json) {
                    is JsonBoolean -> Result.success(json.value)
                    else ->
                        parseFailure(
                            ParseError.InvalidSnapshot("Expected JsonBoolean, got ${json::class.simpleName}"),
                        )
                }

            String::class ->
                when (json) {
                    is JsonString -> Result.success(json.value)
                    else ->
                        parseFailure(
                            ParseError.InvalidSnapshot("Expected JsonString, got ${json::class.simpleName}"),
                        )
                }

            Int::class ->
                when (json) {
                    is JsonNumber -> Result.success(json.toInt())
                    else ->
                        parseFailure(
                            ParseError.InvalidSnapshot("Expected JsonNumber, got ${json::class.simpleName}"),
                        )
                }

            Double::class ->
                when (json) {
                    is JsonNumber -> Result.success(json.toDouble())
                    else ->
                        parseFailure(
                            ParseError.InvalidSnapshot("Expected JsonNumber, got ${json::class.simpleName}"),
                        )
                }

            else -> null
        }

    private fun decodeEnum(kClass: KClass<*>, json: JsonValue): Result<Any>? =
        if (!kClass.java.isEnum) {
            null
        } else {
            when (json) {
                is JsonString -> {
                    @Suppress("UNCHECKED_CAST")
                    val enumClass = kClass.java as Class<out Enum<*>>
                    val enumValue = enumClass.enumConstants.find { it.name == json.value }
                    if (enumValue != null) {
                        Result.success(enumValue)
                    } else {
                        parseFailure(
                            ParseError.InvalidSnapshot(
                                "Unknown enum constant '${json.value}' for ${kClass.simpleName}",
                            ),
                        )
                    }
                }

                else ->
                    parseFailure(
                        ParseError.InvalidSnapshot("Expected JsonString for enum, got ${json::class.simpleName}"),
                    )
            }
        }

    private fun decodeCustomObject(kClass: KClass<*>, json: JsonValue): Result<Any> =
        when (json) {
            is JsonObject -> decode(kClass, json)
            else ->
                parseFailure(
                    ParseError.InvalidSnapshot(
                        "Expected JsonObject for custom type, got ${json::class.simpleName}",
                    ),
                )
        }

    private fun <T : Any> decodeWithoutSchema(kClass: KClass<T>, json: JsonObject): Result<T> =
        kClass.primaryConstructor
            ?.let { constructor ->
                val parametersResult =
                    buildParameterMap(constructor, json, kClass, ::decodeValue)

                if (parametersResult.isSuccess) {
                    runCatching { constructor.callBy(parametersResult.getOrThrow()) }
                        .fold(
                            onSuccess = { Result.success(it) },
                            onFailure = { error ->
                                parseFailure(
                                    ParseError.InvalidSnapshot(
                                        "Failed to instantiate ${kClass.qualifiedName}: ${error.message}",
                                    ),
                                )
                            },
                        )
                } else {
                    Result.failure(
                        parametersResult.exceptionOrNull()
                            ?: IllegalStateException("Unknown constructor parameter decode failure"),
                    )
                }
            }
            ?: parseFailure(
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
    decodeValue: (KClass<*>?, JsonValue) -> Result<Any>,
): Result<Map<KParameter, Any?>> {
    val result = mutableMapOf<KParameter, Any?>()
    for (param in constructor.parameters) {
        val resolved = resolveParameter(param, json, owner, decodeValue)
        if (resolved.isFailure) {
            return Result.failure(
                resolved.exceptionOrNull()
                    ?: IllegalStateException("Unknown parameter resolution failure"),
            )
        }
        when (val resolution = resolved.getOrThrow()) {
            is ParameterResolution.Value -> result[param] = resolution.value
            ParameterResolution.Skip -> Unit
        }
    }
    return Result.success(result)
}

private fun resolveParameter(
    param: KParameter,
    json: JsonObject,
    owner: KClass<*>,
    decodeValue: (KClass<*>?, JsonValue) -> Result<Any>,
): Result<ParameterResolution> {
    val fieldName = param.name
    val jsonValue = fieldName?.let { json.fields[it] }

    return when {
        fieldName == null ->
            parseFailure(
                ParseError.InvalidSnapshot(
                    "Constructor parameter missing name in ${owner.qualifiedName}",
                ),
            )

        jsonValue == null || jsonValue is JsonNull ->
            if (param.isOptional) {
                Result.success(ParameterResolution.Skip)
            } else {
                parseFailure(
                    ParseError.InvalidSnapshot(
                        "Field '$fieldName' missing in JSON and has no default for ${owner.qualifiedName}",
                    ),
                )
            }

        else ->
            decodeValue(param.type.classifier as? KClass<*>, jsonValue)
                .map { decoded -> ParameterResolution.Value(decoded) }
    }
}
