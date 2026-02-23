package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.JsonSchema
import kotlin.reflect.KClass

fun stringSchema(builder: StringSchemaBuilder.() -> Unit = {}): JsonSchema<String> =
    StringSchemaBuilder().apply(builder).build()

fun booleanSchema(builder: BooleanSchemaBuilder.() -> Unit = {}): JsonSchema<Boolean> =
    BooleanSchemaBuilder().apply(builder).build()

fun intSchema(builder: IntSchemaBuilder.() -> Unit = {}): JsonSchema<Int> =
    IntSchemaBuilder().apply(builder).build()

fun doubleSchema(builder: DoubleSchemaBuilder.() -> Unit = {}): JsonSchema<Double> =
    DoubleSchemaBuilder().apply(builder).build()

fun nullSchema(builder: NullSchemaBuilder.() -> Unit = {}): JsonSchema<Any> =
    NullSchemaBuilder().apply(builder).build()

//fun arraySchema(builder: ArraySchemaBuilder<Any>.() -> Unit): JsonSchema<List<Any>> =
//    ArraySchemaBuilder<Any>().apply(builder).build()
//
inline fun <reified T : Any> arraySchema(
    elementSchema: JsonSchema<T>,
    builder: ArraySchemaBuilder<T>.() -> Unit = {},
): JsonSchema<List<T>> = ArraySchemaBuilder<T>().apply {
    elementSchema(elementSchema)
}.apply(builder).build()

@JvmName("arraySchemaTyped")
fun <T : Any> arraySchema(
    builder: ArraySchemaBuilder<T>.() -> Unit,
): JsonSchema<List<T>> = ArraySchemaBuilder<T>().apply(builder).build()

fun <E : Enum<E>> enumSchema(
    enumClass: KClass<E>,
    builder: EnumSchemaBuilder<E>.() -> Unit = {},
): JsonSchema<E> = EnumSchemaBuilder(enumClass).apply(builder).build()
