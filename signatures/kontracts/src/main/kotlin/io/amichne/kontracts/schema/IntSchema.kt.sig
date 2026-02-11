file=kontracts/src/main/kotlin/io/amichne/kontracts/schema/IntSchema.kt
package=io.amichne.kontracts.schema
type=io.amichne.kontracts.schema.IntSchema|kind=class|decl=data class IntSchema( override val title: String? = null, override val description: String? = null, override val default: Int? = null, override val nullable: Boolean = false, override val example: Int? = null, override val deprecated: Boolean = false, val minimum: Int? = null, val maximum: Int? = null, val enum: List<Int>? = null ) : JsonSchema<Int>()
fields:
- override val type: OpenApi.Type
methods:
- override fun toString()
