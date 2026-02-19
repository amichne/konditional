file=kontracts/src/main/kotlin/io/amichne/kontracts/dsl/JsonSchemaFactoryFunctions.kt
package=io.amichne.kontracts.dsl
imports=io.amichne.kontracts.schema.FieldSchema,io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.MapSchema,io.amichne.kontracts.schema.ObjectSchema,io.amichne.kontracts.schema.OneOfSchema
type=io.amichne.kontracts.dsl.FieldSchemaFactoryBuilder|kind=class|decl=class FieldSchemaFactoryBuilder
type=io.amichne.kontracts.dsl.ObjectSchemaFactoryBuilder|kind=class|decl=class ObjectSchemaFactoryBuilder
type=io.amichne.kontracts.dsl.MapSchemaFactoryBuilder|kind=class|decl=class MapSchemaFactoryBuilder<V : Any>
type=io.amichne.kontracts.dsl.OneOfDiscriminatorBuilder|kind=class|decl=class OneOfDiscriminatorBuilder
type=io.amichne.kontracts.dsl.OneOfSchemaFactoryBuilder|kind=class|decl=class OneOfSchemaFactoryBuilder
fields:
- lateinit var schema: JsonSchema<*>
- var required: Boolean
- var defaultValue: Any?
- var description: String?
- var deprecated: Boolean
- var fields: Map<String, FieldSchema>
- var title: String?
- var default: Map<String, Any?>?
- var nullable: Boolean
- var example: Map<String, Any?>?
- var required: Set<String>?
- lateinit var valueSchema: JsonSchema<V>
- var default: Map<String, V>?
- var example: Map<String, V>?
- var minProperties: Int?
- var maxProperties: Int?
- lateinit var propertyName: String
- var mapping: Map<String, String>
- var options: List<JsonSchema<*>>
- var default: Any?
- var example: Any?
methods:
- fun discriminator(builder: OneOfDiscriminatorBuilder.() -> Unit)
