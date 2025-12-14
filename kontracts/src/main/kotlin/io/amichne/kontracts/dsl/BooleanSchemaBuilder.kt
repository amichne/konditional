package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.BooleanSchema

@JsonSchemaBuilderDsl
open class BooleanSchemaBuilder : JsonSchemaBuilder {
    var title: String? = null
    var description: String? = null
    var default: Any? = null
    var nullable: Boolean = false
    var example: Any? = null
    var deprecated: Boolean = false
    fun build() = BooleanSchema(title, description, default, nullable, example, deprecated)
}
