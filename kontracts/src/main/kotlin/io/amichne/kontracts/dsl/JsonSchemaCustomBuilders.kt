package io.amichne.kontracts.dsl

import io.amichne.kontracts.dsl.custom.CustomBooleanSchemaBuilder
import io.amichne.kontracts.dsl.custom.CustomDoubleSchemaBuilder
import io.amichne.kontracts.dsl.custom.CustomIntSchemaBuilder
import io.amichne.kontracts.dsl.custom.CustomStringSchemaBuilder
import io.amichne.kontracts.schema.FieldSchema
import kotlin.reflect.KProperty0

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
context(root: RootObjectSchemaBuilder)
@JvmName("asString")
inline infix fun <reified V : Any> KProperty0<V>.asString(
    builder: CustomStringSchemaBuilder<V>.() -> Unit,
) {
    val schemaBuilder = CustomStringSchemaBuilder<V>().apply(builder)
    val schema = schemaBuilder.build()
    root.fields[name] = FieldSchema(
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
context(root: RootObjectSchemaBuilder)
@JvmName("asNullableString")
inline infix fun <reified V : Any> KProperty0<V?>.asString(
    builder: CustomStringSchemaBuilder<V>.() -> Unit,
) {
    val schemaBuilder = CustomStringSchemaBuilder<V>().apply {
        nullable = true
        builder()
    }
    val schema = schemaBuilder.build()
    root.fields[name] = FieldSchema(
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
context(root: RootObjectSchemaBuilder)
@JvmName("asInt")
inline infix fun <reified V : Any> KProperty0<V>.asInt(
    builder: CustomIntSchemaBuilder<V>.() -> Unit,
) {
    val schemaBuilder = CustomIntSchemaBuilder<V>().apply(builder)
    val schema = schemaBuilder.build()
    root.fields[name] = FieldSchema(
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
context(root: RootObjectSchemaBuilder)
@JvmName("asNullableInt")
inline infix fun <reified V : Any> KProperty0<V?>.asInt(
    builder: CustomIntSchemaBuilder<V>.() -> Unit,
) {
    val schemaBuilder = CustomIntSchemaBuilder<V>().apply {
        nullable = true
        builder()
    }
    val schema = schemaBuilder.build()
    root.fields[name] = FieldSchema(
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
context(root: RootObjectSchemaBuilder)
@JvmName("asBoolean")
inline infix fun <reified V : Any> KProperty0<V>.asBoolean(
    builder: CustomBooleanSchemaBuilder<V>.() -> Unit,
) {
    val schemaBuilder = CustomBooleanSchemaBuilder<V>().apply(builder)
    val schema = schemaBuilder.build()
    root.fields[name] = FieldSchema(
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
context(root: RootObjectSchemaBuilder)
@JvmName("asNullableBoolean")
inline infix fun <reified V : Any> KProperty0<V?>.asBoolean(
    builder: CustomBooleanSchemaBuilder<V>.() -> Unit,
) {
    val schemaBuilder = CustomBooleanSchemaBuilder<V>().apply {
        nullable = true
        builder()
    }
    val schema = schemaBuilder.build()
    root.fields[name] = FieldSchema(
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
context(root: RootObjectSchemaBuilder)
@JvmName("asDouble")
inline infix fun <reified V : Any> KProperty0<V>.asDouble(
    builder: CustomDoubleSchemaBuilder<V>.() -> Unit,
) {
    val schemaBuilder = CustomDoubleSchemaBuilder<V>().apply(builder)
    val schema = schemaBuilder.build()
    root.fields[name] = FieldSchema(
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
context(root: RootObjectSchemaBuilder)
@JvmName("asNullableDouble")
inline infix fun <reified V : Any> KProperty0<V?>.asDouble(
    builder: CustomDoubleSchemaBuilder<V>.() -> Unit,
) {
    val schemaBuilder = CustomDoubleSchemaBuilder<V>().apply {
        nullable = true
        builder()
    }
    val schema = schemaBuilder.build()
    root.fields[name] = FieldSchema(
        schema,
        required = false,
        defaultValue = schema.default,
        description = schema.description,
        deprecated = schema.deprecated
    )
}
