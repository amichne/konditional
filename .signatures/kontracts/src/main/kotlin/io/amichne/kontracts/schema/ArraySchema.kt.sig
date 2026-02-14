file=kontracts/src/main/kotlin/io/amichne/kontracts/schema/ArraySchema.kt
package=io.amichne.kontracts.schema
type=io.amichne.kontracts.schema.ArraySchema|kind=class|decl=data class ArraySchema<E : Any> internal constructor( val elementSchema: JsonSchema<E>, override val title: String? = null, override val description: String? = null, override val default: List<E>? = null, override val nullable: Boolean = false, override val example: List<E>? = null, override val deprecated: Boolean = false, val minItems: Int? = null, val maxItems: Int? = null, val uniqueItems: Boolean = false ) : JsonSchema<List<E>>()
fields:
- override val type: OpenApi.Type
methods:
- override fun toString()
