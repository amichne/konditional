file=kontracts/src/main/kotlin/io/amichne/kontracts/dsl/DoubleSchemaBuilder.kt
package=io.amichne.kontracts.dsl
imports=io.amichne.kontracts.schema.DoubleSchema
type=io.amichne.kontracts.dsl.DoubleSchemaBuilder|kind=class|decl=open class DoubleSchemaBuilder : JsonSchemaBuilder<Double>
fields:
- var title: String?
- var description: String?
- var default: Double?
- var nullable: Boolean
- var example: Double?
- var deprecated: Boolean
- var minimum: Double?
- var maximum: Double?
- var enum: List<Double>?
- var format: String?
