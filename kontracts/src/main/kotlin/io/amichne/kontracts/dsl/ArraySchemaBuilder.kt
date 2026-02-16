package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.ArraySchema
import io.amichne.kontracts.schema.JsonSchema

@JsonSchemaBuilderDsl
class ArraySchemaBuilder @PublishedApi internal constructor() : JsonSchemaBuilder<List<Any>> {
    var title: String? = null
    var description: String? = null
    var default: List<Any>? = null
    var nullable: Boolean = false
    var example: List<Any>? = null
    var deprecated: Boolean = false
    var minItems: Int? = null
    var maxItems: Int? = null
    var uniqueItems: Boolean = false
    lateinit var elementSchema: JsonSchema<Any>
    fun element(builder: RootObjectSchemaBuilder.() -> Unit) {
        elementSchema = RootObjectSchemaBuilder().apply(builder).build()
    }

    override fun build() = ArraySchema(
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

@Suppress("UNCHECKED_CAST")
fun <E : Any> ArraySchemaBuilder.elementSchema(schema: JsonSchema<E>) {
    this.elementSchema = schema as JsonSchema<Any>
}
