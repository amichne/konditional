file=kontracts/src/main/kotlin/io/amichne/kontracts/dsl/custom/CustomStringSchemaBuilder.kt
package=io.amichne.kontracts.dsl.custom
imports=io.amichne.kontracts.dsl.JsonSchemaBuilderDsl,io.amichne.kontracts.dsl.StringSchemaBuilder
type=io.amichne.kontracts.dsl.custom.CustomStringSchemaBuilder|kind=class|decl=class CustomStringSchemaBuilder<V : Any> : StringSchemaBuilder()
fields:
- var represent: (V.() -> String)?
