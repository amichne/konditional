package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.EnumSchema
import kotlin.reflect.KClass

@JsonSchemaBuilderDsl
class EnumSchemaBuilder<E : Enum<E>>(private val enumClass: KClass<E>) : JsonSchemaBuilder {
    var title: String? = null
    var description: String? = null
    var default: Any? = null
    var nullable: Boolean = false
    var example: Any? = null
    var deprecated: Boolean = false
    var values: List<E> = enumClass.java.enumConstants.toList()
    fun build() = EnumSchema(enumClass, values, title, description, default, nullable, example, deprecated)
}
