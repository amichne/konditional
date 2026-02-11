file=kontracts/src/main/kotlin/io/amichne/kontracts/schema/MapSchema.kt
package=io.amichne.kontracts.schema
type=io.amichne.kontracts.schema.MapSchema|kind=class|decl=data class MapSchema<V : Any>( val valueSchema: JsonSchema<V>, override val title: String? = null, override val description: String? = null, override val default: Map<String, V>? = null, override val nullable: Boolean = false, override val example: Map<String, V>? = null, override val deprecated: Boolean = false, val minProperties: Int? = null, val maxProperties: Int? = null ) : JsonSchema<Map<String, V>>()
fields:
- override val type: OpenApi.Type
methods:
- override fun toString()
