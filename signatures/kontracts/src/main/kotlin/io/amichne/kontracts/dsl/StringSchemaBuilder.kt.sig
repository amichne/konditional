file=kontracts/src/main/kotlin/io/amichne/kontracts/dsl/StringSchemaBuilder.kt
package=io.amichne.kontracts.dsl
imports=io.amichne.kontracts.schema.StringSchema
type=io.amichne.kontracts.dsl.StringSchemaBuilder|kind=class|decl=open class StringSchemaBuilder @PublishedApi internal constructor() : JsonSchemaBuilder<String>
fields:
- var title: String?
- var description: String?
- var default: String?
- var nullable: Boolean
- var example: String?
- var deprecated: Boolean
- var minLength: Int?
- var maxLength: Int?
- var pattern: String?
- var format: String?
- var enum: List<String>?
