package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.ArraySchema
import io.amichne.kontracts.schema.BooleanSchema
import io.amichne.kontracts.schema.IntSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.schema.StringSchema
import io.amichne.kontracts.value.JsonArray
import io.amichne.kontracts.value.JsonBoolean
import io.amichne.kontracts.value.JsonNumber
import io.amichne.kontracts.value.JsonObject
import io.amichne.kontracts.value.JsonString
import io.amichne.kontracts.value.JsonValue

/**
 * DSL marker for JSON schema builders.
 */
@DslMarker
annotation class JsonSchemaDsl

/**
 * Top-level function to create a JSON object schema using DSL.
 */
fun jsonObject(builder: JsonObjectSchemaBuilder.() -> Unit): ObjectSchema {
    return JsonObjectSchemaBuilder().apply(builder).build()
}

// ========== JsonValue Builder DSL ==========

/**
 * Top-level function to build a JSON object value.
 */
fun buildJsonObject(
    schema: ObjectSchema? = null,
    builder: JsonObjectBuilder.() -> Unit,
): JsonObject {
    return JsonObjectBuilder(schema).apply(builder).build()
}

fun buildJsonArray(
    builder: JsonFieldSchemaBuilder.() -> JsonSchema,
): JsonArray {
    return JsonArray(
        emptyList(),
        ArraySchema(JsonFieldSchemaBuilder().array { builder() })
    )
}

/**
 * Builds a JSON array from varargs.
 */
fun buildJsonArray(
    vararg elements: JsonValue,
    elementSchema: JsonSchema? = null,
): JsonArray {
    return JsonArray(elements.toList(), elementSchema)
}

/**
 * Builds a JSON array from a list.
 */
fun buildJsonArray(
    elements: List<JsonValue>,
    elementSchema: JsonSchema? = null,
): JsonArray {
    return JsonArray(elements, elementSchema)
}

/**
 * Builds a JSON array of strings.
 */
fun buildJsonArray(vararg strings: String): JsonArray {
    return JsonArray(strings.map { JsonString(it) }, StringSchema())
}

/**
 * Builds a JSON array of integers.
 */
fun buildJsonArray(vararg ints: Int): JsonArray {
    return JsonArray(ints.map { JsonNumber(it.toDouble()) }, IntSchema())
}

/**
 * Builds a JSON array of booleans.
 */
fun buildJsonArray(vararg booleans: Boolean): JsonArray {
    return JsonArray(booleans.map { JsonBoolean(it) }, BooleanSchema())
}

//class TypedFieldBuilder<V : Any>(
//    val property: KProperty0<V>,
//) {
//    val isNullable: Boolean = property.returnType.isMarkedNullable
//    lateinit var schema: JsonSchema
//
//    var default: V? = null
//
//    fun default(value: V) {
//        default = value
//    }
//}
//
//context(builder: JsonObjectSchemaBuilder)
//inline infix fun <reified V : Any> KProperty0<V>.of(crossinline block: TypedFieldBuilder<V>.() -> Unit): Unit =
//    builder.field(name) { TypedFieldBuilder<V>(this@of).apply<TypedFieldBuilder<V>>(block).schema }
//
//inline fun <reified T : JsonSchema> KProperty<*>.toJsonType(): T {
//    return when (returnType) {
//        Int::class -> JsonSchema.IntSchema
//        Double::class -> JsonSchema.DoubleSchema
//        Boolean::class -> JsonSchema.BooleanSchema
//        String::class -> JsonSchema.StringSchema
//        else -> error("Shouldn't be hittable")
//    } as T
//}
