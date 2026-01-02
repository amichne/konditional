package io.amichne.konditional.serialization

import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.types.KotlinEncodeable
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
 * driven by the schema definition. Any type implementing KotlinEncodeable<ObjectSchema> can be
 * serialized without additional registration.
 *
 * ## How It Works
 *
 * **Encoding:**
 * - Walks schema fields to determine property names
 * - Uses reflection to read property values from instance
 * - Converts each value to appropriate JsonValue based on type
 * - Handles nested KotlinEncodeable types recursively
 *
 * **Decoding:**
 * - Validates JSON structure matches schema
 * - Builds constructor parameter map from JSON fields
 * - Uses primary constructor reflection to instantiate class
 * - Handles nested KotlinEncodeable types recursively
 *
 * ## Type Support
 *
 * Built-in types (no schema needed):
 * - Boolean → JsonBoolean
 * - String → JsonString
 * - Int, Double → JsonNumber
 * - Enum → JsonString (constant name)
 *
 * Custom types (require KotlinEncodeable<ObjectSchema>):
 * - Data classes with primary constructor
 * - Nested custom types
 * - Optional fields with defaults
 */
object SchemaBasedSerializer {

    /**
     * Encodes a value to JsonObject using its schema.
     *
     * Walks the schema fields and uses reflection to read corresponding properties
     * from the instance. Property names in the schema must match actual property names.
     *
     * @param value The instance to encode
     * @param schema The ObjectSchema defining the structure
     * @return JsonObject representation
     * @throws IllegalArgumentException if property cannot be read
     */
    fun <T : Any> encode(value: T, schema: ObjectSchema): JsonObject {
        val fields = schema.fields.mapValues { (fieldName, fieldSchema) ->
            // Find property by name using reflection
            val property = value::class.memberProperties.find { it.name == fieldName }
                ?: error("Property '$fieldName' not found on ${value::class.qualifiedName}")

            // Read property value
            val propertyValue = property.call(value)

            // Convert to JsonValue
            when {
                propertyValue == null -> JsonNull
                else -> encodeValue(propertyValue)
            }
        }

        return JsonObject(fields, schema)
    }

    /**
     * Encodes a single value to JsonValue.
     *
     * Handles built-in types and recursively encodes KotlinEncodeable types.
     */
    private fun encodeValue(value: Any): JsonValue = when (value) {
        is Boolean -> JsonBoolean(value)
        is String -> JsonString(value)
        is Int -> JsonNumber(value.toDouble())
        is Double -> JsonNumber(value)
        is Enum<*> -> JsonString(value.name)
        is KotlinEncodeable<*> -> {
            // Recursively encode nested custom types
            val schema = value.schema as? ObjectSchema
                ?: error("KotlinEncodeable must have ObjectSchema, got ${value.schema::class.simpleName}")
            encode(value, schema)
        }
        else -> error(
            "Unsupported type for encoding: ${value::class.qualifiedName}. " +
                "Type must implement KotlinEncodeable<ObjectSchema> or be a built-in type " +
                "(Boolean, String, Int, Double, Enum)."
        )
    }

    /**
     * Decodes JsonObject to an instance using schema and reflection.
     *
     * Validates JSON structure, builds constructor parameter map, and uses primary
     * constructor reflection to instantiate the class.
     *
     * @param kClass The class to instantiate
     * @param json The JSON data
     * @param schema The ObjectSchema defining the structure
     * @return ParseResult with instance or error
     */
    fun <T : Any> decode(kClass: KClass<T>, json: JsonObject, schema: ObjectSchema): ParseResult<T> {
        val constructor = kClass.primaryConstructor
            ?: return ParseResult.Failure(
                ParseError.InvalidSnapshot(
                    "${kClass.qualifiedName} must have a primary constructor for deserialization"
                )
            )

        // Build parameter map from JSON fields
        val parameterMap = mutableMapOf<KParameter, Any?>()

        for (param in constructor.parameters) {
            val fieldName = param.name
                ?: return ParseResult.Failure(
                    ParseError.InvalidSnapshot("Constructor parameter missing name in ${kClass.qualifiedName}")
                )

            val fieldSchema = schema.fields[fieldName]
            val jsonValue = json.fields[fieldName]

            // Determine value for this parameter
            val value = when {
                // Field present in JSON - decode it
                jsonValue != null && jsonValue !is JsonNull -> {
                    when (val decoded = decodeValue(param.type.classifier as? KClass<*>, jsonValue)) {
                        is ParseResult.Success -> decoded.value
                        is ParseResult.Failure -> return decoded
                    }
                }

                // Field missing but has default in schema
                fieldSchema?.defaultValue != null -> {
                    fieldSchema.defaultValue
                }

                // Field missing but parameter is optional
                param.isOptional -> {
                    continue // Skip - let constructor use default
                }

                // Field missing and no default - error
                fieldSchema?.required == true -> {
                    return ParseResult.Failure(
                        ParseError.InvalidSnapshot(
                            "Required field '$fieldName' missing in JSON for ${kClass.qualifiedName}"
                        )
                    )
                }

                // Field not in schema and not optional
                else -> {
                    return ParseResult.Failure(
                        ParseError.InvalidSnapshot(
                            "Field '$fieldName' missing in JSON and has no default in schema"
                        )
                    )
                }
            }

            parameterMap[param] = value
        }

        // Instantiate via constructor
        return try {
            @Suppress("UNCHECKED_CAST")
            ParseResult.Success(constructor.callBy(parameterMap) as T)
        } catch (e: Exception) {
            ParseResult.Failure(
                ParseError.InvalidSnapshot(
                    "Failed to instantiate ${kClass.qualifiedName}: ${e.message}"
                )
            )
        }
    }

    /**
     * Decodes a single JsonValue to a Kotlin value.
     *
     * Handles built-in types and recursively decodes KotlinEncodeable types.
     */
    private fun decodeValue(kClass: KClass<*>?, json: JsonValue): ParseResult<Any> {
        if (kClass == null) {
            return ParseResult.Failure(ParseError.InvalidSnapshot("Cannot decode value without type information"))
        }

        return when {
            // Built-in types
            kClass == Boolean::class -> when (json) {
                is JsonBoolean -> ParseResult.Success(json.value)
                else -> ParseResult.Failure(ParseError.InvalidSnapshot("Expected JsonBoolean, got ${json::class.simpleName}"))
            }

            kClass == String::class -> when (json) {
                is JsonString -> ParseResult.Success(json.value)
                else -> ParseResult.Failure(ParseError.InvalidSnapshot("Expected JsonString, got ${json::class.simpleName}"))
            }

            kClass == Int::class -> when (json) {
                is JsonNumber -> ParseResult.Success(json.toInt())
                else -> ParseResult.Failure(ParseError.InvalidSnapshot("Expected JsonNumber, got ${json::class.simpleName}"))
            }

            kClass == Double::class -> when (json) {
                is JsonNumber -> ParseResult.Success(json.toDouble())
                else -> ParseResult.Failure(ParseError.InvalidSnapshot("Expected JsonNumber, got ${json::class.simpleName}"))
            }

            kClass.java.isEnum -> when (json) {
                is JsonString -> {
                    @Suppress("UNCHECKED_CAST")
                    val enumClass = kClass.java as Class<out Enum<*>>
                    val enumValue = enumClass.enumConstants.find { it.name == json.value }
                    if (enumValue != null) {
                        ParseResult.Success(enumValue)
                    } else {
                        ParseResult.Failure(
                            ParseError.InvalidSnapshot("Unknown enum constant '${json.value}' for ${kClass.simpleName}")
                        )
                    }
                }
                else -> ParseResult.Failure(ParseError.InvalidSnapshot("Expected JsonString for enum, got ${json::class.simpleName}"))
            }

            // Custom types implementing KotlinEncodeable
            else -> when (json) {
                is JsonObject -> {
                    val schema = extractSchema(kClass)
                        ?: return ParseResult.Failure(
                            ParseError.InvalidSnapshot(
                                "${kClass.qualifiedName} must implement KotlinEncodeable<ObjectSchema> " +
                                    "for deserialization"
                            )
                        )
                    decode(kClass, json, schema)
                }
                else -> ParseResult.Failure(
                    ParseError.InvalidSnapshot("Expected JsonObject for custom type, got ${json::class.simpleName}")
                )
            }
        }
    }
}
