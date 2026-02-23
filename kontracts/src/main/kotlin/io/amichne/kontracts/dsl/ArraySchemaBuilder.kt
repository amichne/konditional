package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.ArraySchema
import io.amichne.kontracts.schema.JsonSchema

@JsonSchemaBuilderDsl
class ArraySchemaBuilder<E : Any> @PublishedApi internal constructor() : JsonSchemaBuilder<List<E>> {
    var title: String? = null
    var description: String? = null
    var default: List<E>? = null
    var nullable: Boolean = false
    var example: List<E>? = null
    var deprecated: Boolean = false
    var minItems: Int? = null
    var maxItems: Int? = null
    var uniqueItems: Boolean = false
    lateinit var elementSchema: JsonSchema<E>

    fun element(builder: RootObjectSchemaBuilder.() -> Unit) {
        @Suppress("UNCHECKED_CAST")
        elementSchema = RootObjectSchemaBuilder().apply(builder).build() as JsonSchema<E>
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

@JsonSchemaBuilderDsl
fun <E : Any> ArraySchemaBuilder<E>.elementSchema(schema: JsonSchema<E>) {
    this.elementSchema = schema
}
