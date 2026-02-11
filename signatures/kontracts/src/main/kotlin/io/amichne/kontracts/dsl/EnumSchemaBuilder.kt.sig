file=kontracts/src/main/kotlin/io/amichne/kontracts/dsl/EnumSchemaBuilder.kt
package=io.amichne.kontracts.dsl
imports=io.amichne.kontracts.schema.EnumSchema,kotlin.reflect.KClass
type=io.amichne.kontracts.dsl.EnumSchemaBuilder|kind=class|decl=class EnumSchemaBuilder<E : Enum<E>>(private val enumClass: KClass<E>) : JsonSchemaBuilder<E>
fields:
- var title: String?
- var description: String?
- var default: E?
- var nullable: Boolean
- var example: E?
- var deprecated: Boolean
- var values: List<E>
methods:
- override fun build()
