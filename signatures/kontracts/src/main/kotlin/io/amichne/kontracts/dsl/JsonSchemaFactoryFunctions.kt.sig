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
- var description: String?
- var default: Map<String, Any?>?
- var nullable: Boolean
- var example: Map<String, Any?>?
- var deprecated: Boolean
- var required: Set<String>?
- lateinit var valueSchema: JsonSchema<V>
- var title: String?
- var description: String?
- var default: Map<String, V>?
- var nullable: Boolean
- var example: Map<String, V>?
- var deprecated: Boolean
- var minProperties: Int?
- var maxProperties: Int?
- lateinit var propertyName: String
- var mapping: Map<String, String>
- var options: List<JsonSchema<*>>
- var title: String?
- var description: String?
- var default: Any?
- var nullable: Boolean
- var example: Any?
- var deprecated: Boolean
- private var discriminatorValue: OneOfSchema.Discriminator?
methods:
- internal fun build(): FieldSchema
- internal fun build(): ObjectSchema
- internal fun build(): MapSchema<V>
- internal fun build(): OneOfSchema.Discriminator
- fun discriminator(builder: OneOfDiscriminatorBuilder.() -> Unit)
- internal fun build(): OneOfSchema
