package io.amichne.konditional.core.types.json

import kotlin.reflect.KClass

@DslMarker
annotation class JsonSchemaBuilderDsl

@JsonSchemaBuilderDsl
class BooleanSchemaBuilder {
    var title: String? = null
    var description: String? = null
    var default: Any? = null
    var nullable: Boolean = false
    var example: Any? = null
    var deprecated: Boolean = false
    fun build() = JsonSchema.BooleanSchema(title, description, default, nullable, example, deprecated)
}

@JsonSchemaBuilderDsl
class StringSchemaBuilder {
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
    fun build() = JsonSchema.StringSchema(title, description, default, nullable, example, deprecated, minLength, maxLength, pattern, format, enum)
}

@JsonSchemaBuilderDsl
class IntSchemaBuilder {
    var title: String? = null
    var description: String? = null
    var default: Any? = null
    var nullable: Boolean = false
    var example: Any? = null
    var deprecated: Boolean = false
    var minimum: Int? = null
    var maximum: Int? = null
    var enum: List<Int>? = null
    fun build() = JsonSchema.IntSchema(title, description, default, nullable, example, deprecated, minimum, maximum, enum)
}

@JsonSchemaBuilderDsl
class DoubleSchemaBuilder {
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
    fun build() = JsonSchema.DoubleSchema(title, description, default, nullable, example, deprecated, minimum, maximum, enum, format)
}

@JsonSchemaBuilderDsl
class EnumSchemaBuilder<E : Enum<E>>(private val enumClass: KClass<E>) {
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
class ArraySchemaBuilder {
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
    fun element(builder: JsonSchemaBuilder.() -> Unit) {
        elementSchema = JsonSchemaBuilder().apply(builder).build()
    }
    fun build() = JsonSchema.ArraySchema(elementSchema, title, description, default, nullable, example, deprecated, minItems, maxItems, uniqueItems)
}

@JsonSchemaBuilderDsl
class ObjectSchemaBuilder {
    var title: String? = null
    var description: String? = null
    var default: Any? = null
    var nullable: Boolean = false
    var example: Any? = null
    var deprecated: Boolean = false
    var required: Set<String>? = null
    private val fields = mutableMapOf<String, JsonSchema.FieldSchema>()
    fun field(name: String, required: Boolean = false, defaultValue: Any? = null, description: String? = null, deprecated: Boolean = false, builder: JsonSchemaBuilder.() -> Unit) {
        val schema = JsonSchemaBuilder().apply(builder).build()
        fields[name] = JsonSchema.FieldSchema(schema, required, defaultValue, description, deprecated)
    }
    fun build() = JsonSchema.ObjectSchema(fields, title, description, default, nullable, example, deprecated, required)
}

@JsonSchemaBuilderDsl
class JsonSchemaBuilder {
    @PublishedApi
    internal var schema: JsonSchema? = null
    fun boolean(block: BooleanSchemaBuilder.() -> Unit = {}) {
        schema = BooleanSchemaBuilder().apply(block).build()
    }
    fun string(block: StringSchemaBuilder.() -> Unit = {}) {
        schema = StringSchemaBuilder().apply(block).build()
    }
    fun int(block: IntSchemaBuilder.() -> Unit = {}) {
        schema = IntSchemaBuilder().apply(block).build()
    }
    fun double(block: DoubleSchemaBuilder.() -> Unit = {}) {
        schema = DoubleSchemaBuilder().apply(block).build()
    }
    inline fun <reified E : Enum<E>> enum(block: EnumSchemaBuilder<E>.() -> Unit = {}) {
        schema = EnumSchemaBuilder(E::class).apply(block).build()
    }
    fun nullSchema(block: NullSchemaBuilder.() -> Unit = {}) {
        schema = NullSchemaBuilder().apply(block).build()
    }
    fun array(block: ArraySchemaBuilder.() -> Unit) {
        schema = ArraySchemaBuilder().apply(block).build()
    }
    fun obj(block: ObjectSchemaBuilder.() -> Unit) {
        schema = ObjectSchemaBuilder().apply(block).build()
    }
    fun build(): JsonSchema = schema ?: error("No schema defined")
}

fun jsonSchema(builder: JsonSchemaBuilder.() -> Unit): JsonSchema = JsonSchemaBuilder().apply(builder).build()
