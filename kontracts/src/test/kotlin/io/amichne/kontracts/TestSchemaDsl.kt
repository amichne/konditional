package io.amichne.kontracts

import io.amichne.kontracts.dsl.arraySchema as dslArraySchema
import io.amichne.kontracts.dsl.booleanSchema as dslBooleanSchema
import io.amichne.kontracts.dsl.doubleSchema as dslDoubleSchema
import io.amichne.kontracts.dsl.fieldSchema as dslFieldSchema
import io.amichne.kontracts.dsl.intSchema as dslIntSchema
import io.amichne.kontracts.dsl.nullSchema as dslNullSchema
import io.amichne.kontracts.dsl.objectSchema as dslObjectSchema
import io.amichne.kontracts.dsl.stringSchema as dslStringSchema
import io.amichne.kontracts.schema.FieldSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ObjectSchema

internal fun stringSchema(
    title: String? = null,
    description: String? = null,
    default: String? = null,
    nullable: Boolean = false,
    example: String? = null,
    deprecated: Boolean = false,
    minLength: Int? = null,
    maxLength: Int? = null,
    pattern: String? = null,
    format: String? = null,
    enum: List<String>? = null,
): JsonSchema<String> =
    dslStringSchema {
        this.title = title
        this.description = description
        this.default = default
        this.nullable = nullable
        this.example = example
        this.deprecated = deprecated
        this.minLength = minLength
        this.maxLength = maxLength
        this.pattern = pattern
        this.format = format
        this.enum = enum
    }

internal fun booleanSchema(
    title: String? = null,
    description: String? = null,
    default: Boolean? = null,
    nullable: Boolean = false,
    example: Boolean? = null,
    deprecated: Boolean = false,
): JsonSchema<Boolean> =
    dslBooleanSchema {
        this.title = title
        this.description = description
        this.default = default
        this.nullable = nullable
        this.example = example
        this.deprecated = deprecated
    }

internal fun intSchema(
    title: String? = null,
    description: String? = null,
    default: Int? = null,
    nullable: Boolean = false,
    example: Int? = null,
    deprecated: Boolean = false,
    minimum: Int? = null,
    maximum: Int? = null,
    enum: List<Int>? = null,
): JsonSchema<Int> =
    dslIntSchema {
        this.title = title
        this.description = description
        this.default = default
        this.nullable = nullable
        this.example = example
        this.deprecated = deprecated
        this.minimum = minimum
        this.maximum = maximum
        this.enum = enum
    }

internal fun doubleSchema(
    title: String? = null,
    description: String? = null,
    default: Double? = null,
    nullable: Boolean = false,
    example: Double? = null,
    deprecated: Boolean = false,
    minimum: Double? = null,
    maximum: Double? = null,
    enum: List<Double>? = null,
    format: String? = null,
): JsonSchema<Double> =
    dslDoubleSchema {
        this.title = title
        this.description = description
        this.default = default
        this.nullable = nullable
        this.example = example
        this.deprecated = deprecated
        this.minimum = minimum
        this.maximum = maximum
        this.enum = enum
        this.format = format
    }

internal fun nullSchema(
    title: String? = null,
    description: String? = null,
    default: Any? = null,
    example: Any? = null,
    deprecated: Boolean = false,
): JsonSchema<Any> =
    dslNullSchema {
        this.title = title
        this.description = description
        this.default = default
        this.example = example
        this.deprecated = deprecated
    }

internal fun fieldSchema(
    schema: JsonSchema<*>,
    required: Boolean = false,
    defaultValue: Any? = null,
    description: String? = null,
    deprecated: Boolean = false,
): FieldSchema =
    dslFieldSchema {
        this.schema = schema
        this.required = required
        this.defaultValue = defaultValue
        this.description = description
        this.deprecated = deprecated
    }

internal fun objectSchema(
    fields: Map<String, FieldSchema>,
    title: String? = null,
    description: String? = null,
    default: Map<String, Any?>? = null,
    nullable: Boolean = false,
    example: Map<String, Any?>? = null,
    deprecated: Boolean = false,
    required: Set<String>? = null,
): ObjectSchema =
    dslObjectSchema {
        this.fields = fields
        this.title = title
        this.description = description
        this.default = default
        this.nullable = nullable
        this.example = example
        this.deprecated = deprecated
        this.required = required
    }

internal fun <E : Any> arraySchema(
    elementSchema: JsonSchema<E>,
    title: String? = null,
    description: String? = null,
    default: List<E>? = null,
    nullable: Boolean = false,
    example: List<E>? = null,
    deprecated: Boolean = false,
    minItems: Int? = null,
    maxItems: Int? = null,
    uniqueItems: Boolean = false,
): JsonSchema<List<Any>> =
    dslArraySchema {
        @Suppress("UNCHECKED_CAST")
        this.elementSchema = elementSchema as JsonSchema<Any>
        this.title = title
        this.description = description
        @Suppress("UNCHECKED_CAST")
        this.default = default as? List<Any>
        this.nullable = nullable
        @Suppress("UNCHECKED_CAST")
        this.example = example as? List<Any>
        this.deprecated = deprecated
        this.minItems = minItems
        this.maxItems = maxItems
        this.uniqueItems = uniqueItems
    }
