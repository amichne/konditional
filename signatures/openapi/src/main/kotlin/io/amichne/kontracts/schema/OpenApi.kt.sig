file=openapi/src/main/kotlin/io/amichne/kontracts/schema/OpenApi.kt
package=io.amichne.kontracts.schema
type=io.amichne.kontracts.schema.OpenApi|kind=interface|decl=interface OpenApi<out T : Any>
type=io.amichne.kontracts.schema.Type|kind=enum|decl=enum class Type(val serialized: String)
fields:
- val type: Type
- val title: String?
- val description: String?
- val default: T?
- val nullable: Boolean
- val example: T?
- val deprecated: Boolean
