package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.IntSchema

@JsonSchemaBuilderDsl
open class IntSchemaBuilder : JsonSchemaBuilder {
    var title: String? = null
    var description: String? = null
    var default: Any? = null
    var nullable: Boolean = false
    var example: Any? = null
    var deprecated: Boolean = false
    var minimum: Int? = null
    var maximum: Int? = null
    var enum: List<Int>? = null
    fun build() =
        IntSchema(title, description, default, nullable, example, deprecated, minimum, maximum, enum)
}
