file=kontracts/src/main/kotlin/io/amichne/kontracts/schema/AnySchema.kt
package=io.amichne.kontracts.schema
type=io.amichne.kontracts.schema.AnySchema|kind=class|decl=data class AnySchema internal constructor( override val title: String? = null, override val description: String? = null, override val default: Any? = null, override val nullable: Boolean = false, override val example: Any? = null, override val deprecated: Boolean = false ) : JsonSchema<Any>()
fields:
- override val type: OpenApi.Type
methods:
- override fun toString()
