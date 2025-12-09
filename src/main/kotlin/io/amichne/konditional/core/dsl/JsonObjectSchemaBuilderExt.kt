package io.amichne.konditional.core.dsl

import io.amichne.konditional.core.types.DataClassWithSchema
import io.amichne.konditional.core.types.json.JsonSchema
import io.amichne.konditional.internal.serialization.models.FlagValue
import kotlin.reflect.KClass
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

@Deprecated("Use TypedFieldBuilder with 'typed' custom function instead", ReplaceWith("TypedFieldBuilder"))
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
    val klass: KClass<out V>,
    val property: KProperty0<V>,
) {
    var schemaBuilder: (JsonFieldSchemaBuilder.() -> JsonSchema)? = null

    init {
        schemaBuilder = when (klass) {
            Int::class -> {
                { int() }
            }
            Double::class -> {
                { double() }
            }
            Boolean::class -> {
                { boolean() }
            }
            String::class -> {
                { string() }
            }
//            DataClassWithSchema::class -> {
//                { io.amichne.konditional.core.dsl.jsonObject(schemaBuilder()) }
//            }
            else -> null // For custom/nested types, user must specify
        }
    }

    val isNullable: Boolean = property.returnType.isMarkedNullable

    var default: V? = null

    fun default(value: V) {
        default = value
    }

    fun buildSchema(): JsonFieldSchemaBuilder.() -> JsonSchema =
        schemaBuilder ?: error("You must specify a type/schema for the field")
}

context(builder: JsonObjectSchemaBuilder)
inline infix fun <reified V : Any> KProperty0<V>.of(block: TypedFieldBuilder<V>.() -> Unit) {
    val dsl = TypedFieldBuilder(V::class, this).apply(block)
    return builder.field(name) {
        dsl.buildSchema().invoke(this)
    }
}

context(builder: JsonObjectSchemaBuilder)
inline infix fun <reified V : Any> KProperty0<V>.on(block: TypedFieldBuilder<V>.() -> Unit) {
    val dsl = TypedFieldBuilder(V::class, this@on).apply(block).buildSchema()
    return builder.field(name) {
        dsl.invoke(this)
    }
}
