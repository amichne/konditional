package io.amichne.konditional.core.types.json

import kotlin.reflect.KClass

/**
 * Sealed class representing compile-time schema definitions for JSON values.
 *
 * JsonSchema provides type safety by defining the expected structure of JSON data
 * at compile time. Schemas can be composed to create complex nested structures.
 *
 * Supported schema types:
 * - Primitives: Boolean, String, Int, Double
 * - Enums: User-defined enum types
 * - Objects: Structured JSON objects with typed fields
 * - Arrays: Homogeneous arrays of a single element type
 */
sealed class JsonSchema {

    /**
     * Schema for boolean values.
     */
    object BooleanSchema : JsonSchema() {
        override fun toString() = "BooleanSchema"
    }

    /**
     * Schema for string values.
     */
    object StringSchema : JsonSchema() {
        override fun toString() = "StringSchema"
    }

    /**
     * Schema for integer values.
     */
    object IntSchema : JsonSchema() {
        override fun toString() = "IntSchema"
    }

    /**
     * Schema for double/decimal values.
     */
    object DoubleSchema : JsonSchema() {
        override fun toString() = "DoubleSchema"
    }

    /**
     * Schema for enum values.
     *
     * @param E The enum type
     * @param enumClass The KClass of the enum for runtime type checking
     */
    data class EnumSchema<E : Enum<E>>(
        val enumClass: KClass<E>
    ) : JsonSchema() {
        override fun toString() = "EnumSchema(${enumClass.simpleName})"
    }

    /**
     * Schema for null values.
     */
    object NullSchema : JsonSchema() {
        override fun toString() = "NullSchema"
    }

    /**
     * Schema for JSON objects with typed fields.
     *
     * @param fields Map of field names to their schemas
     * @param required Set of required field names
     */
    data class ObjectSchema(
        val fields: Map<String, FieldSchema>
    ) : JsonSchema() {
        val required: Set<String> = fields.filter { it.value.required }.keys

        /**
         * Validates that all required fields are present.
         */
        fun validateRequiredFields(fieldNames: Set<String>): ValidationResult {
            val missing = required - fieldNames
            return if (missing.isEmpty()) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid("Missing required fields: $missing")
            }
        }

        override fun toString() = "ObjectSchema(fields=${fields.keys})"
    }

    /**
     * Schema for homogeneous arrays.
     *
     * @param elementSchema The schema for all elements in the array
     */
    data class ArraySchema(
        val elementSchema: JsonSchema
    ) : JsonSchema() {
        override fun toString() = "ArraySchema($elementSchema)"
    }

    /**
     * Schema for a single field in an object.
     *
     * @param schema The schema for this field's value
     * @param required Whether this field is required (default: false)
     * @param defaultValue Optional default value if field is missing
     */
    data class FieldSchema(
        val schema: JsonSchema,
        val required: Boolean = false,
        val defaultValue: Any? = null
    ) {
        override fun toString() = "FieldSchema($schema, required=$required)"
    }

    /**
     * Result of schema validation.
     */
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()

        val isValid: Boolean get() = this is Valid
        val isInvalid: Boolean get() = this is Invalid

        fun getErrorMessage(): String? = when (this) {
            is Invalid -> message
            is Valid -> null
        }
    }

    companion object {
        /**
         * Creates a boolean schema.
         */
        fun boolean() = BooleanSchema

        /**
         * Creates a string schema.
         */
        fun string() = StringSchema

        /**
         * Creates an integer schema.
         */
        fun int() = IntSchema

        /**
         * Creates a double schema.
         */
        fun double() = DoubleSchema

        /**
         * Creates an enum schema.
         */
        inline fun <reified E : Enum<E>> enum() = EnumSchema(E::class)

        /**
         * Creates a null schema.
         */
        fun nullSchema() = NullSchema

        /**
         * Creates an array schema.
         */
        fun array(elementSchema: JsonSchema) = ArraySchema(elementSchema)

        /**
         * Creates an object schema.
         */
        fun obj(fields: Map<String, FieldSchema>) = ObjectSchema(fields)
    }
}
