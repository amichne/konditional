package io.amichne.konditional.core.dsl.json

import io.amichne.konditional.core.types.json.JsonSchema
import kotlin.reflect.KProperty0

//
//import io.amichne.konditional.core.types.json.JsonSchema
//import io.amichne.konditional.core.types.json.JsonSchema.Companion.boolean
//import io.amichne.konditional.core.types.json.JsonSchema.Companion.double
//import io.amichne.konditional.core.types.json.JsonSchema.Companion.int
//import io.amichne.konditional.core.types.json.JsonSchema.Companion.string
//import kotlin.reflect.KProperty0
//import kotlin.reflect.jvm.jvmErasure
//
//open class FieldBuilder<T : Any, V : Any>(
//    open val property: KProperty0<V>,
//    val receiver: T,
//) {
//    var required: Boolean = !property.returnType.isMarkedNullable
//    protected open var default: V? = null
//    protected  var schemaBuilder: (JsonFieldSchemaBuilder.() -> JsonSchema)? = null
//
//    // Infer type from property
//    init {
//        val kType = property.returnType.jvmErasure
//        schemaBuilder = when (kType) {
//            Int::class -> {
//                { int() }
//            }
//            Double::class -> {
//                { double() }
//            }
//            Boolean::class -> {
//                { boolean() }
//            }
//            String::class -> {
//                { string() }
//            }
//            else -> null // For custom/nested types, user must specify
//        }
//    }
//
//
//    open fun buildSchema(): JsonFieldSchemaBuilder.() -> JsonSchema = schemaBuilder ?: error("You must specify a type/schema for the field")
//}
////open class FieldBuilder {
////    var required: Boolean = false
////    open var default: Any? = null
////    protected var schemaBuilder: (JsonFieldSchemaBuilder.() -> JsonSchema)? = null
////
////    fun type(builder: JsonFieldSchemaBuilder.() -> JsonSchema) {
////        schemaBuilder = builder
////    }
////
////    fun buildSchema(): JsonFieldSchemaBuilder.() -> JsonSchema =
////        schemaBuilder ?: error("You must specify a type/schema for the field")
////}
//
//class TypedFieldBuilder<V : Any>(
//    override val property: KProperty0<V>,
//) : FieldBuilder<Any, V>(property, receiver = Any()) {
//    val isNullable: Boolean = property.returnType.isMarkedNullable
//    var typedSchemaBuilder: (JsonFieldSchemaBuilder.() -> JsonSchema.FieldSchema)? = null
//
//    override var default: V? = null
//
//    fun default(value: V) {
//        default = value
//    }
//
//    fun type(builder: JsonFieldSchemaBuilder.() -> JsonSchema.FieldSchema) {
//        typedSchemaBuilder = builder
//    }
//
//    fun buildTypedSchema(): JsonFieldSchemaBuilder.() -> JsonSchema.FieldSchema =
//        typedSchemaBuilder ?: error("You must specify a type/schema for the field")
//}
//
//context(builder: JsonObjectSchemaBuilder.() -> Unit)
//inline infix fun <reified V : Any> KProperty0<V>.of(block: TypedFieldBuilder<V>.() -> Unit) {
//    val dsl = TypedFieldBuilder<V>(this).apply(block)
//    return builder.field(name) {
//        dsl.buildSchema().invoke(this)
//    }
//}

@Deprecated("Use TypedFieldBuilder with 'typed' extension function instead", ReplaceWith("TypedFieldBuilder"))
open class FieldBuilder {
    var required: Boolean = false
    open var default: Any? = null
    protected var schemaBuilder: (JsonFieldSchemaBuilder.() -> JsonSchema)? = null

    fun type(builder: JsonFieldSchemaBuilder.() -> JsonSchema) {
        schemaBuilder = builder
    }

    fun buildSchema(): JsonFieldSchemaBuilder.() -> JsonSchema =
        schemaBuilder ?: error("You must specify a type/schema for the field")
}

class TypedFieldBuilder<V : Any>(
    val property: KProperty0<V>,
) {
    val isNullable: Boolean = property.returnType.isMarkedNullable
    var schemaBuilder: (JsonFieldSchemaBuilder.() -> JsonSchema.FieldSchema)? = null

    var default: V? = null

    fun default(value: V) {
        default = value
    }

    fun type(builder: JsonFieldSchemaBuilder.() -> JsonSchema.FieldSchema) {
        schemaBuilder = builder
    }

    fun buildSchema(): JsonFieldSchemaBuilder.() -> JsonSchema.FieldSchema =
        schemaBuilder ?: error("You must specify a type/schema for the field")
}

context(builder: JsonObjectSchemaBuilder)
inline infix fun <reified V : Any> KProperty0<V>.of(block: TypedFieldBuilder<V>.() -> Unit) {
    val dsl = TypedFieldBuilder<V>(this).apply(block)
    return builder.field(name) {
        dsl.buildSchema().invoke(this)
    }
}
