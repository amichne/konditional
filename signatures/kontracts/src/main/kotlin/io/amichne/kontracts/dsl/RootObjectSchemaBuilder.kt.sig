file=kontracts/src/main/kotlin/io/amichne/kontracts/dsl/RootObjectSchemaBuilder.kt
package=io.amichne.kontracts.dsl
imports=io.amichne.kontracts.schema.FieldSchema,io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.ObjectSchema
type=io.amichne.kontracts.dsl.RootObjectSchemaBuilder|kind=class|decl=class RootObjectSchemaBuilder @PublishedApi internal constructor()
fields:
- internal val fields: MutableMap<String, FieldSchema>
- internal var schema: JsonSchema<*>?
methods:
- fun required( name: String, schema: JsonSchema<*>, description: String? = null, defaultValue: Any? = null, deprecated: Boolean = false )
- fun optional( name: String, schema: JsonSchema<*>, description: String? = null, defaultValue: Any? = null, deprecated: Boolean = false )
- fun build(): ObjectSchema
