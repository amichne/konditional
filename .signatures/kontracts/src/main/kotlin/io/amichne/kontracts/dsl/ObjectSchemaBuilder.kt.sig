file=kontracts/src/main/kotlin/io/amichne/kontracts/dsl/ObjectSchemaBuilder.kt
package=io.amichne.kontracts.dsl
imports=io.amichne.kontracts.schema.FieldSchema,io.amichne.kontracts.schema.ObjectSchema
type=io.amichne.kontracts.dsl.ObjectSchemaBuilder|kind=class|decl=class ObjectSchemaBuilder @PublishedApi internal constructor() : JsonSchemaBuilder<Map<String, Any?>>
fields:
- var title: String?
- var description: String?
- var default: Map<String, Any?>?
- var nullable: Boolean
- var example: Map<String, Any?>?
- var deprecated: Boolean
- var required: Set<String>?
- private val fields
methods:
- override fun build()
