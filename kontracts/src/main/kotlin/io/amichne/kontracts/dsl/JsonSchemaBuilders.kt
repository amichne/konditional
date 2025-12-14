package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.FieldSchema
import kotlin.reflect.KProperty0

// ========== Type-inferred DSL for automatic schema type resolution ==========

/**
 * String property schema builder with automatic type inference.
 * Provides access to string-specific attributes without needing to call string(...).
 */
context(root: RootObjectSchemaBuilder)
@JvmName("ofString")
inline infix fun KProperty0<String>.of(
    @JsonSchemaBuilderDsl
    builder: StringSchemaBuilder.() -> Unit = {},
) {
    val schema = StringSchemaBuilder().apply(builder).build()
    root.fields[name] = FieldSchema(
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
context(root: RootObjectSchemaBuilder)
@JvmName("ofNullableString")
inline infix fun KProperty0<String?>.of(
    builder: StringSchemaBuilder.() -> Unit = {},
) {
    val schema = StringSchemaBuilder().apply {
        nullable = true
        builder()
    }.build()
    root.fields[name] = FieldSchema(
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
context(root: RootObjectSchemaBuilder)
@JvmName("ofBoolean")
inline infix fun KProperty0<Boolean>.of(
    builder: BooleanSchemaBuilder.() -> Unit = {},
) {
    val schema = BooleanSchemaBuilder().apply(builder).build()
    root.fields[name] = FieldSchema(
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
context(root: RootObjectSchemaBuilder)
@JvmName("ofNullableBoolean")
inline infix fun KProperty0<Boolean?>.of(
    builder: BooleanSchemaBuilder.() -> Unit = {},
) {
    val schema = BooleanSchemaBuilder().apply {
        nullable = true
        builder()
    }.build()
    root.fields[name] = FieldSchema(
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
context(root: RootObjectSchemaBuilder)
@JvmName("ofInt")
inline infix fun KProperty0<Int>.of(
    builder: IntSchemaBuilder.() -> Unit = {},
) {
    val schema = IntSchemaBuilder().apply(builder).build()
    root.fields[name] = FieldSchema(
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
context(root: RootObjectSchemaBuilder)
@JvmName("ofNullableInt")
inline infix fun KProperty0<Int?>.of(
    builder: IntSchemaBuilder.() -> Unit = {},
) {
    val schema = IntSchemaBuilder().apply {
        nullable = true
        builder()
    }.build()
    root.fields[name] = FieldSchema(
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
context(root: RootObjectSchemaBuilder)
@JvmName("ofDouble")
inline infix fun KProperty0<Double>.of(
    builder: DoubleSchemaBuilder.() -> Unit = {},
) {
    val schema = DoubleSchemaBuilder().apply(builder).build()
    root.fields[name] = FieldSchema(
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
context(root: RootObjectSchemaBuilder)
@JvmName("ofNullableDouble")
inline infix fun KProperty0<Double?>.of(
    builder: DoubleSchemaBuilder.() -> Unit = {},
) {
    val schema = DoubleSchemaBuilder().apply {
        nullable = true
        builder()
    }.build()
    root.fields[name] = FieldSchema(
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
context(root: RootObjectSchemaBuilder)
@JvmName("ofObject")
inline infix fun <reified V : Any> KProperty0<V>.of(
    builder: ObjectSchemaBuilder.() -> Unit,
) {
    root.fields[name] = with(ObjectSchemaBuilder().apply { builder() }.build()) {
        FieldSchema(
            this,
            returnType.isMarkedNullable,
            default,
            description,
            deprecated
        )
    }
}

fun schemaRoot(builder: RootObjectSchemaBuilder.() -> Unit) = RootObjectSchemaBuilder().apply(builder).build()
