package io.amichne.kontracts.schema

import kotlin.reflect.KClass

/**
 * Base interface for OpenAPI-esque properties.
 */
interface OpenApiProps {
    val title: String?
    val description: String?
    val default: Any?
    val nullable: Boolean
    val example: Any?
    val deprecated: Boolean
}

/**
 * Sealed class representing compile-time schema definitions for JSON values, with OpenAPI-esque properties.
 */
sealed class JsonSchema : OpenApiProps {
    override val title: String? = null
    override val description: String? = null
    override val default: Any? = null
    override val nullable: Boolean = false
    override val example: Any? = null
    override val deprecated: Boolean = false

    /**
     * Schema for boolean values.
     */
    data class BooleanSchema(
        override val title: String? = null,
        override val description: String? = null,
        override val default: Any? = null,
        override val nullable: Boolean = false,
        override val example: Any? = null,
        override val deprecated: Boolean = false
    ) : JsonSchema() {
        override fun toString() = "BooleanSchema"
    }

    /**
     * Schema for string values.
     * Supports OpenAPI string constraints.
     */
    data class StringSchema(
        override val title: String? = null,
        override val description: String? = null,
        override val default: Any? = null,
        override val nullable: Boolean = false,
        override val example: Any? = null,
        override val deprecated: Boolean = false,
        val minLength: Int? = null,
        val maxLength: Int? = null,
        val pattern: String? = null,
        val format: String? = null,
        val enum: List<String>? = null
    ) : JsonSchema() {
        override fun toString() = "StringSchema"
    }

    /**
     * Schema for integer values.
     * Supports OpenAPI numeric constraints.
     */
    data class IntSchema(
        override val title: String? = null,
        override val description: String? = null,
        override val default: Any? = null,
        override val nullable: Boolean = false,
        override val example: Any? = null,
        override val deprecated: Boolean = false,
        val minimum: Int? = null,
        val maximum: Int? = null,
        val enum: List<Int>? = null
    ) : JsonSchema() {
        override fun toString() = "IntSchema"
    }

    /**
     * Schema for double/decimal values.
     * Supports OpenAPI numeric constraints.
     */
    data class DoubleSchema(
        override val title: String? = null,
        override val description: String? = null,
        override val default: Any? = null,
        override val nullable: Boolean = false,
        override val example: Any? = null,
        override val deprecated: Boolean = false,
        val minimum: Double? = null,
        val maximum: Double? = null,
        val enum: List<Double>? = null,
        val format: String? = null
    ) : JsonSchema() {
        override fun toString() = "DoubleSchema"
    }

    /**
     * Schema for enum values.
     * @param E The enum type
     * @param enumClass The KClass of the enum for runtime type checking
     * @param values The allowed enum values
     */
    data class EnumSchema<E : Enum<E>>(
        val enumClass: KClass<E>,
        val values: List<E>,
        override val title: String? = null,
        override val description: String? = null,
        override val default: Any? = null,
        override val nullable: Boolean = false,
        override val example: Any? = null,
        override val deprecated: Boolean = false
    ) : JsonSchema() {
        override fun toString() = "EnumSchema(${enumClass.simpleName})"
    }

    /**
     * Schema for null values.
     */
    data class NullSchema(
        override val title: String? = null,
        override val description: String? = null,
        override val default: Any? = null,
        override val nullable: Boolean = true,
        override val example: Any? = null,
        override val deprecated: Boolean = false
    ) : JsonSchema() {
        override fun toString() = "NullSchema"
    }

    interface ObjectTraits {
        val fields: Map<String, FieldSchema>
        val required: Set<String>?

        /**
         * Validates that all required fields are present.
         */
        fun validateRequiredFields(fieldNames: Set<String>): ValidationResult {
            val req = required ?: fields.filter { it.value.required }.keys
            val missing = req - fieldNames
            return if (missing.isEmpty()) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid("Missing required fields: $missing")
            }
        }

    }

    data class RootObjectSchema(
        override val fields: Map<String, FieldSchema>,
        override val title: String? = null,
        override val description: String? = null,
        override val default: Any? = null,
        override val nullable: Boolean = false,
        override val example: Any? = null,
        override val deprecated: Boolean = false,
        override val required: Set<String>? = null
    ) : JsonSchema(), ObjectTraits {
        override fun toString() = "RootObjectSchema(fields=${fields.keys})"
    }

    /**
     * Schema for JSON objects with typed fields.
     * @param fields Map of field names to their schemas
     */
    data class ObjectSchema(
        override val fields: Map<String, FieldSchema>,
        override val title: String? = null,
        override val description: String? = null,
        override val default: Any? = null,
        override val nullable: Boolean = false,
        override val example: Any? = null,
        override val deprecated: Boolean = false,
        override val required: Set<String>? = null
    ) : JsonSchema(), ObjectTraits {
        override fun toString() = "ObjectSchema(fields=${fields.keys})"
    }

    /**
     * Schema for homogeneous arrays.
     * @param elementSchema The schema for all elements in the array
     */
    data class ArraySchema(
        val elementSchema: JsonSchema,
        override val title: String? = null,
        override val description: String? = null,
        override val default: Any? = null,
        override val nullable: Boolean = false,
        override val example: Any? = null,
        override val deprecated: Boolean = false,
        val minItems: Int? = null,
        val maxItems: Int? = null,
        val uniqueItems: Boolean = false
    ) : JsonSchema() {
        override fun toString() = "ArraySchema($elementSchema)"
    }

    /**
     * Schema for a single field in an object.
     * @param schema The schema for this field's value
     * @param required Whether this field is required (default: false)
     * @param defaultValue Optional default value if field is missing
     */
    data class FieldSchema(
        val schema: JsonSchema,
        val required: Boolean = false,
        val defaultValue: Any? = null,
        val description: String? = null,
        val deprecated: Boolean = false
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
        fun boolean(
            title: String? = null,
            description: String? = null,
            default: Any? = null,
            nullable: Boolean = false,
            example: Any? = null,
            deprecated: Boolean = false
        ) = BooleanSchema(title, description, default, nullable, example, deprecated)

        fun string(
            title: String? = null,
            description: String? = null,
            default: Any? = null,
            nullable: Boolean = false,
            example: Any? = null,
            deprecated: Boolean = false,
            minLength: Int? = null,
            maxLength: Int? = null,
            pattern: String? = null,
            format: String? = null,
            enum: List<String>? = null
        ): StringSchema = StringSchema(title, description, default, nullable, example, deprecated, minLength, maxLength, pattern, format, enum)

        fun int(
            title: String? = null,
            description: String? = null,
            default: Any? = null,
            nullable: Boolean = false,
            example: Any? = null,
            deprecated: Boolean = false,
            minimum: Int? = null,
            maximum: Int? = null,
            enum: List<Int>? = null
        ) = IntSchema(title, description, default, nullable, example, deprecated, minimum, maximum, enum)

        fun double(
            title: String? = null,
            description: String? = null,
            default: Any? = null,
            nullable: Boolean = false,
            example: Any? = null,
            deprecated: Boolean = false,
            minimum: Double? = null,
            maximum: Double? = null,
            enum: List<Double>? = null,
            format: String? = null
        ) = DoubleSchema(title, description, default, nullable, example, deprecated, minimum, maximum, enum, format)

        inline fun <reified E : Enum<E>> enum(
            values: List<E>,
            title: String? = null,
            description: String? = null,
            default: Any? = null,
            nullable: Boolean = false,
            example: Any? = null,
            deprecated: Boolean = false
        ) = EnumSchema(E::class, values, title, description, default, nullable, example, deprecated)

        fun nullSchema(
            title: String? = null,
            description: String? = null,
            default: Any? = null,
            example: Any? = null,
            deprecated: Boolean = false
        ) = NullSchema(title, description, default, true, example, deprecated)

        fun array(
            elementSchema: JsonSchema,
            title: String? = null,
            description: String? = null,
            default: Any? = null,
            nullable: Boolean = false,
            example: Any? = null,
            deprecated: Boolean = false,
            minItems: Int? = null,
            maxItems: Int? = null,
            uniqueItems: Boolean = false
        ) = ArraySchema(elementSchema, title, description, default, nullable, example, deprecated, minItems, maxItems, uniqueItems)

        fun obj(
            fields: Map<String, FieldSchema>,
            title: String? = null,
            description: String? = null,
            default: Any? = null,
            nullable: Boolean = false,
            example: Any? = null,
            deprecated: Boolean = false,
            required: Set<String>? = null
        ) = ObjectSchema(fields, title, description, default, nullable, example, deprecated, required)
    }
}
