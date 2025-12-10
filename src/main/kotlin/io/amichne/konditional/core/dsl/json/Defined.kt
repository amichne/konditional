package io.amichne.konditional.core.dsl.json

import io.amichne.konditional.core.dsl.TypedFieldScope
import io.amichne.konditional.core.types.json.JsonSchema
import kotlin.reflect.KProperty0

context(builder: JsonObjectSchemaBuilder)
inline infix fun <reified V : Any> KProperty0<V>.def(crossinline block: TypedFieldScope<V>.() -> Unit) =
    builder.field<JsonSchema>(name) {
        TypedFieldBuilder<V>(
            this@def,
            when (returnType.classifier) {
                Int::class -> JsonSchema.IntSchema
                Double::class -> JsonSchema.DoubleSchema
                Boolean::class -> JsonSchema.BooleanSchema
                String::class -> JsonSchema.StringSchema
                else -> error("Unsupported type: $returnType")
            }
        ).apply(block).let {
            JsonSchema.FieldSchema(
                schema = it.schema, required = !it.isNullable, defaultValue = it.default
            )
        }
    }
