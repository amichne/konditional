package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.ArraySchema
import io.amichne.kontracts.schema.JsonSchema

@JsonSchemaBuilderDsl
class ArraySchemaBuilder : JsonSchemaBuilder {
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
    fun element(builder: RootObjectSchemaBuilder.() -> Unit) {
        elementSchema = RootObjectSchemaBuilder().apply(builder).build()
    }

    fun build() = ArraySchema(
        elementSchema,
        title,
        description,
        default,
        nullable,
        example,
        deprecated,
        minItems,
        maxItems,
        uniqueItems
    )
}
