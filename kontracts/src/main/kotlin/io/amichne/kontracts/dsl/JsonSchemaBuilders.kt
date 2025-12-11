package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.JsonSchema
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0

@DslMarker
annotation class JsonSchemaBuilderDsl

sealed interface JsonSchemaApi

@JsonSchemaBuilderDsl
open class BooleanSchemaBuilder : JsonSchemaApi {
    var title: String? = null
    var description: String? = null
    var default: Any? = null
    var nullable: Boolean = false
    var example: Any? = null
    var deprecated: Boolean = false
    fun build() = JsonSchema.BooleanSchema(title, description, default, nullable, example, deprecated)
}

@JsonSchemaBuilderDsl
open class StringSchemaBuilder : JsonSchemaApi {
    var title: String? = null
    var description: String? = null
    var default: Any? = null
    var nullable: Boolean = false
    var example: Any? = null
    var deprecated: Boolean = false
    var minLength: Int? = null
    var maxLength: Int? = null
    var pattern: String? = null
    var format: String? = null
    var enum: List<String>? = null
    fun build() = JsonSchema.StringSchema(
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
}

@JsonSchemaBuilderDsl
open class IntSchemaBuilder : JsonSchemaApi {
    var title: String? = null
    var description: String? = null
    var default: Any? = null
    var nullable: Boolean = false
    var example: Any? = null
    var deprecated: Boolean = false
    var minimum: Int? = null
    var maximum: Int? = null
    var enum: List<Int>? = null
    fun build() =
        JsonSchema.IntSchema(title, description, default, nullable, example, deprecated, minimum, maximum, enum)
}

@JsonSchemaBuilderDsl
open class DoubleSchemaBuilder : JsonSchemaApi {
    var title: String? = null
    var description: String? = null
    var default: Any? = null
    var nullable: Boolean = false
    var example: Any? = null
    var deprecated: Boolean = false
    var minimum: Double? = null
    var maximum: Double? = null
    var enum: List<Double>? = null
    var format: String? = null
    fun build() = JsonSchema.DoubleSchema(
        title,
        description,
        default,
        nullable,
        example,
        deprecated,
        minimum,
        maximum,
        enum,
        format
    )
}

@JsonSchemaBuilderDsl
class EnumSchemaBuilder<E : Enum<E>>(private val enumClass: KClass<E>) : JsonSchemaApi {
    var title: String? = null
    var description: String? = null
    var default: Any? = null
    var nullable: Boolean = false
    var example: Any? = null
    var deprecated: Boolean = false
    var values: List<E> = enumClass.java.enumConstants.toList()
    fun build() = JsonSchema.EnumSchema(enumClass, values, title, description, default, nullable, example, deprecated)
}

@JsonSchemaBuilderDsl
class NullSchemaBuilder {
    var title: String? = null
    var description: String? = null
    var default: Any? = null
    var example: Any? = null
    var deprecated: Boolean = false
    fun build() = JsonSchema.NullSchema(title, description, default, true, example, deprecated)
}

@JsonSchemaBuilderDsl
class ArraySchemaBuilder : JsonSchemaApi {
    var title: String? = null
    var description: String? = null
    var default: Any? = null
    var nullable: Boolean = false
    var example: Any? = null
    var deprecated: Boolean = false
    var minItems: Int? = null
    var maxItems: Int? = null
    var uniqueItems: Boolean = false
    lateinit var elementSchema: JsonSchema
    fun element(builder: JsonSchemaRootBuilder.() -> Unit) {
        elementSchema = JsonSchemaRootBuilder().apply(builder).build()
    }

    fun build() = JsonSchema.ArraySchema(
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
}

@JsonSchemaBuilderDsl
class ObjectSchemaBuilder : JsonSchemaApi {
    var title: String? = null
    var description: String? = null
    var default: Any? = null
    var nullable: Boolean = false
    var example: Any? = null
    var deprecated: Boolean = false
    var required: Set<String>? = null
    private val fields = mutableMapOf<String, JsonSchema.FieldSchema>()

    fun field(
        name: String,
        required: Boolean = false,
        defaultValue: Any? = null,
        description: String? = null,
        deprecated: Boolean = false,
        builder: JsonObjectSchemaBuilder.() -> Unit,
    ) {
        fields[name] = JsonSchema.FieldSchema(
            JsonObjectSchemaBuilder().apply(builder).build(),
            required,
            defaultValue,
            description,
            deprecated
        )
    }

    fun build() = JsonSchema.ObjectSchema(fields, title, description, default, nullable, example, deprecated, required)
}

@JsonSchemaBuilderDsl
class JsonSchemaRootBuilder {
    @PublishedApi
    internal val fields: MutableMap<String, JsonSchema.FieldSchema> = mutableMapOf()

    @PublishedApi
    internal var schema: JsonSchema? = null

    fun build(): JsonSchema.ObjectSchema = when (val builtSchema = schema) {
        is JsonSchema.ObjectSchema -> builtSchema
        null -> JsonSchema.ObjectSchema(fields.toMap())
        else -> throw IllegalStateException("Top-level schema must be an ObjectSchema")
    }
}

// ========== Type-inferred DSL for automatic schema type resolution ==========

/**
 * String property schema builder with automatic type inference.
 * Provides access to string-specific attributes without needing to call string(...).
 */
context(root: JsonSchemaRootBuilder)
@JvmName("ofString")
inline infix fun KProperty0<String>.of(
    @JsonSchemaBuilderDsl
    builder: StringSchemaBuilder.() -> Unit = {},
) {
    val schema = StringSchemaBuilder().apply(builder).build()
    root.fields[name] = JsonSchema.FieldSchema(
        schema,
        required = !returnType.isMarkedNullable,
        defaultValue = schema.default,
        description = schema.description,
        deprecated = schema.deprecated
    )
}

/**
 * Nullable String property schema builder.
 */
context(root: JsonSchemaRootBuilder)
@JvmName("ofNullableString")
inline infix fun KProperty0<String?>.of(
    builder: StringSchemaBuilder.() -> Unit = {},
) {
    val schema = StringSchemaBuilder().apply {
        nullable = true
        builder()
    }.build()
    root.fields[name] = JsonSchema.FieldSchema(
        schema,
        required = false,
        defaultValue = schema.default,
        description = schema.description,
        deprecated = schema.deprecated
    )
}

/**
 * Boolean property schema builder with automatic type inference.
 */
context(root: JsonSchemaRootBuilder)
@JvmName("ofBoolean")
inline infix fun KProperty0<Boolean>.of(
    builder: BooleanSchemaBuilder.() -> Unit = {},
) {
    val schema = BooleanSchemaBuilder().apply(builder).build()
    root.fields[name] = JsonSchema.FieldSchema(
        schema,
        required = !returnType.isMarkedNullable,
        defaultValue = schema.default,
        description = schema.description,
        deprecated = schema.deprecated
    )
}

/**
 * Nullable Boolean property schema builder.
 */
context(root: JsonSchemaRootBuilder)
@JvmName("ofNullableBoolean")
inline infix fun KProperty0<Boolean?>.of(
    builder: BooleanSchemaBuilder.() -> Unit = {},
) {
    val schema = BooleanSchemaBuilder().apply {
        nullable = true
        builder()
    }.build()
    root.fields[name] = JsonSchema.FieldSchema(
        schema,
        required = false,
        defaultValue = schema.default,
        description = schema.description,
        deprecated = schema.deprecated
    )
}

/**
 * Int property schema builder with automatic type inference.
 */
context(root: JsonSchemaRootBuilder)
@JvmName("ofInt")
inline infix fun KProperty0<Int>.of(
    builder: IntSchemaBuilder.() -> Unit = {},
) {
    val schema = IntSchemaBuilder().apply(builder).build()
    root.fields[name] = JsonSchema.FieldSchema(
        schema,
        required = !returnType.isMarkedNullable,
        defaultValue = schema.default,
        description = schema.description,
        deprecated = schema.deprecated
    )
}

/**
 * Nullable Int property schema builder.
 */
context(root: JsonSchemaRootBuilder)
@JvmName("ofNullableInt")
inline infix fun KProperty0<Int?>.of(
    builder: IntSchemaBuilder.() -> Unit = {},
) {
    val schema = IntSchemaBuilder().apply {
        nullable = true
        builder()
    }.build()
    root.fields[name] = JsonSchema.FieldSchema(
        schema,
        required = false,
        defaultValue = schema.default,
        description = schema.description,
        deprecated = schema.deprecated
    )
}

/**
 * Double property schema builder with automatic type inference.
 */
context(root: JsonSchemaRootBuilder)
@JvmName("ofDouble")
inline infix fun KProperty0<Double>.of(
    builder: DoubleSchemaBuilder.() -> Unit = {},
) {
    val schema = DoubleSchemaBuilder().apply(builder).build()
    root.fields[name] = JsonSchema.FieldSchema(
        schema,
        required = !returnType.isMarkedNullable,
        defaultValue = schema.default,
        description = schema.description,
        deprecated = schema.deprecated
    )
}

/**
 * Nullable Double property schema builder.
 */
context(root: JsonSchemaRootBuilder)
@JvmName("ofNullableDouble")
inline infix fun KProperty0<Double?>.of(
    builder: DoubleSchemaBuilder.() -> Unit = {},
) {
    val schema = DoubleSchemaBuilder().apply {
        nullable = true
        builder()
    }.build()
    root.fields[name] = JsonSchema.FieldSchema(
        schema,
        required = false,
        defaultValue = schema.default,
        description = schema.description,
        deprecated = schema.deprecated
    )
}

/**
 * Generic object property schema builder (fallback for complex types).
 * Use this for nested objects or when you need explicit object schema.
 */
context(root: JsonSchemaRootBuilder)
@JvmName("ofObject")
inline infix fun <reified V : Any> KProperty0<V>.of(
    builder: ObjectSchemaBuilder.() -> Unit,
) {
    root.fields[name] = with(ObjectSchemaBuilder().apply { builder() }.build()) {
        JsonSchema.FieldSchema(
            this,
            returnType.isMarkedNullable,
            default,
            description,
            deprecated
        )
    }
}

// ========== Custom Type Mapping DSL (as*) ==========

/**
 * Builder for custom types that should be represented as strings in the schema.
 * Allows specifying a conversion function from V to String.
 */
@JsonSchemaBuilderDsl
class CustomStringSchemaBuilder<V : Any> : StringSchemaBuilder() {
    /**
     * Optional conversion function that transforms the custom type V into a String.
     * This is for documentation and potential runtime conversion.
     */
    var represent: (V.() -> String)? = null
}

/**
 * Builder for custom types that should be represented as booleans in the schema.
 */
@JsonSchemaBuilderDsl
class CustomBooleanSchemaBuilder<V : Any> : BooleanSchemaBuilder() {
    var represent: (V.() -> Boolean)? = null
}

/**
 * Builder for custom types that should be represented as integers in the schema.
 */
@JsonSchemaBuilderDsl
class CustomIntSchemaBuilder<V : Any> : IntSchemaBuilder() {
    var represent: (V.() -> Int)? = null
}

/**
 * Builder for custom types that should be represented as doubles in the schema.
 */
@JsonSchemaBuilderDsl
class CustomDoubleSchemaBuilder<V : Any> : DoubleSchemaBuilder() {
    var represent: (V.() -> Double)? = null
}

/**
 * Maps a custom type property to a String schema representation.
 *
 * Example:
 * ```kotlin
 * data class UserId(val value: String)
 *
 * ::userId asString {
 *     represent { this.value }
 *     pattern = "[A-Z0-9]+"
 *     minLength = 8
 *     description = "Unique user identifier"
 * }
 * ```
 */
context(root: JsonSchemaRootBuilder)
@JvmName("asString")
inline infix fun <reified V : Any> KProperty0<V>.asString(
    builder: CustomStringSchemaBuilder<V>.() -> Unit,
) {
    val schemaBuilder = CustomStringSchemaBuilder<V>().apply(builder)
    val schema = schemaBuilder.build()
    root.fields[name] = JsonSchema.FieldSchema(
        schema,
        required = !returnType.isMarkedNullable,
        defaultValue = schema.default,
        description = schema.description,
        deprecated = schema.deprecated
    )
}

/**
 * Maps a nullable custom type property to a String schema representation.
 */
context(root: JsonSchemaRootBuilder)
@JvmName("asNullableString")
inline infix fun <reified V : Any> KProperty0<V?>.asString(
    builder: CustomStringSchemaBuilder<V>.() -> Unit,
) {
    val schemaBuilder = CustomStringSchemaBuilder<V>().apply {
        nullable = true
        builder()
    }
    val schema = schemaBuilder.build()
    root.fields[name] = JsonSchema.FieldSchema(
        schema,
        required = false,
        defaultValue = schema.default,
        description = schema.description,
        deprecated = schema.deprecated
    )
}

/**
 * Maps a custom type property to an Int schema representation.
 *
 * Example:
 * ```kotlin
 * data class Count(val value: Int)
 *
 * ::count asInt {
 *     represent { this.value }
 *     minimum = 0
 *     maximum = 100
 * }
 * ```
 */
context(root: JsonSchemaRootBuilder)
@JvmName("asInt")
inline infix fun <reified V : Any> KProperty0<V>.asInt(
    builder: CustomIntSchemaBuilder<V>.() -> Unit,
) {
    val schemaBuilder = CustomIntSchemaBuilder<V>().apply(builder)
    val schema = schemaBuilder.build()
    root.fields[name] = JsonSchema.FieldSchema(
        schema,
        required = !returnType.isMarkedNullable,
        defaultValue = schema.default,
        description = schema.description,
        deprecated = schema.deprecated
    )
}

/**
 * Maps a nullable custom type property to an Int schema representation.
 */
context(root: JsonSchemaRootBuilder)
@JvmName("asNullableInt")
inline infix fun <reified V : Any> KProperty0<V?>.asInt(
    builder: CustomIntSchemaBuilder<V>.() -> Unit,
) {
    val schemaBuilder = CustomIntSchemaBuilder<V>().apply {
        nullable = true
        builder()
    }
    val schema = schemaBuilder.build()
    root.fields[name] = JsonSchema.FieldSchema(
        schema,
        required = false,
        defaultValue = schema.default,
        description = schema.description,
        deprecated = schema.deprecated
    )
}

/**
 * Maps a custom type property to a Boolean schema representation.
 *
 * Example:
 * ```kotlin
 * data class Flag(val enabled: Boolean)
 *
 * ::flag asBoolean {
 *     represent { this.enabled }
 *     description = "Feature flag state"
 * }
 * ```
 */
context(root: JsonSchemaRootBuilder)
@JvmName("asBoolean")
inline infix fun <reified V : Any> KProperty0<V>.asBoolean(
    builder: CustomBooleanSchemaBuilder<V>.() -> Unit,
) {
    val schemaBuilder = CustomBooleanSchemaBuilder<V>().apply(builder)
    val schema = schemaBuilder.build()
    root.fields[name] = JsonSchema.FieldSchema(
        schema,
        required = !returnType.isMarkedNullable,
        defaultValue = schema.default,
        description = schema.description,
        deprecated = schema.deprecated
    )
}

/**
 * Maps a nullable custom type property to a Boolean schema representation.
 */
context(root: JsonSchemaRootBuilder)
@JvmName("asNullableBoolean")
inline infix fun <reified V : Any> KProperty0<V?>.asBoolean(
    builder: CustomBooleanSchemaBuilder<V>.() -> Unit,
) {
    val schemaBuilder = CustomBooleanSchemaBuilder<V>().apply {
        nullable = true
        builder()
    }
    val schema = schemaBuilder.build()
    root.fields[name] = JsonSchema.FieldSchema(
        schema,
        required = false,
        defaultValue = schema.default,
        description = schema.description,
        deprecated = schema.deprecated
    )
}

/**
 * Maps a custom type property to a Double schema representation.
 *
 * Example:
 * ```kotlin
 * data class Percentage(val value: Double)
 *
 * ::percentage asDouble {
 *     represent { this.value }
 *     minimum = 0.0
 *     maximum = 100.0
 *     format = "double"
 * }
 * ```
 */
context(root: JsonSchemaRootBuilder)
@JvmName("asDouble")
inline infix fun <reified V : Any> KProperty0<V>.asDouble(
    builder: CustomDoubleSchemaBuilder<V>.() -> Unit,
) {
    val schemaBuilder = CustomDoubleSchemaBuilder<V>().apply(builder)
    val schema = schemaBuilder.build()
    root.fields[name] = JsonSchema.FieldSchema(
        schema,
        required = !returnType.isMarkedNullable,
        defaultValue = schema.default,
        description = schema.description,
        deprecated = schema.deprecated
    )
}

/**
 * Maps a nullable custom type property to a Double schema representation.
 */
context(root: JsonSchemaRootBuilder)
@JvmName("asNullableDouble")
inline infix fun <reified V : Any> KProperty0<V?>.asDouble(
    builder: CustomDoubleSchemaBuilder<V>.() -> Unit,
) {
    val schemaBuilder = CustomDoubleSchemaBuilder<V>().apply {
        nullable = true
        builder()
    }
    val schema = schemaBuilder.build()
    root.fields[name] = JsonSchema.FieldSchema(
        schema,
        required = false,
        defaultValue = schema.default,
        description = schema.description,
        deprecated = schema.deprecated
    )
}

fun schemaRoot(builder: JsonSchemaRootBuilder.() -> Unit) = JsonSchemaRootBuilder().apply(builder).build()
