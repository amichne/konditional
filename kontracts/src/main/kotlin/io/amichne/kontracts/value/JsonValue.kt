package io.amichne.kontracts.value

import io.amichne.kontracts.schema.JsonSchema
import kotlin.collections.iterator

/**
 * Sealed class representing runtime JSON values.
 *
 * JsonValue provides a type-safe representation of JSON data that can be validated
 * against JsonSchema definitions. All JsonValue instances are immutable.
 *
 * Supported value types:
 * - Primitives: Boolean, String, Number, Null
 * - Objects: Structured key-value pairs
 * - Arrays: Lists of homogeneous values
 */
sealed class JsonValue {

    /**
     * Validates this value against a schema.
     */
    abstract fun validate(schema: JsonSchema): JsonSchema.ValidationResult

    // ========== Primitive Values ==========

    /**
     * JSON boolean value.
     */
    data class JsonBoolean(val value: Boolean) : JsonValue() {
        override fun validate(schema: JsonSchema): JsonSchema.ValidationResult {
            return when (schema) {
                is JsonSchema.BooleanSchema -> JsonSchema.ValidationResult.Valid
                else -> JsonSchema.ValidationResult.Invalid(
                    "Expected ${schema}, but got Boolean"
                )
            }
        }

        override fun toString() = value.toString()
    }

    /**
     * JSON string value.
     */
    data class JsonString(val value: String) : JsonValue() {
        override fun validate(schema: JsonSchema): JsonSchema.ValidationResult {
            return when (schema) {
                is JsonSchema.StringSchema -> JsonSchema.ValidationResult.Valid
                is JsonSchema.EnumSchema<*> -> {
                    // Validate that the string corresponds to a valid enum value
                    val enumConstants = schema.enumClass.java.enumConstants
                    if (enumConstants.any { it.name == value }) {
                        JsonSchema.ValidationResult.Valid
                    } else {
                        JsonSchema.ValidationResult.Invalid(
                            "String '$value' is not a valid ${schema.enumClass.simpleName} value"
                        )
                    }
                }
                else -> JsonSchema.ValidationResult.Invalid(
                    "Expected ${schema}, but got String"
                )
            }
        }

        override fun toString() = "\"$value\""
    }

    /**
     * JSON number value (stored as Double for precision).
     */
    data class JsonNumber(val value: Double) : JsonValue() {
        override fun validate(schema: JsonSchema): JsonSchema.ValidationResult {
            return when (schema) {
                is JsonSchema.IntSchema -> {
                    if (value == value.toInt().toDouble()) {
                        JsonSchema.ValidationResult.Valid
                    } else {
                        JsonSchema.ValidationResult.Invalid(
                            "Expected integer, but got non-integer number: $value"
                        )
                    }
                }
                is JsonSchema.DoubleSchema -> JsonSchema.ValidationResult.Valid
                else -> JsonSchema.ValidationResult.Invalid(
                    "Expected ${schema}, but got Number"
                )
            }
        }

        fun toInt(): Int = value.toInt()
        fun toDouble(): Double = value

        override fun toString() = value.toString()
    }

    /**
     * JSON null value.
     */
    object JsonNull : JsonValue() {
        override fun validate(schema: JsonSchema): JsonSchema.ValidationResult {
            return when (schema) {
                is JsonSchema.NullSchema -> JsonSchema.ValidationResult.Valid
                else -> JsonSchema.ValidationResult.Invalid(
                    "Expected ${schema}, but got Null"
                )
            }
        }

        override fun toString() = "null"
    }

    // ========== Complex Values ==========

    /**
     * JSON object value with typed fields.
     *
     * @param fields Map of field names to their values
     * @param schema Optional schema for validation
     */
    data class JsonObject(
        val fields: Map<String, JsonValue>,
        val schema: JsonSchema.ObjectSchema? = null
    ) : JsonValue() {

        init {
            // Validate against schema if provided
            schema?.let { s ->
                val result = validate(s)
                if (result.isInvalid) {
                    throw IllegalArgumentException(
                        "JsonObject does not match schema: ${result.getErrorMessage()}"
                    )
                }
            }
        }

        override fun validate(schema: JsonSchema): JsonSchema.ValidationResult {
            if (schema !is JsonSchema.ObjectSchema) {
                return JsonSchema.ValidationResult.Invalid(
                    "Expected ${schema}, but got JsonObject"
                )
            }

            // Check required fields
            val requiredValidation = schema.validateRequiredFields(fields.keys)
            if (requiredValidation.isInvalid) {
                return requiredValidation
            }

            // Validate each field
            for ((key, value) in fields) {
                val fieldSchema = schema.fields[key]
                    ?: return JsonSchema.ValidationResult.Invalid(
                        "Unknown field '$key' in object"
                    )

                val fieldValidation = value.validate(fieldSchema.schema)
                if (fieldValidation.isInvalid) {
                    return JsonSchema.ValidationResult.Invalid(
                        "Field '$key': ${fieldValidation.getErrorMessage()}"
                    )
                }
            }

            return JsonSchema.ValidationResult.Valid
        }

        /**
         * Gets a field value by name.
         */
        operator fun get(key: String): JsonValue? = fields[key]

        /**
         * Gets a typed value from a field.
         */
        inline fun <reified T> getTyped(key: String): T? {
            return when (val value = fields[key]) {
                is JsonBoolean -> if (T::class == Boolean::class) value.value as T else null
                is JsonString -> if (T::class == String::class) value.value as T else null
                is JsonNumber -> when (T::class) {
                    Int::class -> value.toInt() as T
                    Double::class -> value.toDouble() as T
                    else -> null
                }
                is JsonObject -> if (T::class == JsonObject::class) value as T else null
                is JsonArray -> if (T::class == JsonArray::class) value as T else null
                JsonNull -> null
                null -> null
            }
        }

        override fun toString(): String {
            val fieldsStr = fields.entries.joinToString(", ") { (k, v) -> "\"$k\": $v" }
            return "{$fieldsStr}"
        }
    }

    /**
     * JSON array value with homogeneous elements.
     *
     * @param elements List of array elements (must all match elementSchema)
     * @param elementSchema Schema for array elements
     */
    data class JsonArray(
        val elements: List<JsonValue>,
        val elementSchema: JsonSchema? = null
    ) : JsonValue() {

        init {
            // Validate all elements against schema if provided
            elementSchema?.let { schema ->
                val result = validate(JsonSchema.ArraySchema(schema))
                if (result.isInvalid) {
                    throw IllegalArgumentException(
                        "JsonArray does not match schema: ${result.getErrorMessage()}"
                    )
                }
            }
        }

        override fun validate(schema: JsonSchema): JsonSchema.ValidationResult {
            if (schema !is JsonSchema.ArraySchema) {
                return JsonSchema.ValidationResult.Invalid(
                    "Expected ${schema}, but got JsonArray"
                )
            }

            // Validate each element
            for ((index, element) in elements.withIndex()) {
                val elementValidation = element.validate(schema.elementSchema)
                if (elementValidation.isInvalid) {
                    return JsonSchema.ValidationResult.Invalid(
                        "Element at index $index: ${elementValidation.getErrorMessage()}"
                    )
                }
            }

            return JsonSchema.ValidationResult.Valid
        }

        /**
         * Gets an element by index.
         */
        operator fun get(index: Int): JsonValue? = elements.getOrNull(index)

        /**
         * Returns the number of elements.
         */
        val size: Int get() = elements.size

        /**
         * Checks if the array is empty.
         */
        fun isEmpty(): Boolean = elements.isEmpty()

        /**
         * Checks if the array is not empty.
         */
        fun isNotEmpty(): Boolean = elements.isNotEmpty()

        override fun toString(): String = elements.toString()
    }

    companion object {
        /**
         * Creates a JsonBoolean from a Boolean.
         */
        fun from(value: Boolean): JsonBoolean = JsonBoolean(value)

        /**
         * Creates a JsonString from a String.
         */
        fun from(value: String): JsonString = JsonString(value)

        /**
         * Creates a JsonNumber from an Int.
         */
        fun from(value: Int): JsonNumber = JsonNumber(value.toDouble())

        /**
         * Creates a JsonNumber from a Double.
         */
        fun from(value: Double): JsonNumber = JsonNumber(value)

        /**
         * Creates a JsonObject from a map.
         */
        fun obj(
            fields: Map<String, JsonValue>,
            schema: JsonSchema.ObjectSchema? = null
        ): JsonObject = JsonObject(fields, schema)

        /**
         * Creates a JsonArray from a list.
         */
        fun array(
            elements: List<JsonValue>,
            elementSchema: JsonSchema? = null
        ): JsonArray = JsonArray(elements, elementSchema)
    }
}
