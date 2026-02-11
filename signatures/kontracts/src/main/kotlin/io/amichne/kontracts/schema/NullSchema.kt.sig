file=kontracts/src/main/kotlin/io/amichne/kontracts/schema/NullSchema.kt
package=io.amichne.kontracts.schema
type=io.amichne.kontracts.schema.NullSchema|kind=class|decl=data class NullSchema( override val title: String? = null, override val description: String? = null, override val default: Any? = null, override val nullable: Boolean = true, override val example: Any? = null, override val deprecated: Boolean = false ) : JsonSchema<Any>()
fields:
- override val type: OpenApi.Type
methods:
- override fun toString()
