file=kontracts/src/main/kotlin/io/amichne/kontracts/dsl/NullSchemaBuilder.kt
package=io.amichne.kontracts.dsl
imports=io.amichne.kontracts.schema.NullSchema
type=io.amichne.kontracts.dsl.NullSchemaBuilder|kind=class|decl=class NullSchemaBuilder : JsonSchemaBuilder<Any>
fields:
- var title: String?
- var description: String?
- var default: Any?
- var example: Any?
- var deprecated: Boolean
methods:
- override fun build()
