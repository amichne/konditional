package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.NullSchema

@JsonSchemaBuilderDsl
class NullSchemaBuilder {
    var title: String? = null
    var description: String? = null
    var default: Any? = null
    var example: Any? = null
    var deprecated: Boolean = false
    fun build() = NullSchema(title, description, default, true, example, deprecated)
}
