package io.amichne.kontracts.value

import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.schema.ValidationResult

/**
 * JSON object value with typed fields.
 *
 * @param fields Map of field names to their values
 * @param schema Optional schema for validation
 */
data class JsonObject(
    val fields: Map<String, JsonValue>,
    val schema: ObjectSchema? = null,
) : JsonValue() {

    init {
        schema?.let { s ->
            val result = validate(s)
            if (result.isInvalid) {
                throw IllegalArgumentException(
                    "JsonObject does not match schema: ${result.getErrorMessage()}"
                )
            }
        }
    }

    override fun validate(schema: JsonSchema<*>): ValidationResult {
        if (schema !is ObjectSchema) {
            return ValidationResult.Invalid(
                "Expected ${schema}, but got JsonObject"
            )
        }

        val requiredValidation = schema.validateRequiredFields(fields.keys)
        if (requiredValidation.isInvalid) {
            return requiredValidation
        }
        for ((key, value) in fields) {
            val fieldSchema = schema.fields[key] ?: return ValidationResult.Invalid("Unknown field '$key' in object")

            val fieldValidation = value.validate(fieldSchema.schema)
            if (fieldValidation.isInvalid) {
                return ValidationResult.Invalid(
                    "Field '$key': ${fieldValidation.getErrorMessage()}"
                )
            }
        }

        return ValidationResult.Valid
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
