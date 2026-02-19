file=kontracts/src/main/kotlin/io/amichne/kontracts/dsl/ArraySchemaBuilder.kt
package=io.amichne.kontracts.dsl
imports=io.amichne.kontracts.schema.ArraySchema,io.amichne.kontracts.schema.JsonSchema
type=io.amichne.kontracts.dsl.ArraySchemaBuilder|kind=class|decl=class ArraySchemaBuilder @PublishedApi internal constructor() : JsonSchemaBuilder<List<Any>>
fields:
- var title: String?
- var description: String?
- var default: List<Any>?
- var nullable: Boolean
- var example: List<Any>?
- var deprecated: Boolean
- var minItems: Int?
- var maxItems: Int?
- var uniqueItems: Boolean
- lateinit var elementSchema: JsonSchema<Any>
methods:
- fun element(builder: RootObjectSchemaBuilder.() -> Unit)
