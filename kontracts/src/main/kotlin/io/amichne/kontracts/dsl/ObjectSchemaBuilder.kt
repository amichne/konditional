package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.FieldSchema
import io.amichne.kontracts.schema.ObjectSchema

@JsonSchemaBuilderDsl
class ObjectSchemaBuilder : JsonSchemaBuilder {
    var title: String? = null
    var description: String? = null
    var default: Any? = null
    var nullable: Boolean = false
    var example: Any? = null
    var deprecated: Boolean = false
    var required: Set<String>? = null
    private val fields = mutableMapOf<String, FieldSchema>()

    fun build() = ObjectSchema(fields, title, description, default, nullable, example, deprecated, required)
}
