file=kontracts/src/main/kotlin/io/amichne/kontracts/dsl/IntSchemaBuilder.kt
package=io.amichne.kontracts.dsl
imports=io.amichne.kontracts.schema.IntSchema
type=io.amichne.kontracts.dsl.IntSchemaBuilder|kind=class|decl=open class IntSchemaBuilder @PublishedApi internal constructor() : JsonSchemaBuilder<Int>
fields:
- var title: String?
- var description: String?
- var default: Int?
- var nullable: Boolean
- var example: Int?
- var deprecated: Boolean
- var minimum: Int?
- var maximum: Int?
- var enum: List<Int>?
methods:
- override fun build()
