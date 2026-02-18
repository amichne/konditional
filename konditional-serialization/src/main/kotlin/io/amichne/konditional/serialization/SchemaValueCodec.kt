package io.amichne.konditional.serialization

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.parseFailure
import io.amichne.konditional.core.types.Konstrained
import io.amichne.konditional.core.types.asObjectSchema
import io.amichne.kontracts.dsl.jsonArray
import io.amichne.kontracts.dsl.jsonObject
import io.amichne.kontracts.dsl.jsonValue
import io.amichne.kontracts.schema.ArraySchema
import io.amichne.kontracts.schema.BooleanSchema
import io.amichne.kontracts.schema.DoubleSchema
import io.amichne.kontracts.schema.IntSchema
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.schema.ObjectTraits
import io.amichne.kontracts.schema.StringSchema
import io.amichne.kontracts.value.JsonArray
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
 * Supports all [Konstrained] schema variants:
 * - [ObjectSchema] / [io.amichne.kontracts.schema.RootObjectSchema]: data-class encoding via field reflection
 * - [StringSchema], [BooleanSchema], [IntSchema], [DoubleSchema]: single-property primitive extraction
 * - [ArraySchema]: single-property list extraction
 *
 * Any type implementing [Konstrained] can be encoded/decoded without additional registration.
 * For primitive and array schemas the implementing class must have exactly one property of
 * the matching Kotlin type; `@JvmInline value class` is the idiomatic way to guarantee this
 * at the language level.
 */
@KonditionalInternalApi
object SchemaValueCodec {

    /**
     * Encodes a value to JsonObject using its schema.
     *
     * @throws IllegalStateException if a property referenced by the schema is missing.
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

    /**
     * Encodes any [Konstrained] instance to the appropriate [JsonValue] by dispatching on
     * its declared schema type.
     *
     * - Object schemas → [JsonObject] (via field-reflection codec)
     * - String/Boolean/Int/Double schemas → the matching JSON primitive
     * - Array schemas → [JsonArray] from the single list-typed property
     *
     * @throws IllegalArgumentException if the schema type is unsupported, or if the
     *   implementing class does not have the required single-property structure for
     *   primitive/array schemas.
     */
    @KonditionalInternalApi
    fun encodeKonstrained(konstrained: Konstrained<*>): JsonValue =
        when (val schema = konstrained.schema) {
            is ObjectTraits -> encode(konstrained, schema.asObjectSchema())
            is StringSchema -> jsonValue { string(konstrained.extractSinglePrimitiveProperty()) }
            is BooleanSchema -> jsonValue { boolean(konstrained.extractSinglePrimitiveProperty()) }
            is IntSchema -> jsonValue { number(konstrained.extractSinglePrimitiveProperty<Int>()) }
            is DoubleSchema -> jsonValue { number(konstrained.extractSinglePrimitiveProperty<Double>()) }
            is ArraySchema<*> -> encodeKonstrainedArray(konstrained)
            else ->
                error(
                    "Unsupported schema type for Konstrained encoding: ${schema::class.simpleName}. " +
                        "Supported: ObjectSchema, RootObjectSchema, StringSchema, BooleanSchema, " +
                        "IntSchema, DoubleSchema, ArraySchema.",
                )
        }

    /**
     * Decodes a raw primitive or list value back into a [Konstrained] value class instance.
     *
     * The [kClass] must have a primary constructor with exactly one parameter whose type is
     * assignment-compatible with [rawValue]. `@JvmInline value class` satisfies this by
     * construction.
     *
     * @param kClass Target class to instantiate (typically a value class).
     * @param rawValue The raw primitive (`String`, `Boolean`, `Int`, `Double`) or `List<*>`.
     * @return [Result.success] with the constructed instance, or [Result.failure] with a
     *   [ParseError.InvalidSnapshot] if construction fails.
     */
    @KonditionalInternalApi
    fun <T : Any> decodeKonstrainedPrimitive(kClass: KClass<T>, rawValue: Any): Result<T> {
        val constructor =
            kClass.primaryConstructor
                ?: return parseFailure(
                    ParseError.InvalidSnapshot(
                        "${kClass.qualifiedName} has no primary constructor",
                    ),
                )

        if (constructor.parameters.size != 1) {
            return parseFailure(
                ParseError.InvalidSnapshot(
                    "${kClass.qualifiedName} must have exactly one constructor parameter for primitive " +
                        "schema backing (got ${constructor.parameters.size}). " +
                        "Consider using @JvmInline value class.",
                ),
            )
        }

        return runCatching { constructor.call(rawValue) }
            .fold(
                onSuccess = { Result.success(it) },
                onFailure = {
                    parseFailure(
                        ParseError.InvalidSnapshot(
                            "Failed to construct ${kClass.qualifiedName} from $rawValue: ${it.message}",
                        ),
                    )
                },
            )
    }

    private fun encodeValue(value: Any): JsonValue =
        when (value) {
            is Boolean -> jsonValue { boolean(value) }
            is String -> jsonValue { string(value) }
            is Int -> jsonValue { number(value) }
            is Double -> jsonValue { number(value) }
            is Enum<*> -> jsonValue { string(value.name) }
            is Konstrained<*> -> encodeKonstrained(value)
            else ->
                error(
                    "Unsupported type for encoding: ${value::class.qualifiedName}. " +
                        "Supported built-in types: Boolean, String, Int, Double, Enum. " +
                        "Custom types must implement Konstrained<S> where S is a supported schema.",
                )
        }

    private fun encodeKonstrainedArray(konstrained: Konstrained<*>): JsonArray {
        val kClass = konstrained::class
        // Filter to List-typed properties only — this excludes the `schema` property inherited
        // from Konstrained<*> and any other non-list fields, matching the same pattern used by
        // extractSinglePrimitiveProperty for scalar schemas.
        val listProps = kClass.memberProperties.filter { it.returnType.classifier == List::class }
        val prop =
            listProps.singleOrNull()
                ?: error(
                    "${kClass.simpleName} must have exactly one List-typed property for ArraySchema backing " +
                        "(found ${listProps.size}: ${listProps.map { it.name }}). " +
                        "Consider using @JvmInline value class.",
                )
        val list =
            prop.call(konstrained) as? List<*>
                ?: error(
                    "${kClass.simpleName}.${prop.name} must be a List for ArraySchema backing.",
                )
        return jsonArray {
            elements(list.map { element -> element.toJsonValue() })
        }
    }

    /**
     * Decodes JsonObject to an instance using schema and reflection.
     */
    fun <T : Any> decode(kClass: KClass<T>, json: JsonObject, schema: ObjectSchema): Result<T> =
        kClass.primaryConstructor
            ?.let { constructor ->
                buildSchemaParameterMap(constructor, json, schema, kClass, ::decodeValue)
                    .fold(
                        onSuccess = { parameters ->
                            runCatching { constructor.callBy(parameters) }
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
                        },
                        onFailure = { error -> Result.failure(error) },
                    )
            }
            ?: parseFailure(
                ParseError.InvalidSnapshot(
                    "${kClass.qualifiedName} must have a primary constructor for deserialization",
                ),
            )

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

/**
 * Extracts the single primitive property of the expected type [T] from a [Konstrained] instance.
 *
 * Prefers properties whose declared type exactly matches [T]. Falls back to the sole property
 * if only one exists. Throws with a clear message if zero or multiple candidates are found,
 * directing users toward `@JvmInline value class`.
 */
private inline fun <reified T : Any> Konstrained<*>.extractSinglePrimitiveProperty(): T {
    val kClass = this::class
    val allProps = kClass.memberProperties.toList()
    val matching = allProps.filter { it.returnType.classifier == T::class }
    val prop =
        when {
            matching.size == 1 -> matching[0]
            matching.isEmpty() && allProps.size == 1 -> allProps[0]
            matching.isEmpty() ->
                error(
                    "${kClass.simpleName} has no property of type ${T::class.simpleName} " +
                        "(found ${allProps.size} properties: ${allProps.map { it.name }}). " +
                        "Consider using @JvmInline value class for compile-time enforcement.",
                )
            else ->
                error(
                    "${kClass.simpleName} has ${matching.size} properties of type ${T::class.simpleName}. " +
                        "Primitive-backed Konstrained requires exactly one. " +
                        "Consider using @JvmInline value class.",
                )
        }
    return prop.call(this) as? T
        ?: error(
            "${kClass.simpleName}.${prop.name} did not return ${T::class.simpleName} " +
                "(got ${prop.call(this)?.let { it::class.simpleName } ?: "null"}).",
        )
}

/** Converts `Any?` to [JsonValue] for array-element encoding. */
private fun Any?.toJsonValue(): JsonValue =
    when (this) {
        null -> JsonNull
        is Boolean -> jsonValue { boolean(this@toJsonValue) }
        is String -> jsonValue { string(this@toJsonValue) }
        is Int -> jsonValue { number(this@toJsonValue) }
        is Double -> jsonValue { number(this@toJsonValue) }
        is JsonValue -> this
        else ->
            error(
                "Unsupported array element type for encoding: ${this::class.qualifiedName}.",
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

private fun <T : Any> buildSchemaParameterMap(
    constructor: kotlin.reflect.KFunction<T>,
    json: JsonObject,
    schema: ObjectSchema,
    owner: KClass<*>,
    decodeValue: (KClass<*>?, JsonValue) -> Result<Any>,
): Result<Map<KParameter, Any?>> {
    val result = mutableMapOf<KParameter, Any?>()
    for (param in constructor.parameters) {
        val resolved = resolveSchemaParameter(param, json, schema, owner, decodeValue)
        if (resolved.isFailure) {
            return Result.failure(
                resolved.exceptionOrNull()
                    ?: IllegalStateException("Unknown schema parameter resolution failure"),
            )
        }
        when (val resolution = resolved.getOrThrow()) {
            is ParameterResolution.Value -> result[param] = resolution.value
            ParameterResolution.Skip -> Unit
        }
    }
    return Result.success(result)
}

private fun resolveSchemaParameter(
    param: KParameter,
    json: JsonObject,
    schema: ObjectSchema,
    owner: KClass<*>,
    decodeValue: (KClass<*>?, JsonValue) -> Result<Any>,
): Result<ParameterResolution> {
    val fieldName = param.name
    val jsonValue = fieldName?.let { json.fields[it] }
    val fieldSchema = fieldName?.let { schema.fields[it] }

    return when {
        fieldName == null ->
            parseFailure(
                ParseError.InvalidSnapshot(
                    "Constructor parameter missing name in ${owner.qualifiedName}",
                ),
            )

        jsonValue != null && jsonValue !is JsonNull ->
            decodeValue(param.type.classifier as? KClass<*>, jsonValue)
                .map { decoded -> ParameterResolution.Value(decoded) }

        fieldSchema?.defaultValue != null -> Result.success(ParameterResolution.Value(fieldSchema.defaultValue))
        param.isOptional -> Result.success(ParameterResolution.Skip)
        fieldSchema?.required == true ->
            parseFailure(
                ParseError.InvalidSnapshot(
                    "Required field '$fieldName' missing in JSON for ${owner.qualifiedName}",
                ),
            )

        else ->
            parseFailure(
                ParseError.InvalidSnapshot(
                    "Field '$fieldName' missing in JSON and has no default in schema",
                ),
            )
    }
}
