package io.amichne.konditional.core.types

import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.types.json.JsonSchema
import io.amichne.konditional.core.types.json.JsonValue
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Converts a data class instance to a JsonValue.JsonObject.
 *
 * This function uses reflection to extract all properties from the data class
 * and convert them to JsonValue instances based on their types.
 *
 * @param schema The schema to validate against (optional, defaults to the data class's schema)
 * @return JsonValue.JsonObject representation of this data class
 */
fun DataClassWithSchema.toJsonValue(schema: JsonSchema.ObjectSchema? = null): JsonValue.JsonObject {
    val actualSchema = schema ?: this.schema
    val fields = mutableMapOf<String, JsonValue>()

    // Get all properties from the data class using reflection
    // Exclude the 'schema' property itself from serialization
    this::class.memberProperties.forEach { property ->
        if (property.name != "schema") {
            val value = property.call(this)
            val jsonValue = value.toJsonValue()
            fields[property.name] = jsonValue
        }
    }

    return JsonValue.JsonObject(fields, actualSchema)
}

/**
 * Converts any value to a JsonValue.
 *
 * This is a helper function that converts Kotlin types to their JsonValue equivalents.
 */
internal fun Any?.toJsonValue(): JsonValue = when (this) {
    null -> JsonValue.JsonNull
    is Boolean -> JsonValue.JsonBoolean(this)
    is String -> JsonValue.JsonString(this)
    is Int -> JsonValue.JsonNumber(this.toDouble())
    is Double -> JsonValue.JsonNumber(this)
    is Enum<*> -> JsonValue.JsonString(this.name)
    is DataClassWithSchema -> toJsonValue()
    is JsonValue -> this
    is List<*> -> JsonValue.JsonArray( map { it.toJsonValue() }, null )
    else -> throw IllegalArgumentException(
        "Unsupported type for JSON conversion: ${this::class.simpleName}"
    )
}

/**
 * Parses a JsonValue.JsonObject into a data class instance.
 *
 * This function uses reflection to instantiate the data class with values
 * extracted from the JsonObject, validating against the schema if present.
 *
 * @param T The data class type to parse into
 * @return ParseResult containing either the data class instance or an error
 */
inline fun <reified T : DataClassWithSchema> JsonValue.JsonObject.parseAs(): ParseResult<T> {
    return try {
        val kClass = T::class
        val constructor = kClass.primaryConstructor
            ?: return ParseResult.Failure(
                ParseError.InvalidSnapshot("Data class ${kClass.simpleName} must have a primary constructor")
            )

        // Validate against schema if present
        this.schema?.let { schema ->
            val validationResult = this.validate(schema)
            if (validationResult is JsonSchema.ValidationResult.Invalid) {
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
            val value = if (jsonValue != null) {
                jsonValue.toKotlinValue(param.type.classifier as? KClass<*>)
            } else {
                // Use default value if available
                if (param.isOptional) {
                    null // Will use default
                } else {
                    return ParseResult.Failure(
                        ParseError.InvalidSnapshot("Required field '$fieldName' is missing")
                    )
                }
            }

            if (value != null) {
                parameterMap[param] = value
            }
        }

        // Instantiate the data class
        val instance = constructor.callBy(parameterMap)
        ParseResult.Success(instance)
    } catch (e: Exception) {
        ParseResult.Failure(
            ParseError.InvalidSnapshot("Failed to parse data class: ${e.message}")
        )
    }
}

/**
 * Converts a JsonValue to its Kotlin equivalent.
 */
@PublishedApi
internal fun JsonValue.toKotlinValue(targetClass: KClass<*>?): Any? = when (this) {
    is JsonValue.JsonNull -> null
    is JsonValue.JsonBoolean -> this.value
    is JsonValue.JsonString -> this.value
    is JsonValue.JsonNumber -> when (targetClass) {
        Int::class -> this.toInt()
        Double::class -> this.toDouble()
        else -> this.toDouble()
    }
    is JsonValue.JsonObject -> this
    is JsonValue.JsonArray -> this.elements
    else -> throw IllegalArgumentException("Unsupported JsonValue type: ${this::class.simpleName}")
}

/**
 * Converts a JsonValue to a primitive value for serialization.
 *
 * This is used when converting JsonObjects to Maps for FlagValue serialization.
 */
fun JsonValue.toPrimitiveValue(): Any? = when (this) {
    is JsonValue.JsonNull -> null
    is JsonValue.JsonBoolean -> this.value
    is JsonValue.JsonString -> this.value
    is JsonValue.JsonNumber -> this.value
    is JsonValue.JsonObject -> this.fields.mapValues { (_, v) -> v.toPrimitiveValue() }
    is JsonValue.JsonArray -> this.elements.map { it.toPrimitiveValue() }
}
