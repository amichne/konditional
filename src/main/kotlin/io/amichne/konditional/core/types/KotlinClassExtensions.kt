package io.amichne.konditional.core.types

import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.kontracts.schema.DoubleSchema
import io.amichne.kontracts.schema.IntSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ValidationResult
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
import kotlin.reflect.jvm.isAccessible

/**
 * Converts a custom encodeable instance to a JsonValue.JsonObject.
 *
 * This function uses reflection to extract all properties from the custom type
 * and convert them to JsonValue instances based on their types.
 *
 * @param schema The schema to validate against (optional, defaults to the instance's schema)
 * @return JsonValue.JsonObject representation create this custom encodeable type
 */
fun KotlinEncodeable<*>.toJsonValue(schema: JsonSchema? = null): JsonObject =
    JsonObject(
        fields =
            buildMap {
                val instance = this@toJsonValue
                instance::class.memberProperties
                    .asSequence()
                    .filterNot { it.name == "schema" }
                    .sortedBy { it.name }
                    .forEach { property ->
                        property.isAccessible = true
                        put(property.name, property.call(instance).toJsonValue())
                    }
            },
        schema = (schema ?: this.schema).asObjectSchema(),
    )

/**
 * Converts any value to a JsonValue.
 *
 * This is a helper function that converts Kotlin types to their JsonValue equivalents.
 */
internal fun Any?.toJsonValue(): JsonValue = when (this) {
    null -> JsonNull
    is Boolean -> JsonBoolean(this)
    is String -> JsonString(this)
    is Int -> JsonNumber(this.toDouble())
    is Double -> JsonNumber(this)
    is Enum<*> -> JsonString(this.name)
    is Map<*, *> -> {
        @Suppress("UNCHECKED_CAST")
        JsonObject(
            fields =
                this.entries.associate { (rawKey, rawValue) ->
                    val key =
                        rawKey as? String
                            ?: error("JsonObject keys must be strings, got ${rawKey?.let { it::class.simpleName }}")
                    key to rawValue.toJsonValue()
                },
            schema = null,
        )
    }
    is KotlinEncodeable<*> -> {
        this.toJsonValue()
    }
    is JsonValue -> this
    is List<*> -> JsonArray(map { it.toJsonValue() }, null)
    else -> throw IllegalArgumentException(
        "Unsupported type for JSON conversion: ${this::class.simpleName}"
    )
}

/**
 * Parses a JsonValue.JsonObject into a custom encodeable instance.
 *
 * This function uses reflection to instantiate the custom type with values
 * extracted from the JsonObject, validating against the schema if present.
 *
 * @param T The custom encodeable type to parse into
 * @return ParseResult containing either the custom type instance or an error
 */
inline fun <reified T : KotlinEncodeable<*>> JsonObject.parseAs(): ParseResult<T> {
    return try {
        val kClass = T::class
        val constructor = kClass.primaryConstructor ?: return ParseResult.Failure(
            ParseError.InvalidSnapshot("Custom type ${kClass.simpleName} must have a primary constructor")
        )
        constructor.isAccessible = true

        // Validate against schema if present
        this.schema?.let { schema ->
            val validationResult = this.validate(schema)
            if (validationResult is ValidationResult.Invalid) {
                return ParseResult.Failure(
                    ParseError.InvalidSnapshot("Schema validation failed: ${validationResult.message}")
                )
            }
        }

        // Build parameter map for constructor
        val parameterMap = mutableMapOf<KParameter, Any?>()
        constructor.parameters.forEach { param ->
            val fieldName = param.name ?: return ParseResult.Failure(
                ParseError.InvalidSnapshot("Constructor parameter has no name")
            )

            val jsonValue = this.fields[fieldName]
            val value = if (jsonValue != null) jsonValue.toKotlinValue(param.type.classifier as? KClass<*>) else
                if (param.isOptional) null else {
                    return ParseResult.Failure(
                        ParseError.InvalidSnapshot("Required field '$fieldName' is missing")
                    )
                }

            if (value != null) {
                parameterMap[param] = value
            }
        }

        // Instantiate the custom type
        val instance = constructor.callBy(parameterMap)
        ParseResult.Success(instance)
    } catch (e: Exception) {
        ParseResult.Failure(
            ParseError.InvalidSnapshot("Failed to parse custom type: ${e.message}")
        )
    }
}

/**
 * Converts a JsonValue to its Kotlin equivalent.
 */
@PublishedApi
internal fun JsonValue.toKotlinValue(targetClass: KClass<*>?): Any? = when (this) {
    is JsonNull -> null
    is JsonBoolean -> value
    is JsonString -> value
    is JsonNumber -> when (targetClass) {
        Int::class -> toInt()
        Double::class -> toDouble()
        else -> toDouble()
    }
    is JsonObject -> this
    is JsonArray -> elements
}

/**
 * Converts a JsonValue to a primitive value for serialization.
 *
 * This is used when converting JsonObjects to Maps for FlagValue serialization.
 */
fun JsonValue.toPrimitiveValue(): Any? = when (this) {
    is JsonNull -> null
    is JsonBoolean -> value
    is JsonString -> value
    is JsonNumber -> value
    is JsonObject ->
        schema?.let { s ->
            fields.mapValues { (k, v) ->
                val fieldSchema = s.fields[k]?.schema
                when {
                    v is JsonNumber && fieldSchema is IntSchema -> v.toInt()
                    v is JsonNumber && fieldSchema is DoubleSchema -> v.toDouble()
                    else -> v.toPrimitiveValue()
                }
            }
        } ?: fields.mapValues { (_, v) -> v.toPrimitiveValue() }
    is JsonArray -> elements.map { it.toPrimitiveValue() }
}
