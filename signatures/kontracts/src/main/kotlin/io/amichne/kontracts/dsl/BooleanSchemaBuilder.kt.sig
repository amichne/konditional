file=kontracts/src/main/kotlin/io/amichne/kontracts/dsl/BooleanSchemaBuilder.kt
package=io.amichne.kontracts.dsl
imports=io.amichne.kontracts.schema.BooleanSchema
type=io.amichne.kontracts.dsl.BooleanSchemaBuilder|kind=class|decl=open class BooleanSchemaBuilder @PublishedApi internal constructor() : JsonSchemaBuilder<Boolean>
fields:
- var title: String?
- var description: String?
- var default: Boolean?
- var nullable: Boolean
- var example: Boolean?
- var deprecated: Boolean
methods:
- override fun build()
