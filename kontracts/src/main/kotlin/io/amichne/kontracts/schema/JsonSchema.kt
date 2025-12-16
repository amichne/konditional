package io.amichne.kontracts.schema

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

    companion object {
        fun boolean(
            title: String? = null,
            description: String? = null,
            default: Any? = null,
            nullable: Boolean = false,
            example: Any? = null,
            deprecated: Boolean = false
        ): BooleanSchema = BooleanSchema(title, description, default, nullable, example, deprecated)

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
        ): StringSchema = StringSchema(
            title,
            description,
            default,
            nullable,
            example,
            deprecated,
            minLength,
            maxLength,
            pattern,
            format,
            enum
        )

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
        ) = ArraySchema(
            elementSchema,
            title,
            description,
            default,
            nullable,
            example,
            deprecated,
            minItems,
            maxItems,
            uniqueItems
        )

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
